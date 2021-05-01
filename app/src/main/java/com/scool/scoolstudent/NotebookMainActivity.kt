package com.scool.scoolstudent

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSortedSet
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.DrawingView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StatusTextView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import kotlinx.android.synthetic.main.activity_digital_ink_main.*
import kotlinx.android.synthetic.main.activity_digital_ink_main.view.*
import kotlinx.android.synthetic.main.drawing_view.*
import java.util.*


/** Main activity which creates a StrokeManager and connects it to the DrawingView.  */
class NotebookMainActivity : AppCompatActivity() {
    @JvmField
    @VisibleForTesting
    val strokeManager = StrokeManager()

    // private lateinit var languageAdapter: ArrayAdapter<ModelLanguageContainer>

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.N)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawing_view)
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val statusTextView = findViewById<StatusTextView>(
            R.id.statusTextView
        )
        drawingView.setStrokeManager(strokeManager)
        val searchBar = findViewById<SearchView>(R.id.searchView)

        statusTextView.setStrokeManager(strokeManager)
        strokeManager.setStatusChangedListener(statusTextView)
        strokeManager.setContentChangedListener(drawingView)
        //   strokeManager.setDownloadedModelsChangedListener(this)
        strokeManager.setClearCurrentInkAfterRecognition(true)
        strokeManager.setTriggerRecognitionAfterInput(false)
        //   strokeManager.refreshDownloadedModelsStatus()
        strokeManager.setActiveModel("he") //default hebrew lang
        strokeManager.download()


        //Search function
        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(newText: String?): Boolean {
                strokeManager.searchInk(newText!!, drawingView)
                drawingView.invalidate()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                strokeManager.searchInk(newText!!, drawingView)
                drawingView.invalidate()
                return true
            }
        })
        searchBar.setOnCloseListener {
            strokeManager.resetSearchRect(drawingView)
        }


    } // end of onCrate


    fun debugClick(v: View?) {
        strokeManager.testHashMap(drawingView = drawing_view)

    }

    fun downloadClick(v: View?) {
        strokeManager.download()
    }


    fun savePage(v: View?) {
        val test = strokeManager.getPageContent()
        Log.i("eyalo", "test stop")
    }


    //Sample function to re paint on canvas given data
    fun onLoadPage(v: View?) {
        val paintStyle = Paint()
        paintStyle.style = Paint.Style.STROKE
        paintStyle.color = Color.RED
        paintStyle.strokeJoin = Paint.Join.ROUND
        paintStyle.strokeCap = Paint.Cap.ROUND
        paintStyle.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            3.toFloat(),
            resources.displayMetrics
        )
        val textPaintRed = Paint(paintStyle)

        val test = strokeManager.getContent()
        drawingView.clear()
        textPaintRed.color = Color.RED// red.
        //draw ink on screen
        for (i in test) {
            drawingView.drawStroke(i.stroke, textPaintRed)
        }
        drawingView.invalidate()
    }


    fun clearClick(v: View?) {
        strokeManager.reset()
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.clear()
    }


    fun eraseClick(v: View?) {
        val drawingView = findViewById<DrawingView>(R.id.drawing_view)

        if (!drawingView.isEraseOn) {
            eraseButton.setBackgroundColor(Color.RED)
        } else {
            eraseButton.setBackgroundColor(Color.BLUE)
        }
        drawingView.onEraseClick();

    }

    fun colorPickerClicked(v: View?) {
        val drawingViewd = findViewById<DrawingView>(R.id.drawingView)
        drawingViewd.showColorPicker()
    }


    fun recognizeClick(view: View) {
        strokeManager.recognize()
    }

    fun undo(view:View?){
        strokeManager.undo()
    }
    fun redo(view:View){
        strokeManager.redo()
    }

}
