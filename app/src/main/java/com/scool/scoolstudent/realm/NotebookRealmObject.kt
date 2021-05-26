package com.scool.scoolstudent.realm


import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


open class NotebookRealmObject(
    //notebook name as ID
    @PrimaryKey
    var name: String = "notebook",
    //Notebook content as JSON
    var content: String = ""
    //TODO add bitmap preview

) : RealmObject()

