package com.scool.scoolstudent

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.DrawingView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.StatusTextView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager.Companion.strokeContent
import kotlinx.android.synthetic.main.activity_digital_ink_main.*
import kotlinx.android.synthetic.main.drawing_view.*


/** Main activity which creates a StrokeManager and connects it to the DrawingView.  */
class NotebookMainActivity : AppCompatActivity() {
    @JvmField
    @VisibleForTesting
    val strokeManager = StrokeManager()

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
        strokeManager.setActiveModel("he") //default hebrew lang
        strokeManager.download()


        //Search function
        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(newText: String?): Boolean {
                strokeManager.searchInk(newText!!, drawingView)
                drawingView.invalidate()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                StrokeManager.matchingIndexes.clear()
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
        Log.i("eyalo", "test stop")

    }

    fun downloadClick(v: View?) {
        strokeManager.download()
    }


    fun savePage(v: View?) {
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
        if(! strokeManager.undo()){
            Toast.makeText(this,"Stack is empty",Toast.LENGTH_LONG).show()
        }
    }
    fun redo(view:View){
        if(!strokeManager.redo()){
            Toast.makeText(this,"Nothing to restore",Toast.LENGTH_LONG).show()
        }
    }

}
