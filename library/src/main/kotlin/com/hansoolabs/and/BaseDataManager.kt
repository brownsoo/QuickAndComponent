package com.hansoolabs.and

import com.hansoolabs.and.databinding.Section

/**
 *
 * Created by brownsoo on 2017. 5. 13..
 */

interface BaseDataManager<T> {

    fun add(tag: String, item: T)
    fun addAll(tag: String, list: List<T>)
    fun removeAll(tag: String): Boolean

    fun createSection(tag: String, hidden: Boolean): Section<T>
    fun createSection(tag: String, list: List<T>, hidden: Boolean): Section<T>
    fun getSections(): List<Section<T>>
    fun removeAllSections()
    fun getSection(tag: String): Section<out T>?
    fun setSectionVisibility(tag: String, visible: Boolean)

}
