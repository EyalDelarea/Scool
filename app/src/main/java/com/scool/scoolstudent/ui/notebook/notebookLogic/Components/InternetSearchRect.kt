package com.scool.scoolstudent.ui.notebook.notebookLogic.Components

import android.graphics.Rect

data class InternetSearchRect(
    val rect: Rect,
    val txt: String
) {
    fun contains(x: Float, y: Float): Boolean {
        return this.rect.contains(x.toInt(), y.toInt())
    }
}