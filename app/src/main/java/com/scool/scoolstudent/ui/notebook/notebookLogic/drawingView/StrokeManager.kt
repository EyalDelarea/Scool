package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView

import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.RecognitionTask.RecognizedInk
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.Ink.Stroke
import java.util.*
import kotlin.collections.ArrayDeque

/** Manages the recognition logic and the content that has been added to the current page.  */
class StrokeManager() {
    /** Interface to register to be notified of changes in the recognized content.  */
    interface ContentChangedListener {
        /** This method is called when the recognized content changes.  */
        fun onContentChanged()
    }

    /** Interface to register to be notified of changes in the status.  */
    interface StatusChangedListener {
        /** This method is called when the recognized content changes.  */
        fun onStatusChanged()
    }

//    /** Interface to register to be notified of changes in the downloaded model state.  */
//    interface DownloadedModelsChangedListener {
//        /** This method is called when the downloaded models changes.  */
//        fun onDownloadedModelsChanged(downloadedLanguageTags: Set<String>)
//    }

    // For handling recognition and model downloading.
    private var recognitionTask: RecognitionTask? = null

    @JvmField
    @VisibleForTesting
    var modelManager =
        ModelManager()

    // Managing the recognition queue.
    //Holding <Ink,Text> object
    private val inkContent: MutableList<RecognizedInk> = ArrayList()

    //Holding <Stroke,Char> object
    private val strokeContent: MutableList<RecognizedStroke> = ArrayList()

    //Hold search rect views
    private val searchRect: MutableList<Rect> = ArrayList()

    private val strokeStack: ArrayDeque<RecognizedStroke> = ArrayDeque()


    // Managing ink currently drawn.
    private var strokeBuilder = Stroke.builder()
    private var inkBuilder = Ink.builder()
    private var stateChangedSinceLastRequest = false
    private var contentChangedListener: ContentChangedListener? = null
    private var statusChangedListener: StatusChangedListener? = null

    //   private var downloadedModelsChangedListener: DownloadedModelsChangedListener? = null
    private var triggerRecognitionAfterInput = true
    private var clearCurrentInkAfterRecognition = true
    private val textPaint: TextPaint = TextPaint()

    /** Helper class that stores an Stroke along with the corresponding recognized char.  */
    class contentObject internal constructor(
        val inkList: MutableList<RecognizedInk>,
        val strokes: MutableList<RecognizedStroke>
    )

    var status: String? = ""
        private set(newStatus) {
            field = newStatus
            statusChangedListener?.onStatusChanged()
        }

    fun setTriggerRecognitionAfterInput(shouldTrigger: Boolean) {
        triggerRecognitionAfterInput = shouldTrigger
    }

    fun setClearCurrentInkAfterRecognition(shouldClear: Boolean) {
        clearCurrentInkAfterRecognition = shouldClear
    }

    fun getPageContent(): contentObject {
        return contentObject(inkContent, strokeContent)
    }

    // Handler to handle the UI Timeout.
    // This handler is only used to trigger the UI timeout. Each time a UI interaction happens,
    // the timer is reset by clearing the queue on this handler and sending a new delayed message (in
    // addNewTouchEvent).
    private val uiHandler = Handler(
        Handler.Callback { msg: Message ->
            if (msg.what == TIMEOUT_TRIGGER) {
                Log.i(
                    TAG,
                    "Handling timeout trigger."
                )
                commitResult()
                return@Callback true
            }
            false
        }
    )

    /**
     * Adds the new result to the content list
     */
    private fun commitResult() {
        if (recognitionTask != null) {
            recognitionTask!!.result()?.let {
                handleInkContent(it)
                contentChangedListener?.onContentChanged()
                updateContent()
                //reset recognition task status
                recognitionTask = null
                resetCurrentInk()
            }
        }
    }


    private fun handleInkContent(recognizedInk: RecognizedInk) {
        //Add the whole ink to the inkContent
        inkContent.add(recognizedInk)
        //Add each stroke
        strokeContent.add(recognizedInk)
    }

    fun undo(): Boolean {

        return if (strokeContent.isNotEmpty()) {
            strokeStack.addFirst(strokeContent[strokeContent.size - 1]) // add to stack
            strokeContent.removeAt(strokeContent.size - 1) //remove last stroke from stack
            contentChangedListener?.onContentChanged() //notify content change
            true
        } else {
            false
        }
    }

    fun redo(): Boolean {
        return if (strokeStack.isNotEmpty()) {
            strokeContent.add(strokeStack.first())
            strokeStack.removeFirst()
            contentChangedListener?.onContentChanged()
            true
        } else {
            false
        }
    }

