package com.scool.scoolstudent.ui.notebook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.scool.scoolstudent.NotebookMainActivity
import com.scool.scoolstudent.R
import com.scool.scoolstudent.realm.NotebookRealmObject
import com.scool.scoolstudent.realm.notesObject.NotebookDataInstanceItem
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_notebooks.*
import kotlinx.android.synthetic.main.nav_header_main.*

class NotebookFragment : Fragment() {

    private lateinit var notebookViewModel: NotebookViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        notebookViewModel =
                ViewModelProvider(this).get(NotebookViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_notebooks, container, false)
        val infoText: TextView = root.findViewById(R.id.infoText)
        notebookViewModel.text.observe(viewLifecycleOwner, Observer {
            infoText.text = it
        })




        //setting up the FAB
        val fab: FloatingActionButton = root.findViewById(R.id.fab)
        fab.setOnClickListener { e ->
            val intent = Intent(context, NotebookMainActivity::class.java)
            startActivity(intent)
        }
        return root
    }



}