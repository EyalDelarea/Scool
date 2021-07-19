@file:Suppress("UNUSED_VARIABLE")

package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.utils

import android.graphics.Rect
import android.text.TextPaint
import android.util.Log
import com.google.gson.Gson
import com.google.mlkit.vision.digitalink.Ink
import com.scool.scoolstudent.realm.NotebookRealmObject
import com.scool.scoolstudent.realm.notesObject.NotebookDataInstanceItem
import com.scool.scoolstudent.ui.notebook.notebookLogic.Components.InternetSearchRect
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.DrawingView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import io.realm.RealmResults


object UtilsFunctions {

    /**
     * Calculate bondingBox for a streak
     * build a new ink from the strokes and calculates rect
     * return rect
     */
    private fun calBoundingRect(
        startIndex: Int,
        heightStreak: Int,
        strokeContent: MutableList<StrokeManager.RecognizedStroke>
    ): Rect {
        val ink = Ink.builder()
        if (startIndex + heightStreak < strokeContent.size) {
            for (s in startIndex..(startIndex + heightStreak)) {
                ink.addStroke(strokeContent[s].stroke)
            }
        }
        val doneInk = ink.build()
        return DrawingView.computeInkBoundingBox(doneInk)
    }

    /**
     * Function to find the longest matching chars in the list
     * returns the startIndex and the amount
     */
    private fun findBestMatches(
        matchingIndexes: MutableList<Int>,
        query: String,
        strokeContent: MutableList<StrokeManager.RecognizedStroke>
    )
            : Pair<Int, Int> {
        //Find the largest streak
        var heightStreak = 0
        var myQuery = query
        var count = 0
        var startIndex = 0
        var shouldUpdate = true
        var bestMatchStartIndex = 0
        matchingIndexes.forEachIndexed { index, i ->
            if (index + 1 < matchingIndexes.size) { //make sure index is not out of bounds
                if (i + 1 == matchingIndexes[index + 1]) {
                    count++
                    //remove the selected char from the query
                    myQuery = myQuery.replace(strokeContent[i].ch, "")
                        .toString()
                    //if the query is empty , end the function
                    if (myQuery.isEmpty()) {
                        return Pair(heightStreak, bestMatchStartIndex)
                    }
                    if (shouldUpdate) {
                        startIndex = i
                        shouldUpdate = false
                    }
                } else {
                    shouldUpdate = true
                    count = 0
                }
                if (heightStreak < count) {
                    heightStreak = count
                    bestMatchStartIndex = startIndex
                }
            }
        }
        //limit the streak to maxLength of the quote
        if (heightStreak > query.length)
            heightStreak = query.length
        return Pair(heightStreak, bestMatchStartIndex)
    }

    /**
     * Marks the text on screen (yellow highlight)
     * Query - query sting
     * Drawing view - the view which will be highlighted
     * strokeContent - <Stroke,Char> list
     * searchRect - list of highlighted Rect already shown
     * textPaint - the color of the rect
     *
     * First we search for matches inside the stroke content list
     * then we look for the highest streak of matches to mark them
     */
    fun markTextOnScreen(
        query: String,
        drawingView: DrawingView,
        searchStrokeContent: MutableList<StrokeManager.RecognizedStroke>,
        searchRect: MutableList<Rect>,
        textPaint: TextPaint,
    ) {
        if (query != "") {
            //find the best matches for the query
            val (heightStreak, startIndex) = findBestMatches(
                StrokeManager.matchingIndexes,
                query,
                searchStrokeContent
            )
            //If we have a streak build a rect from few stokes and mark it
            if (heightStreak > 0) {
                val rect = calBoundingRect(startIndex, heightStreak, searchStrokeContent)
                searchRect.add(rect) //add to rect stack
                drawingView.drawSingleBoundingBox(rect, textPaint) //draw rect
                removeMarkedStrokesFromList(startIndex, heightStreak, searchStrokeContent)
            } else {
                //Only one char matches
                StrokeManager.matchingIndexes.forEach {
                    val rect = DrawingView.computeStrokeBoundingBox(searchStrokeContent[it].stroke)
                    searchRect.add(rect)
                    drawingView.drawSingleBoundingBox(rect, textPaint)
                }
            }
        }
    }

    /**
     * Marks the text on screen (yellow highlight)
     * Drawing view - the view which will be highlighted
     * strokeContent - <Stroke,Char> list
     * searchRect - list of highlighted Rect already shown
     *
     */
    fun markInternetRects(
        drawingView: DrawingView,
        searchStrokeContent: MutableList<StrokeManager.RecognizedStroke>,
        searchRect: MutableList<InternetSearchRect>,
        start: Int,
        count: Int,
        word: String
    ) {
        val textPaint = TextPaint() //check if it alpha is low
        textPaint.alpha = 0

        val rect = calBoundingRect(start, count - 1, searchStrokeContent)
        searchRect.add(InternetSearchRect(rect, word)) //add to rect stack
        drawingView.drawSingleBoundingBox(rect, textPaint) //draw rect
    }

    private fun removeMarkedStrokesFromList(
        startIndex: Int,
        heightStreak: Int,
        searchStrokeContent: MutableList<StrokeManager.RecognizedStroke>
    ) {
        //TODO Fix if the query doesn't start from 0

        if (startIndex == heightStreak) {
            searchStrokeContent.removeAt(startIndex)
        } else {
            for ((counter, j) in (startIndex..startIndex + heightStreak).withIndex()) {
                searchStrokeContent.removeAt(j - counter)
            }
        }

    }
}

/**
 * Overwrite function to replace string only the first appearance
 */
private fun String.replace(oldChar: Char?, newChar: String): Any {
    return buildString(length) {
        var count = 0 //make sure we remove only one char
        this@replace.forEach { c ->
            append(
                if (c == oldChar && count == 0) {
                    count++
                    newChar
                } else c
            )
        }
    }
}

/**
 *
 */
fun buildContent(notebooks: RealmResults<NotebookRealmObject>): Pair<Ink, String> {
    try {
        var strokeBuilder = Ink.Stroke.builder()
        val inkBuilder = Ink.builder()
        val gson = Gson()
        //Get the content of the notebook
        val jsonData = notebooks[0]?.content
        //Set the data
        val data: List<NotebookDataInstanceItem> =
            gson.fromJson(jsonData, Array<NotebookDataInstanceItem>::class.java).toList()
        //Build ink object from data
        data[0].ink.zza.forEach { stroke ->
            stroke.zza.forEach { p ->
                //build point
                strokeBuilder.addPoint(
                    Ink.Point.create(
                        p.zza.toFloat(),
                        p.zzb.toFloat(),
                        p.zzc
                    )
                )
            }
            //build stroke
            inkBuilder.addStroke(strokeBuilder.build())
            //set new stroke
            strokeBuilder = Ink.Stroke.builder()
        }
        //build ink object
        val ink = inkBuilder.build()
        return Pair(ink, data[0].text)
    } catch (e: Exception) {
        //No database or any such error
        Log.i("eyalo", "$e")
    }
    return Pair(Ink.builder().build(), "0")
}