    fun testHashMap(drawingView: DrawingView) {
        strokeContent.removeAt(0) //remove char
        updateContent()               //update status text
        drawingView.onContentChanged() //re draw on screen
        //TODO update inkContent upState when deleting
        Log.i("eyalo", "deleteing")
    }


    private fun updateContent() {
        var contentString = "";
        for (item in inkContent) {
            contentString += item.text
            contentString += " "
        }
        status = contentString
    }

    fun reset() {
        Log.i(TAG, "reset")
        resetCurrentInk()
        inkContent.clear()
        strokeContent.clear()
        recognitionTask?.cancel()
        status = ""
    }

    fun resetSearchRect(drawingView: DrawingView): Boolean {
        this.searchRect.clear()
        drawingView.invalidate()
        contentChangedListener?.onContentChanged()
        return true
    }


    fun searchInk(query: String, drawingView: DrawingView) {

        resetSearchRect(drawingView) //clear previous marks
        //find in content
        textPaint.color = -0x0000ff // yellow.
        textPaint.alpha = 70
        //for each world separate by spaces
        val delim = " "
        val list = query.split(delim)
        for (i in list) {
            markTextOnScreen(i, drawingView)
            Log.i("eyalo", "this is q : $i ")
        }
    }

    private fun markTextOnScreen(
        query: String,
        drawingView: DrawingView,
    ) {
        val matchingIndexes: MutableList<Int> = ArrayList()
        //Find all matching indexes in strokes
        strokeContent.forEachIndexed { index, recognizedStroke ->
            if (query.contains(recognizedStroke.ch!!)) {
                matchingIndexes.add(index)
            }
        }
        if (query != "") {
            //find the best matches for the query
            val (heightStreak, startIndex) = findBestMatches(matchingIndexes, query.length - 1)
            Log.i("eyalo", "heightStreak : $heightStreak , startIndex : $startIndex ")
            //If we have a streak build a rect from few stokes
            //and then mark it
            if (heightStreak > 1) {
                val rect = calBoundingRect(startIndex, heightStreak)
                searchRect.add(rect)
                drawingView.drawTextIntoBoundingBox(searchRect, textPaint)
            } else {
                //No strokes , mark each match alone
                matchingIndexes.forEach {
                    val rect = DrawingView.computeStrokeBoundingBox(strokeContent[it].stroke)
                    searchRect.add(rect)
                    drawingView.drawTextIntoBoundingBox(searchRect, textPaint)
                }
            }
        }
    }

    /**
     * Calculate bondingBox for a streak
     * build a new ink from the strokes and calculates rect
     * return rect
     */
    private fun calBoundingRect(startIndex: Int, heightStreak: Int): Rect {
        val ink = Ink.builder();
        if (startIndex + heightStreak < strokeContent.size) {
            for (s in startIndex..(startIndex + heightStreak)) {
                ink.addStroke(strokeContent[s].stroke)
            }
        }
        val doneInk = ink.build()
        return DrawingView.computeInkBoundingBox(doneInk)

    }

    /**
     * Function to find the longest matching chars in the list
     * returns the startIndex and the amount
     */
    private fun findBestMatches(matchingIndexes: MutableList<Int>, maxLength: Int): Pair<Int, Int> {
        //Find the largest streak
        var heightStreak = 0
        var count = 0;
        var startIndex = 0
        var shouldUpdate = true
        var bestMatchStartIndex = 0


        matchingIndexes.forEachIndexed { index, i ->
            if (index + 1 < matchingIndexes.size) {
                if (i + 1 == matchingIndexes[index + 1]) {
                    count++
                    if (shouldUpdate) {
                        startIndex = i
                        shouldUpdate = false
                    }
                } else {
                    shouldUpdate = true
                    count = 0
                }
                if (heightStreak < count) {
                    heightStreak = count
                    bestMatchStartIndex = startIndex
                }
            }
        }
        //limit the streak to maxLength of the quote
        if (heightStreak > maxLength)
            heightStreak = maxLength
        return Pair(heightStreak, bestMatchStartIndex)
    }


    private fun resetCurrentInk() {
        inkBuilder = Ink.builder()
        strokeBuilder = Stroke.builder()
        stateChangedSinceLastRequest = false
    }

    val currentInk: Ink
        get() = inkBuilder.build()


