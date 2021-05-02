package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.Utils

import android.graphics.Rect
import android.text.TextPaint
import android.util.Log
import com.google.mlkit.vision.digitalink.Ink
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.DrawingView
import com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView.StrokeManager
import java.util.ArrayList

object UtilsFunctions {

    /**
     * Calculate bondingBox for a streak
     * build a new ink from the strokes and calculates rect
     * return rect
     */
     fun calBoundingRect(startIndex: Int, heightStreak: Int,strokeContent:MutableList<StrokeManager.RecognizedStroke>): Rect {
        val ink = Ink.builder();
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
     fun findBestMatches(matchingIndexes: MutableList<Int>, maxLength: Int): Pair<Int, Int> {
        //Find the largest streak
        var heightStreak = 0
        var count = 0;
        var startIndex = 0
        var shouldUpdate = true
        var bestMatchStartIndex = 0
        matchingIndexes.forEachIndexed { index, i ->
            if (index + 1 < matchingIndexes.size) {
                if (i + 1 == matchingIndexes[index + 1]) {
                    count++
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
        if (heightStreak > maxLength)
            heightStreak = maxLength
        return Pair(heightStreak, bestMatchStartIndex)
    }

     fun markTextOnScreen(
         query: String,
         drawingView: DrawingView,
         strokeContent: MutableList<StrokeManager.RecognizedStroke>,
         searchRect:MutableList<Rect>,
         textPaint:TextPaint
    ) {
        val matchingIndexes: MutableList<Int> = ArrayList()
        //Find all matching indexes in strokes
        strokeContent.forEachIndexed { index, recognizedStroke ->
            if (query.contains(recognizedStroke.ch!!)) {
                matchingIndexes.add(index)
            }
        }
        if (query != "") {
            //find the best matches for the query
            val (heightStreak, startIndex) = findBestMatches(matchingIndexes, query.length)
            Log.i("eyalo", "heightStreak : $heightStreak , startIndex : $startIndex ")
            //If we have a streak build a rect from few stokes
            //and then mark it
            if (heightStreak > 1) {
                val rect = calBoundingRect(startIndex, heightStreak,strokeContent)
                searchRect.add(rect)
                drawingView.drawTextIntoBoundingBox(searchRect, textPaint)
            } else {
                //No strokes , mark each match alone
                matchingIndexes.forEach {
                    val rect = DrawingView.computeStrokeBoundingBox(strokeContent[it].stroke)
                    searchRect.add(rect)
                    drawingView.drawTextIntoBoundingBox(searchRect, textPaint)
                }
            }
        }
    }
}