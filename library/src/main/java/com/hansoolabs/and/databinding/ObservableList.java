package com.hansoolabs.and.databinding;

import java.util.Collection;
import java.util.List;

/**
 * Cloned from com.android.databinding
 */

public interface ObservableList<T> extends List<T> {

    void addOnListChangedCallback(ObservableList.OnListChangedCallback<? extends ObservableList<T>> callback);

    void removeOnListChangedCallback(ObservableList.OnListChangedCallback<? extends ObservableList<T>> callback);


    boolean silentAdd(T object);

    void silentAdd(int index, T object);

    boolean silentAddAll(Collection<? extends T> collection);

    boolean silentAddAll(int index, Collection<? extends T> collection);

    void silentClear();

    T silentRemove(int index);

    boolean silentRemove(T object);

    void silentRemoveRange(int fromIndex, int toIndex);

    T silentSet(int index, T object);

    abstract class OnListChangedCallback<T extends ObservableList> {

        public OnListChangedCallback() {}

        public abstract void onChanged(T list);

        public abstract void onItemRangeChanged(T list, int index, int count);

        public abstract void onItemRangeInserted(T list, int index, int count);

        public abstract void onItemRangeMoved(T list, int fromIndex, int toIndex, int count);

        public abstract void onItemRangeRemoved(T list, int index, int count);
    }
}

