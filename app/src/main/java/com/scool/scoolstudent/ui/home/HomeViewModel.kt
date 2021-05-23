package com.scool.scoolstudent.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.scool.scoolstudent.realm.NotebookRealmObject
import com.scool.scoolstudent.realm.notesObject.NotebookDataInstanceItem
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.where

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Welcome, Eyal"
    }
    val text: LiveData<String> = _text

}



