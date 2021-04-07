package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView

import android.os.Handler
import android.os.Message
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.RecognitionTask.RecognizedInk
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.Ink.Stroke
import java.util.ArrayList

/** Manages the recognition logic and the content that has been added to the current page.  */
class StrokeManager {
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

    /** Interface to register to be notified of changes in the downloaded model state.  */
    interface DownloadedModelsChangedListener {
        /** This method is called when the downloaded models changes.  */
        fun onDownloadedModelsChanged(downloadedLanguageTags: Set<String>)
    }

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


    // Managing ink currently drawn.
    private var strokeBuilder = Stroke.builder()
    private var inkBuilder = Ink.builder()
    private var stateChangedSinceLastRequest = false
    private var contentChangedListener: ContentChangedListener? = null
    private var statusChangedListener: StatusChangedListener? = null
    private var downloadedModelsChangedListener: DownloadedModelsChangedListener? = null
    private var triggerRecognitionAfterInput = true
    private var clearCurrentInkAfterRecognition = true
    private val textPaint: TextPaint = TextPaint()


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

    /**
     *This function breaks down and ink object into strokes which holds
     * corresponding chars.
     * NOTICE - Special Chars in HE which contains two strokes are handled.
     *
     * TODO handle combined letters like כ+ל and so on
     *
     * Adding the <Stroke,Char> object to strokeContent
     */
    private fun handleInkContent(recognizedInk: RecognizedInk) {
        //Add the whole ink to the inkContent
        inkContent.add(recognizedInk)
        //define the special heb chars to handle as two strokes
        val specialChars = arrayOf('ה', 'ת', 'א', 'ק')
        //remove spaces
        val noSpacesText = recognizedInk.text?.replace("\\s".toRegex(), "")

        var textIndex = 0 //iterate over the non spaces text
        var strokeIndex = 0 //iterate of the strokes array
        while (textIndex < noSpacesText?.length!! ) {
            strokeContent.add(
                RecognizedStroke(
                    recognizedInk.ink.strokes[strokeIndex],
                    noSpacesText[textIndex]
                )
            )
            //if we found one of the special chars,they hold two strokes
            if(specialChars.contains(noSpacesText[textIndex])){
                strokeIndex++
                strokeContent.add(
                    RecognizedStroke(
                        recognizedInk.ink.strokes[strokeIndex],
                        noSpacesText[textIndex]
                    )
                )

            }
            textIndex++
            strokeIndex++
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

    //TODO computeStrokeBoundingBox for more the one char - unite them
    fun searchInk(query: String, drawingView: DrawingView) {
        //find in content
        textPaint.color = -0x0000ff // yellow.
        textPaint.alpha = 70
        if (query != "") {
            for (i in strokeContent) {
                if (query.contains(i.ch!!)) {
                    Log.i("debug", "true")
                    //we found a match inside i
                    //get coordinates and mark place as found
                    val rect = DrawingView.computeStrokeBoundingBox(i.stroke)

                    drawingView.drawTextIntoBoundingBox("", rect, textPaint)
                } else {
                    Log.i("debug", "false")
                }
            }
        }
        //if yes - mark on screen
        //if no - not found
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
                if (isEraseOn) {
                    //TODO CRash when more then one index
                    inkBuilder.build()
                    for ((index, value) in inkContent.withIndex()) {
                        if (true) {
                            //Figure out where to delete
                            //In the content list or unRecognized list
                            //TODO implement a delete function from hashMap
                            if (recognitionTask == null) {
                                inkContent.removeAt(index)
                            } else {
                                //  unRecognizedContent.removeAt(index)
                            }
                        }
                    }
                    contentChangedListener?.onContentChanged()
                    resetCurrentInk()
                    updateContent()
                } else {
                    strokeBuilder.addPoint(Ink.Point.create(x, y, t))
                    inkBuilder.addStroke(strokeBuilder.build())
                    strokeBuilder = Stroke.builder()
                    stateChangedSinceLastRequest = true
                    // recognize()
                }
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

    fun setDownloadedModelsChangedListener(
        downloadedModelsChangedListener: DownloadedModelsChangedListener?
    ) {
        this.downloadedModelsChangedListener = downloadedModelsChangedListener
    }


    // Model downloading / deleting / setting.
    fun setActiveModel(languageTag: String) {
        status = modelManager.setModel(languageTag)
    }

    fun deleteActiveModel(): Task<Nothing?> {
        return modelManager
            .deleteActiveModel()
            .addOnSuccessListener { refreshDownloadedModelsStatus() }
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
            .addOnSuccessListener { refreshDownloadedModelsStatus() }
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

    fun refreshDownloadedModelsStatus() {
        modelManager
            .downloadedModelLanguages
            .addOnSuccessListener { downloadedLanguageTags: Set<String> ->
                downloadedModelsChangedListener?.onDownloadedModelsChanged(downloadedLanguageTags)
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




