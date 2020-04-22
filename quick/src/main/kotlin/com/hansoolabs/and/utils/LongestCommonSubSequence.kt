package com.hansoolabs.and.utils

import java.util.ArrayList

/**
 * Created by vulpes on 2017. 3. 6..
 */

object LongestCommonSubSequence {

    interface AreObjectEquals<T, K> {
        fun areEquals(obj1: T, obj2: K): Boolean
    }

    private enum class Origin {
        TOP, LEFT, DIAGONAL, END
    }

    fun <T, K> find(a: List<T>, b: List<K>, comparator: AreObjectEquals<T, K>): List<T> {
        val sizeOfA = a.size
        val sizeOfB = b.size
        val LCS = Array(sizeOfA + 1) { IntArray(sizeOfB + 1) }
        val solution = Array<Array<Origin?>>(sizeOfA + 1) { arrayOfNulls(sizeOfB + 1) }

        for (i in 0..sizeOfB) {
            LCS[0][i] = 0
            solution[0][i] = Origin.END
        }

        for (i in 0 until sizeOfA) {
            LCS[i][0] = 0
            solution[i][0] = Origin.END
        }
        for (i in 1..sizeOfA) {
            for (j in 1..sizeOfB) {
                if (comparator.areEquals(a[i - 1], b[j - 1])) {
                    LCS[i][j] = LCS[i - 1][j - 1] + 1
                    solution[i][j] = Origin.DIAGONAL
                } else {
                    LCS[i][j] = Math.max(LCS[i - 1][j], LCS[i][j - 1])
                    if (LCS[i][j] == LCS[i - 1][j]) {
                        solution[i][j] = Origin.TOP
                    } else {
                        solution[i][j] = Origin.LEFT
                    }
                }
            }
        }
        var origin: Origin
        val answer = ArrayList<T>()
        var idxOfA = sizeOfA
        var idxOfB = sizeOfB

        origin = solution[idxOfA][idxOfB]!!
        while (origin != Origin.END) {
            if (origin == Origin.DIAGONAL) {
                answer.add(0, a[idxOfA - 1])
                idxOfA--
                idxOfB--
            } else if (origin == Origin.LEFT) {
                idxOfB--
            } else {
                idxOfA--
            }
            origin = solution[idxOfA][idxOfB]!!
        }
        return answer
    }
}