    /**
     * This method is called when a new touch event happens on the drawing client and notifies the
     * StrokeManager of new content being added.
     *
     *
     * This method takes care of triggering the UI timeout and scheduling recognitions on the
     * background thread.
     *
     * @return whether the touch event was handled.
     */
    fun addNewTouchEvent(event: MotionEvent, isEraseOn: Boolean): Boolean {

        val action = event.actionMasked
        val x = event.x
        val y = event.y
        val t = System.currentTimeMillis()

        // A new event happened -> clear all pending timeout messages.
        uiHandler.removeMessages(TIMEOUT_TRIGGER)
        when (action) {
            //Gather
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> strokeBuilder.addPoint(
                Ink.Point.create(
                    x,
                    y,
                    t
                )
            )
            MotionEvent.ACTION_UP -> {
                strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                inkBuilder.addStroke(strokeBuilder.build())
                strokeBuilder = Stroke.builder()
                stateChangedSinceLastRequest = true
                // recognize()

            }
            else -> // Indicate touch event wasn't handled.
                return false
        }
        return true
    }


    fun getContent(): MutableList<RecognizedStroke> {
        return strokeContent
    }

    // Listeners to update the drawing and status.
    fun setContentChangedListener(contentChangedListener: ContentChangedListener?) {
        this.contentChangedListener = contentChangedListener
    }

    fun setStatusChangedListener(statusChangedListener: StatusChangedListener?) {
        this.statusChangedListener = statusChangedListener
    }

    // Model downloading / deleting / setting.
    fun setActiveModel(languageTag: String) {
        status = modelManager.setModel(languageTag)
    }

    fun deleteActiveModel(): Task<Nothing?> {
        return modelManager
            .deleteActiveModel()
            .onSuccessTask(
                SuccessContinuation { status: String? ->
                    this.status = status
                    return@SuccessContinuation Tasks.forResult(null)
                }
            )
    }

    fun download(): Task<Nothing?> {
        status = "Download started."
        return modelManager
            .download()
            .onSuccessTask(
                SuccessContinuation { status: String? ->
                    this.status = status
                    return@SuccessContinuation Tasks.forResult(null)
                }
            )
    }

    // Recognition-related.
    fun recognize(): Task<String?> {
        if (!stateChangedSinceLastRequest || inkBuilder.isEmpty) {
            status = "No recognition, ink unchanged or empty"
            return Tasks.forResult(null)
        }
        if (modelManager.recognizer == null) {
            status = "Recognizer not set"
            return Tasks.forResult(null)
        }
        return modelManager
            .checkIsModelDownloaded()
            .onSuccessTask { result: Boolean? ->
                if (!result!!) {
                    status = "Model not downloaded yet"
                    return@onSuccessTask Tasks.forResult<String?>(
                        null
                    )
                }
                stateChangedSinceLastRequest = false
                recognitionTask =
                    RecognitionTask(
                        modelManager.recognizer,
                        inkBuilder.build()
                    )
                uiHandler.sendMessageDelayed(
                    uiHandler.obtainMessage(TIMEOUT_TRIGGER),
                    CONVERSION_TIMEOUT_MS
                )
                recognitionTask!!.run()
            }
    }

    /** Helper class that stores an Stroke along with the corresponding recognized char.  */
    class RecognizedStroke internal constructor(val stroke: Stroke, val ch: Char?)

    companion object {
        @JvmField
        @VisibleForTesting
        //1000 default
        val CONVERSION_TIMEOUT_MS: Long = 1000
        private const val TAG = "MLKD.StrokeManager"

        // This is a constant that is used as a message identifier to trigger the timeout.
        private const val TIMEOUT_TRIGGER = 1
    }
}

/**
 * Extension function to strokes content list
 * receiving RecognizedStroke <Stroke,Char>
 *     Adding each stroke the strokeContent list
 *     While taking care of special case letter which
 *     hold two strokes
 *     TODO implement fix for 1 stroke 2 chars ש+ל and so on
 */
private fun MutableList<StrokeManager.RecognizedStroke>.add(recognizedInk: RecognizedInk) {
    //remove spaces
    val noSpacesText = recognizedInk.text?.replace("\\s".toRegex(), "")
    val specialChars = arrayOf('ה', 'ת', 'א', 'ק')
    var textIndex = 0 //iterate over the non spaces text
    var strokeIndex = 0 //iterate of the strokes array
    while (textIndex < noSpacesText?.length!! && strokeIndex < recognizedInk.ink.strokes.size) {
        add(
            StrokeManager.RecognizedStroke(
                recognizedInk.ink.strokes[strokeIndex],
                noSpacesText[textIndex]
            )
        )
        //if we found one of the special chars,they hold two strokes
        if (specialChars.contains(noSpacesText[textIndex])) {
            strokeIndex++
            add(
                StrokeManager.RecognizedStroke(
                    recognizedInk.ink.strokes[strokeIndex],
                    noSpacesText[textIndex]
                )
            )

        }
        textIndex++
        strokeIndex++
    }
}




