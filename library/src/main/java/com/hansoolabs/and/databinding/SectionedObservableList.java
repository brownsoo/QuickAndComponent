package com.hansoolabs.and.databinding;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vulpes on 2016. 10. 5..
 */

public class SectionedObservableList<T> extends ArrayList<T> implements ObservableList<T> {

    private transient ListChangeRegistry mListeners = new ListChangeRegistry();

    private final ArrayList<Section<T>> sections = new ArrayList<>();
    private final Map<String, Section<T>> sectionMap = new HashMap<>();

    public ArrayList<Section<T>> getSections() {
        return sections;
    }

    public Section<T> addSection(String tag, T item) {
        return addSection(tag, item, false);
    }

    public Section<T> addSection(String tag, T item, boolean hidden) {
        List<T> list = new ArrayList<>();
        list.add(item);
        return addSection(tag, list, hidden);
    }

    public synchronized Section<T> addSection(String tag, List<T> list) {
        return addSection(tag, list, false);
    }

    public synchronized Section<T> addSection(String tag, List<T> list, boolean hidden) {
        Section<T> section = new Section<>(tag, !hidden, list);
        section.attach(this);
        sections.add(section);
        sectionMap.put(tag, section);

        return section;
    }

    public synchronized boolean removeSection(String tag) {
        Section<T> section = sectionMap.remove(tag);
        if (section == null || !section.isAttached()) {
            return false;
        }
        section.detach();
        sections.remove(section);
        return true;
    }

    public synchronized void removeAllSections() {
        sections.clear();
        sectionMap.clear();
        superClear();
        notifyChange();
    }

    @Nullable
    public Section<T> getSection(String tag) {
        return sectionMap.get(tag);
    }

    public List<T> getSectionList(String tag) {
        Section<T> section = sectionMap.get(tag);
        if (section == null) {
            return null;
        }
        return section.getItems();
    }

    public void setSectionVisibility(String tag, boolean visible) {
        Section<T> section = sectionMap.get(tag);
        if (section == null) {
            return;
        }
        section.setVisible(visible);
    }

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

    protected void notifyAdd(String tag, int start, int count) {
        if (mListeners != null) {
            mListeners.notifyInserted(this, start, count);
        }
    }

    protected void notifyRemove(String tag, int start, int count) {
        if (mListeners != null) {
            mListeners.notifyRemoved(this, start, count);
        }
    }

    protected void notifyMoved(String tag, int start, int to, int count) {
        if (mListeners != null) {
            mListeners.notifyMoved(this, start, to, count);
        }
    }

    protected void notifyChange(String tag, int start, int count) {
        notifyChange(start, count);
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

    protected boolean superAdd(T object) {
        return super.add(object);
    }

    protected void superAdd(int index, T object) {
        super.add(index, object);
    }

    protected boolean superAddAll(Collection<? extends T> collection) {
        return super.addAll(collection);
    }

    protected boolean superAddAll(int index, Collection<? extends T> collection) {
        return super.addAll(index, collection);
    }

    protected void superClear() {
        super.clear();
    }

    protected T superRemove(int index) {
        return super.remove(index);
    }

    protected boolean superRemove(Object object) {
        return super.remove(object);
    }

    protected T superSet(int index, T object) {
        return super.set(index, object);
    }

    protected void superRemoveRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }

    protected boolean superRemoveAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public T get(int index) {
        int from;
        int to;
        Section<T> found = null;
        for (Section<T> section : sections) {
            if (!section.isVisible()) {
                continue;
            }
            from = section.getIndex();
            to = from + section.getSize();
            if (index >= from && index < to) {
                found = section;
                break;
            }
        }
        if (found == null) {
            throw new IndexOutOfBoundsException();
        }
        return found.getItems().get(index - found.getIndex());
    }

    @Override
    public boolean add(T object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public void add(int index, T object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> collection) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public void clear() {
        removeAllSections();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public T set(int index, T object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean silentAdd(T object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public void silentAdd(int index, T object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean silentAddAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean silentAddAll(int index, Collection<? extends T> collection) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public void silentClear() {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public T silentRemove(int index) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public boolean silentRemove(Object object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public void silentRemoveRange(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }

    @Override
    public T silentSet(int index, T object) {
        throw new UnsupportedOperationException("SectionedObservableList is read only");
    }
}
