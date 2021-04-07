package com.scool.scoolstudent.ui.notebook.notebookLogic.drawingView

import com.google.mlkit.vision.digitalink.Ink

/**
 * This class represents (x,y) for the HashMap as key
 * and value as a pointer to a specific Stroke
 *
 * In order to check collations we check on the HashMap if (x,y) exists , if it
 * does we can know the specific stroke -> via pointer.
 */
open class dataStructure(val x: Float,
                         val y: Float) {

}

/**
 * Stroke object containing the Stroke information ( which contains Points )
 * and a char which was classified to
 *
 * Optional attribute , pointer to different stroke
 * for special cases which some letters are build from more then one stroke
 */
open class strokeOjbect(val char: Char,
                        val stroke: Ink.Stroke, val strokePair: Any) {

    //TODO pointer to stroke ? or actually stroke?
    //TODO if pointer , to where ?

}

