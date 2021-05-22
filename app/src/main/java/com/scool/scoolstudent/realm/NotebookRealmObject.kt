package com.scool.scoolstudent.realm

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.NotNull

open class NotebookRealmObject(

    @PrimaryKey
    var name: String = "notebook",

    //var notebookContent: MutableList<StrokeManager.RecognizedStroke> = ArrayList()

  var content : String = ""
//    var notebookContent: RealmList<StrokeManager.RecognizedStroke> = RealmList()

) : RealmObject()

