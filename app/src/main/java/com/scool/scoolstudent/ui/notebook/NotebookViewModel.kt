package com.scool.scoolstudent.ui.notebook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.scool.scoolstudent.R

class NotebookViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This will show all of your notebooks"
    }

    val text: LiveData<String> = _text

}