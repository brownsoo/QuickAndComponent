package com.hansoolabs.and

import android.support.v7.widget.RecyclerView
import com.hansoolabs.and.databinding.ObservableList
import com.hansoolabs.and.databinding.Section
import com.hansoolabs.and.databinding.SectionedObservableList
import java.util.ArrayList

/**
 *
 * Created by brownsoo on 2017. 5. 14..
 */


abstract class AbsDataAdapter<T> :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        BaseDataManager<T> {

    private val items: SectionedObservableList<T> = SectionedObservableList()
    private val onListChangedCallback = object : ObservableList.OnListChangedCallback<ObservableList<T>>() {
        override fun onChanged(list: ObservableList<T>) {
            notifyDataSetChanged()
        }

        override fun onItemRangeChanged(list: ObservableList<T>, index: Int, count: Int) {
            if (count > 0) {
                notifyItemRangeChanged(index, count)
                onAfterItemRangeChanged(index, count)
            }
        }

        override fun onItemRangeInserted(list: ObservableList<T>, index: Int, count: Int) {
            if (count > 0) {
                notifyItemRangeInserted(index, count)
                onAfterItemRangeInserted(index, count)
            }
        }

        override fun onItemRangeMoved(list: ObservableList<T>,
                                      fromIndex: Int,
                                      toIndex: Int,
                                      count: Int) {
            if (count > 0) {
                notifyItemMoved(fromIndex, toIndex)
                onAfterItemMoved(fromIndex, toIndex)
            }
        }

        override fun onItemRangeRemoved(list: ObservableList<T>, index: Int, count: Int) {
            if (count > 0) {
                notifyItemRangeRemoved(index, count)
                onAfterItemRangeRemoved(index, count)
            }
        }
    }

    init {
        items.addOnListChangedCallback(onListChangedCallback)
    }

    protected abstract fun onBindViewHolder(holder: RecyclerView.ViewHolder,
                                            item: T,
                                            position: Int)

    abstract fun getItemViewType(item: T, position: Int): Int

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, items[position], position)
    }

    override fun getItemViewType(position: Int): Int {
        return getItemViewType(items[position], position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun add(tag: String, item: T) {
        var section = items.getSection(tag)
        if (section == null) {
            section = createSection(tag, false)
        }
        section.items.add(item)
    }

    override fun addAll(tag: String, list: List<T>) {
        var section = items.getSection(tag)
        if (section == null) {
            section = createSection(tag, false)
        }
        section.items.addAll(list)
    }

    override fun removeAll(tag: String): Boolean {
        return items.removeSection(tag)
    }

    override fun createSection(tag: String, hidden: Boolean): Section<T> {
        return items.addSection(tag, ArrayList<T>(), hidden)
    }

    override fun createSection(tag: String, list: List<T>, hidden: Boolean): Section<T> {
        return items.addSection(tag, list, hidden)
    }

    override fun getSections(): List<Section<T>> {
        return items.sections
    }

    override fun removeAllSections() {
        items.removeAllSections()
    }

    override fun getSection(tag: String): Section<T>? {
        return items.getSection(tag)
    }

    override fun setSectionVisibility(tag: String, visible: Boolean) {
        items.setSectionVisibility(tag, visible)
    }

    // own


    protected open fun onAfterItemRangeChanged(index: Int, count: Int) {}

    protected open fun onAfterItemRangeInserted(index: Int, count: Int) {}

    protected open fun onAfterItemMoved(fromIndex: Int, toIndex: Int) {}

    protected open fun onAfterItemRangeRemoved(index: Int, count: Int) {}


}
