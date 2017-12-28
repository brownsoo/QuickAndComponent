package com.hansoolabs.and.databinding;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Cloned from com.android.databinding
 */

public class ObservableArrayList<T> extends ArrayList<T>
        implements ObservableList<T> {
    private transient ListChangeRegistry mListeners = new ListChangeRegistry();

    @Override
    public void addOnListChangedCallback(OnListChangedCallback listener) {
        if (mListeners == null) {
            mListeners = new ListChangeRegistry();
        }
        mListeners.add(listener);
    }

    @Override
    public void removeOnListChangedCallback(OnListChangedCallback listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }

    @Override
    public boolean silentAdd(T object) {
        return super.add(object);
    }

    @Override
    public void silentAdd(int index, T object) {
        super.add(index, object);
    }

    @Override
    public boolean silentAddAll(Collection<? extends T> collection) {
        return super.addAll(collection);
    }

    @Override
    public boolean silentAddAll(int index, Collection<? extends T> collection) {
        return super.addAll(index, collection);
    }

    @Override
    public void silentClear() {
        super.clear();
    }

    @Override
    public T silentRemove(int index) {
        return super.remove(index);
    }

    @Override
    public boolean silentRemove(Object object) {
        return super.remove(object);
    }

    @Override
    public void silentRemoveRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public T silentSet(int index, T object) {
        return super.set(index, object);
    }

    @Override
    public boolean add(T object) {
        boolean value = super.add(object);
        notifyAdd(size() - 1, 1);
        return value;
    }

    @Override
    public void add(int index, T object) {
        super.add(index, object);
        notifyAdd(index, 1);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        int oldSize = size();
        boolean added = super.addAll(collection);
        if (added && collection.size() > 0) {
            notifyAdd(oldSize, size() - oldSize);
        }
        return added;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> collection) {
        boolean added = super.addAll(index, collection);
        if (added) {
            notifyAdd(index, collection.size());
        }
        return added;
    }

    @Override
    public void clear() {
        int oldSize = size();
        super.clear();
        if (oldSize != 0) {
            notifyRemove(0, oldSize);
        }
    }

    @Override
    public T remove(int index) {
        T val = super.remove(index);
        notifyRemove(index, 1);
        return val;
    }

    @Override
    public boolean remove(Object object) {
        int index = indexOf(object);
        if (index >= 0) {
            remove(index);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        notifyRemove(fromIndex, toIndex - fromIndex);
    }

    @Override
    public T set(int index, T object) {
        T val = super.set(index, object);
        if (mListeners != null) {
            mListeners.notifyChanged(this, index, 1);
        }
        return val;
    }

    private void notifyAdd(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyInserted(this, start, count);
        }
    }

    private void notifyRemove(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyRemoved(this, start, count);
        }
    }

    public void notifyChange() {
        if (mListeners != null) {
            mListeners.notifyChanged(this);
        }
    }

    public void notifyChange(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyChanged(this, start, count);
        }
    }
}

