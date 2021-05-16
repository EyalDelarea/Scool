package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView


import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.mlkit.vision.digitalink.Ink
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager.ContentChangedListener
import kotlin.math.max
import kotlin.math.min


/**
 * Main view for rendering content.
 *
 *
 * The view accepts touch inputs, renders them on screen, and passes the content to the
 * StrokeManager. The view is also able to draw content from the StrokeManager.
 */


class DrawingView @JvmOverloads constructor(
    context: Context?,
    attributeSet: AttributeSet? = null

) :
    View(context, attributeSet), ContentChangedListener {
    private val recognizedStrokePaint: Paint
    private var markerPaint: TextPaint
    private val erasePaint = TextPaint()
    private var preTextPaint = Paint()
    var isEraseOn = false
    private var currentBackgroundColor = 0x0000FF
    private var currentStrokePaint: Paint
    private val canvasPaint: Paint
    private val currentStroke: Path
    private lateinit var drawCanvas: Canvas
    private lateinit var canvasBitmap: Bitmap
    private lateinit var strokeManager: StrokeManager
    fun setStrokeManager(strokeManager: StrokeManager) {
        this.strokeManager = strokeManager
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        Log.i(TAG, "onSizeChanged")
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap)
        invalidate()
    }

    fun getCanvas(): Canvas {
        return drawCanvas
    }

    /**
     * Function to handle the flag of the erase state
     * and the paint to save the last paint.
     */
    fun onEraseClick() {
        if (!isEraseOn) {
            isEraseOn = true
            preTextPaint = currentStrokePaint
            currentStrokePaint = erasePaint
        } else {
            isEraseOn = false
            currentStrokePaint = preTextPaint
        }
    }

    fun clear() {
        currentStroke.reset()
        onSizeChanged(
            canvasBitmap.width,
            canvasBitmap.height,
            canvasBitmap.width,
            canvasBitmap.height
        )
    }

    override fun onDraw(canvas: Canvas) {
        Log.i(TAG, "onDraw")
        //Draw whole line to screen
        canvas.drawBitmap(canvasBitmap, 0f, 0f, canvasPaint)
        //Realtime draw
        canvas.drawPath(currentStroke, currentStrokePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val x = event.x
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> currentStroke.moveTo(x, y) //start
            MotionEvent.ACTION_MOVE -> currentStroke.lineTo(x, y) //on motion
            MotionEvent.ACTION_UP -> { //finished
                currentStroke.lineTo(x, y)
                drawCanvas.drawPath(currentStroke, currentStrokePaint)
                currentStroke.reset()
            }
            else -> {
                return false
            }
        }
        //Send info to strokeManger
        strokeManager.addNewTouchEvent(event, isEraseOn)
        //Calls onDraw to re-render the screen
        invalidate()
        return true
    }

    override fun onContentChanged() {
        redrawContent()
    }

    /**
     * Delete the entire screen and draw only the content which was recognized
     * Drawing strokes
     */
    private fun redrawContent() {
        clear()
        //Current ink or stroke specific ink ? not saves
        val content = strokeManager.getContent()
        for (ri in content) {
            drawStroke(ri.stroke, recognizedStrokePaint)
        }
        invalidate()
    }


