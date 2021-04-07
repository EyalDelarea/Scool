package com.scool.scoolstudent.ui.notebook

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.scool.scoolstudent.NotebookMainActivity
import com.scool.scoolstudent.R

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
        val textView: TextView = root.findViewById(R.id.text_gallery)
        notebookViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        val fab: FloatingActionButton = root.findViewById(R.id.fab)
        fab.setOnClickListener { e ->
            val intent = Intent(context, NotebookMainActivity::class.java)
            startActivity(intent)
        }
        return root
    }

}