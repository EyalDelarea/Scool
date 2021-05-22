package com.scool.scoolstudent.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.digitalink.Ink
import com.scool.scoolstudent.realm.NotebookRealmObject
import com.scool.scoolstudent.realm.Task
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.annotations.RealmModule
import io.realm.kotlin.where

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Welcome, Eyal"
    }
    val text: LiveData<String> = _text
    lateinit var notebooks:RealmResults<NotebookRealmObject>




    val mdb: Realm = Realm.getDefaultInstance()
    val realmName: String = "Notebooks"
    val config = RealmConfiguration.Builder().name(realmName).build()
    val backgroundThreadRealm: Realm = Realm.getInstance(config)



    fun getNotebooksInstances() {
        // all tasks in the realm
         notebooks = backgroundThreadRealm.where<NotebookRealmObject>().findAll()
        Log.i("eyalo", "got tasks ${notebooks[0]?.content}")

    }

    fun paraseJSON(){

        val gson = Gson()

//        val test = gson.fromJson(notebooks[0]?.content,ContentJSON::class.java)
//        val myType = object : TypeToken<List<StrokeManager.RecognizedStroke>>() {}.type
//        val logs = gson.fromJson<ArrayList<StrokeManager.RecognizedStroke>>(notebooks[0]?.content, myType)

        Log.i("eyalo","debug")
    }


}