//    fun drawInk(ink: Ink, paint: Paint) {
//        // Log.i("DEBUG", "DrawInk")
//        for (s in ink.strokes) {
//            drawStroke(s, paint)
//        }
//        invalidate()
//    }

    fun drawStroke(s: Ink.Stroke, paint: Paint) {
        // Log.i(TAG, "drawstroke")
        val path = Path()
        path.moveTo(s.points[0].x, s.points[0].y)
        for (p in s.points.drop(1)) {
            path.lineTo(p.x, p.y)
        }
        drawCanvas.drawPath(path, paint)
    }


    companion object {
        private const val TAG = "MLKD.DrawingView"
        private const val STROKE_WIDTH_DP = 3
        private const val MIN_BB_WIDTH = 10
        private const val MIN_BB_HEIGHT = 10
        private const val MAX_BB_WIDTH = 256
        private const val MAX_BB_HEIGHT = 256
        fun computeInkBoundingBox(ink: Ink): Rect {
            var top = Float.MAX_VALUE
            var left = Float.MAX_VALUE
            var bottom = Float.MIN_VALUE
            var right = Float.MIN_VALUE
            for (s in ink.strokes) {
                for (p in s.points) {
                    top = min(top, p.y)
                    left = min(left, p.x)
                    bottom = max(bottom, p.y)
                    right = max(right, p.x)
                }
            }
            return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }

        fun computeStrokeBoundingBox(s: Ink.Stroke): Rect {
            var top = Float.MAX_VALUE
            var left = Float.MAX_VALUE
            var bottom = Float.MIN_VALUE
            var right = Float.MIN_VALUE

            for (p in s.points) {
                top = min(top, p.y)
                left = min(left, p.x)
                bottom = max(bottom, p.y)
                right = max(right, p.x)
            }

            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            val bb =
                Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            // Enforce a minimum size of the bounding box such that recognitions for small inks are readable
            bb.union(
                (centerX - MIN_BB_WIDTH / 2).toInt(),
                (centerY - MIN_BB_HEIGHT / 2).toInt(),
                (centerX + MIN_BB_WIDTH / 2).toInt(),
                (centerY + MIN_BB_HEIGHT / 2).toInt()
            )
            // Enforce a maximum size of the bounding box, to ensure Emoji characters get displayed
            // correctly
            if (bb.width() > MAX_BB_WIDTH) {
                bb[bb.centerX() - MAX_BB_WIDTH / 2, bb.top, bb.centerX() + MAX_BB_WIDTH / 2] =
                    bb.bottom
            }
            if (bb.height() > MAX_BB_HEIGHT) {
                bb[bb.left, bb.centerY() - MAX_BB_HEIGHT / 2, bb.right] =
                    bb.centerY() + MAX_BB_HEIGHT / 2
            }
            return bb
        }
    }


    /**
     * Shows color picker dialog
     */
    fun showColorPicker() {
        ColorPickerDialogBuilder
            .with(context)
            .setTitle("Choose color")
            .initialColor(currentBackgroundColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
            .density(15)
            .setOnColorSelectedListener {
                currentStrokePaint.color = it
                recognizedStrokePaint.color = it
            }
            .setPositiveButton(
                "ok"
            )
            { dialog, selectedColor, allColors -> changeBackgroundColor(selectedColor) }
            .setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, which -> })
            .showColorPreview(true)
            .build()
            .show()
    }

    //Helper function for showColorPicker
    private fun changeBackgroundColor(color: Int) {
        currentBackgroundColor = color
    }

    fun drawSingleBoundingBox(rect: Rect, textPaint: TextPaint) {
        drawCanvas.drawRect(rect, textPaint)
    }

    init {
        currentStrokePaint = Paint()
        currentStrokePaint.color = Color.BLACK // black.
        currentStrokePaint.isAntiAlias = true
        // Set stroke width based on display density.
        currentStrokePaint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            STROKE_WIDTH_DP.toFloat(),
            resources.displayMetrics
        )
        currentStrokePaint.style = Paint.Style.STROKE
        currentStrokePaint.strokeJoin = Paint.Join.ROUND
        currentStrokePaint.strokeCap = Paint.Cap.ROUND
        recognizedStrokePaint = Paint(currentStrokePaint)
        recognizedStrokePaint.color = currentStrokePaint.color // black
        markerPaint = TextPaint()
        markerPaint.color = -0x0000ff // yellow.
        markerPaint.alpha = 80

        //eraser
        erasePaint.color = Color.WHITE
        erasePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        currentStroke = Path()
        canvasPaint = Paint(Paint.DITHER_FLAG)
    }
}
