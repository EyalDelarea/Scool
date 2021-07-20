package com.scool.scoolstudent

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.DrawingView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import io.realm.*
import kotlinx.android.synthetic.main.notebook_screen.*


/** Main activity which creates a StrokeManager and connects it to the DrawingView.  */
class NotebookMainActivity : AppCompatActivity() {
    @JvmField
    @VisibleForTesting
    val strokeManager = StrokeManager()
    private lateinit var drawingView: DrawingView
    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.N)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notebook_screen)
        drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.setStrokeManager(strokeManager)
        val searchBar = findViewById<SearchView>(R.id.searchView)
        drawingMode(1)

        //Setup Stroke Manager
        strokeManager.setContentChangedListener(drawingView)
        strokeManager.setActiveModel("he") //default hebrew lang
        strokeManager.download()
        strokeManager.parentContext = this

        //Search function
        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(newText: String?): Boolean {
                strokeManager.searchInk(newText!!, drawingView)
                drawingView.invalidate()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                StrokeManager.matchingIndexes.clear()
                strokeManager.searchInk(newText, drawingView)
                drawingView.invalidate()
                return true
            }
        })
        searchBar.setOnCloseListener {
            strokeManager.resetSearchRect(drawingView)
        }

    } // end of onCrate

    fun makeToast(txt: String) {
        Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
    }

    fun internetSearch(view: View?) {
        //Map each word
        strokeManager.buildInternetSearchRect(drawingView)
        //Toggle flag
        drawingView.setInputtoFalse()
        drawingMode(0)
    }

    private fun drawingMode(int: Int) {
        if (int == 1) { //drawingMode
            internetBtn.setBackgroundResource(R.drawable.roundcorner)
            drawingBtn.setBackgroundResource(R.drawable.rounder_selected)
            makeToast("Now you can write on the screen again!")
        } else { //Internet searchMode
            internetBtn.setBackgroundResource(R.drawable.rounder_selected)
            drawingBtn.setBackgroundResource(R.drawable.roundcorner)
            makeToast("Now you can click on words to search them online!")
        }

    }

    fun drawingMode(view: View?) {
        drawingView.setInputtoTrue()
        strokeManager.clearSearchRect()
        drawingMode(1)

    }

    fun clearClick(view: View?) {
        strokeManager.reset()
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.clear()

    }

}
