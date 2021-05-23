package com.scool.scoolstudent.ui.notebook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.scool.scoolstudent.R
import com.scool.scoolstudent.realm.NotebookRealmObject
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.where

class NotebookViewModel : ViewModel() {

    val realmName: String = "Notebooks"
    val config = RealmConfiguration.Builder().name(realmName).build()
    val backgroundThreadRealm : Realm = Realm.getInstance(config)

    val notebooksList : RealmResults<NotebookRealmObject> = backgroundThreadRealm.where<NotebookRealmObject>().findAll()

    private val _text = MutableLiveData<String>().apply {
        value = "You have ${notebooksList.size} notebooks saved"
    }

    val text: LiveData<String> = _text



}