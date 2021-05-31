package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.Ink.Stroke
import com.scool.scoolstudent.ui.notebook.notebookLogic.Components.InternetSearchRect
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager.RecognizedStroke
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.ModelManager
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.RecognitionTask
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.RecognitionTask.RecognizedInk
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.UtilsFunctions.markInternetRects
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.UtilsFunctions.markTextOnScreen
import kotlinx.serialization.*
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList


/** Manages the recognition logic and the content that has been added to the current page.  */
@Suppress("DEPRECATION")
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

    @JvmField
    @VisibleForTesting
    var modelManager =
        ModelManager()

    /** Helper class that stores an Stroke along with the corresponding recognized char.  */
    class RecognizedStroke internal constructor(val stroke: Stroke, val ch: Char)

    // For handling recognition and model downloading.
    private var recognitionTask: RecognitionTask? = null

    // Managing the recognition queue.
    //Holding <Ink,Text> object
    var inkContent: MutableList<RecognizedInk> = ArrayList()

    //Hold search rect views
    private val searchRect: MutableList<Rect> = ArrayList()

    //Hold internet search Rect
    private val internetSearchRect: MutableList<InternetSearchRect> = ArrayList()

    //Stack for the use of undo & redo
    private val strokeStack: ArrayDeque<RecognizedStroke> = ArrayDeque()

    //used to show dialogs from the stroke manger\ drawing view
    lateinit var parentContext: Context


    // Managing ink currently drawn.
    private var strokeBuilder = Stroke.builder()
    private var inkBuilder = Ink.builder()
    private var stateChangedSinceLastRequest = false
    private var contentChangedListener: ContentChangedListener? = null
    private var statusChangedListener: StatusChangedListener? = null
    private val textPaint: TextPaint = TextPaint()


    var status: String? = ""
        set(newStatus) {
            field = newStatus
            statusChangedListener?.onStatusChanged()
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
     * Search function
     * Reset all search rects.
     * For each word , separated by spaces " " , mark the test on the screen
     */
    fun searchInk(query: String, drawingView: DrawingView) {
        resetSearchRect(drawingView) //clear previous marks
        textPaint.color = -0x0000ff // yellow.
        textPaint.alpha = 70
        //for each world separate by spaces
        val delim = " "
        val list = query.split(delim)
        matchingIndexes.clear()
        //Find all matching indexes in strokes

        //Create new instance to handle duplicates
        val searchStrokeContent: MutableList<RecognizedStroke> = ArrayList()
        strokeContent.forEach {
            searchStrokeContent.add(it)
        }
        //For each word separately mark text on screen
        //When index is marked on screen, it is removed from the list
        for (i in list) {
            matchingIndexes.clear()
            //Find matching indexes
            searchStrokeContent.forEachIndexed { index, recognizedStroke ->
                if (i.contains(recognizedStroke.ch)) {
                    matchingIndexes.add(index)
                }
            }
            markTextOnScreen(i, drawingView, searchStrokeContent, searchRect, textPaint)
        }
    }

    /**
     * Undo function
     * NEED TO IMPLEMENT - stack for un recognized strokes
     */
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

    /**
     * Redo function
     * NEED TO IMPLEMENT - stack for un recognized strokes
     */
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

    /**
     * Handling an RecognizedInk object
     * Adding the ink object the array list of ink
     * Adding and analyzing each stroke to the corresponding char
     */
    private fun handleInkContent(recognizedInk: RecognizedInk) {
        //Add the whole ink to the inkContent
        inkContent.add(recognizedInk)
        //Add each stroke
        strokeContent.add(recognizedInk)
    }

    private fun updateContent() {
        var contentString = ""
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
        searchRect.clear()
        internetSearchRect.clear()
        recognitionTask?.cancel()
        status = ""
    }

    /**
     * Clears the search rect on screen
     */
    fun resetSearchRect(drawingView: DrawingView): Boolean {
        this.searchRect.clear()
        drawingView.invalidate()
        contentChangedListener?.onContentChanged()
        return true
    }

    private fun resetCurrentInk() {
        inkBuilder = Ink.builder()
        strokeBuilder = Stroke.builder()
        stateChangedSinceLastRequest = false
    }

    fun handleSearchRectTouch(x: Float, y: Float) {
        internetSearchRect.forEach {
            if (it.contains(x, y)) {
                val dialog: AlertDialog.Builder = AlertDialog.Builder(parentContext)
                dialog.setMessage("Search the web for : ${it.txt} ?")
                    .setPositiveButton("Search") { _, _ ->
                        //open web
                    }.setNegativeButton("Cancel") { dialog1, _ ->
                        dialog1.dismiss()
                    }

                dialog.create()
                dialog.show()

            }
        }
    }

    fun buildInternetSearchRect(drawingView: DrawingView) {
        val specialChars = arrayOf('ה', 'ת', 'א', 'ק')
        //Create new instance to handle duplicates
        val searchInkContent: MutableList<RecognizedInk> = ArrayList()
        inkContent.forEach {
            searchInkContent.add(it)
        }

        //iterate over all the text
        val text = StringBuilder()
        //Stroke Count
        var count = 0
        //From where to start
        var start = 0
        searchInkContent.forEach { ink ->
            ink.text?.forEach { char ->
                //Add to rect boundingBox
                if (specialChars.contains(char)) {
                    count += 2
                    text.append(char)
                } else {
                    if (char == ' ') {
                        //stop adding,reset
                        markInternetRects(
                            drawingView,
                            strokeContent,
                            internetSearchRect,
                            start,
                            count,
                            text.toString()
                        )
                        start += count
                        count = 0
                        text.clear()
                    } else {
                        count++
                        text.append(char)
                    }
                }

            }
            //last word
            markInternetRects(
                drawingView,
                strokeContent,
                internetSearchRect,
                start,
                count,
                text.toString()
            )
        }
    }

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
                recognize()
            }
            else -> // Indicate touch event wasn't handled.
                return false
        }
        return true
    }


    fun getContent(): MutableList<RecognizedStroke> {
        return strokeContent
    }

    fun getInk(): MutableList<RecognizedInk> {
        return inkContent
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


    companion object {
        @VisibleForTesting
        const//1000 default

        val CONVERSION_TIMEOUT_MS: Long = 1000

        //Find all matching indexes that matches the query
        val matchingIndexes: MutableList<Int> = ArrayList()

        //Holding <Stroke,Char> object
        val strokeContent: MutableList<RecognizedStroke> = ArrayList()


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
private fun MutableList<RecognizedStroke>.add(recognizedInk: RecognizedInk) {
    //remove spaces
    val noSpacesText = recognizedInk.text?.replace("\\s".toRegex(), "")!!
    val specialChars = arrayOf('ה', 'ת', 'א', 'ק')
    var textIndex = 0 //iterate over the non spaces text
    var strokeIndex = 0 //iterate of the strokes array
    while (textIndex < noSpacesText.length && strokeIndex < recognizedInk.ink.strokes.size) {
        add(
            RecognizedStroke(
                recognizedInk.ink.strokes[strokeIndex],
                noSpacesText[textIndex]
            )
        )
        //if we found one of the special chars,they hold two strokes
        if (specialChars.contains(noSpacesText[textIndex])) {
            strokeIndex++
            add(
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




