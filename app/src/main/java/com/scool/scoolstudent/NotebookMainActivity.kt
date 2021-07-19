package com.scool.scoolstudent

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.scool.scoolstudent.realm.NotebookRealmObject
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.DrawingView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils.StatusTextView
import io.realm.*
import kotlinx.android.synthetic.main.drawing_view.*


/** Main activity which creates a StrokeManager and connects it to the DrawingView.  */
class NotebookMainActivity : AppCompatActivity() {
    @JvmField
    @VisibleForTesting
    val strokeManager = StrokeManager()
    private lateinit var backgroundThreadRealm: Realm
    private val realmName = "Notebooks"
    private lateinit var drawingView: DrawingView

    //info text
    //true = drawingMode false = Internet search
    private var textMode: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.N)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawing_view)
        drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.setStrokeManager(strokeManager)
        val searchBar = findViewById<SearchView>(R.id.searchView)
        drawingMode(1)
        //Setup database connection
        // val config = RealmConfiguration.Builder().name(realmName).build()
        // backgroundThreadRealm = Realm.getInstance(config)

        //Setup Stroke Manager
        //  statusTextView.setStrokeManager(strokeManager)
        //strokeManager.setStatusChangedListener(statusTextView)
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
          //  infoText.text =
         //       "1.Write on screen only in hebrew \n2.Without joined letters \n3.Search them on the screen!"
        } else { //Internet searchMode
            internetBtn.setBackgroundResource(R.drawable.rounder_selected)
            drawingBtn.setBackgroundResource(R.drawable.roundcorner)
            makeToast("Now you can click on words to search them online!")
      //      infoText.text = "Touch any word to search it online!"
        }

    }

    fun drawingMode(view: View?) {
        drawingView.setInputtoTrue()
        strokeManager.clearSearchRect()
        drawingMode(1)

    }


    fun savePage() {
        val savedNotebook = NotebookRealmObject()
        //TODO implement notebook name
        savedNotebook.name = "{${System.currentTimeMillis()}}"
        val gson = Gson()
        val json = gson.toJson(strokeManager.getInk()).toString()
        savedNotebook.content = json
        backgroundThreadRealm.executeTransactionAsync { transactionRealm ->
            transactionRealm.insert(savedNotebook)
        }
    }


    fun clearClick(view: View?) {
        strokeManager.reset()
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.clear()

    }


    fun colorPickerClicked(view: View?) {
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.showColorPicker()
    }


    fun recognizeClick(view: View?) {
        strokeManager.recognize()
    }

    fun undo(view: View?) {
        if (!strokeManager.undo()) {
            Toast.makeText(this, "Stack is empty", Toast.LENGTH_LONG).show()
        }
    }

    fun redo(view: View?) {
        if (!strokeManager.redo()) {
            Toast.makeText(this, "Nothing to restore", Toast.LENGTH_LONG).show()
        }
    }

}
