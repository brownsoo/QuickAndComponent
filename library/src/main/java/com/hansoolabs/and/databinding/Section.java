package com.hansoolabs.and.databinding;

import java.util.List;

/**
 * Created by vulpes on 2017. 2. 17..
 */

public final class Section<T> {
    private final String tag;
    private final ObservableList<T> items;
    private int index;
    private int size;
    private boolean visible;
    private Section<T> prevSection;
    private Section<T> nextSection;
    private boolean attached;
    private SectionedObservableList<T> parentList;

    public Section(String tag, boolean visible, List<T> items) {
        this.tag = tag;
        this.index = 0;
        this.visible = visible;
        if (items instanceof ObservableList) {
            this.items = (ObservableList<T>) items;
        } else {
            this.items = new ObservableArrayList<>();
            this.items.addAll(items);
        }
        this.size = items.size();
    }

    synchronized boolean isAttached() {
        return attached;
    }

    synchronized void attach(SectionedObservableList<T> parentList) {
        attached = true;
        this.parentList = parentList;

        List<Section<T>> sections = parentList.getSections();
        int sectionsSize = sections.size();
        Section<T> tail = sectionsSize == 0 ? null : sections.get(sectionsSize - 1);
        if (tail != null) {
            tail.setNextSection(this);
            setPrevSection(tail);
        }
        updateIndex();

        items.addOnListChangedCallback(callback);
        if (visible) {
            parentList.superAddAll(getItems());
            parentList.notifyAdd(
                    tag,
                    tail == null ? 0 : tail.getIndex() + tail.getVisibleSize(),
                    getItems().size());
        }
    }

    synchronized void detach() {
        if (!attached) {
            throw new RuntimeException("Section cannot be detached before attached");
        }
        attached = false;
        Section<T> prev = getPrevSection();
        Section<T> next = getNextSection();

        if (prev != null) {
            prev.setNextSection(next);
        }
        if (next != null) {
            next.setPrevSection(prev);
            next.updateIndex();
        }
        items.removeOnListChangedCallback(callback);
        if (visible) {
            parentList.superRemoveAll(getItems());
            parentList.notifyRemove(tag, getIndex(), getVisibleSize());
        }
        parentList = null;
    }

    public String getTag() {
        return tag;
    }

    public int getIndex() {
        return index;
    }

    private void setIndex(int index) {
        this.index = index;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        if (attached) {
            if (visible) {
                int size = items.size();
                setSize(size);
                Section<T> next = getNextSection();
                if (next != null) {
                    next.updateIndex();
                }

                int index = getIndex();
                int totalStart = index;

                parentList.superAddAll(totalStart, items);
                parentList.notifyAdd(tag, totalStart, size);
            } else {
                updateIndex();
                parentList.superRemoveRange(getIndex(), getIndex() + getSize());
                parentList.notifyRemove(tag, getIndex(), getSize());
            }
        }
    }

    public ObservableList<T> getItems() {
        return items;
    }

    public int getSize() {
        return size;
    }

    private void setSize(int size) {
        this.size = size;
    }

    public int getVisibleSize() {
        return visible ? getSize() : 0;
    }

    public Section<T> getNextSection() {
        return nextSection;
    }

    private void setNextSection(Section<T> section) {
        this.nextSection = section;
    }

    public Section<T> getPrevSection() {
        return prevSection;
    }

    private void setPrevSection(Section<T> section) {
        this.prevSection = section;
    }

    private void updateIndex() {
        Section<T> prev = prevSection;
        Section<T> next = nextSection;
        this.index = prev == null ? 0 : prev.getIndex() + prev.getVisibleSize();
        if (next != null) {
            next.updateIndex();
        }
    }

    private final ObservableList.OnListChangedCallback<ObservableList<T>> callback =
            new ObservableList.OnListChangedCallback<ObservableList<T>>() {

                @Override
                public void onChanged(ObservableList<T> ts) {
                    int prevSize = getVisibleSize();
                    setSize(ts.size());
                    int currSize = getVisibleSize();
                    if (!attached || !isVisible()) {
                        return;
                    }

                    Section<T> next = getNextSection();
                    if (next != null) {
                        next.updateIndex();
                    }
                    int index = getIndex();

                    parentList.superRemoveRange(index, index + prevSize);
                    parentList.superAddAll(index, ts);
                    if (prevSize > currSize) {
                        parentList.notifyChange(tag, index, currSize);
                        parentList.notifyRemove(tag, index + currSize, prevSize - currSize);
                    } else if (prevSize == currSize) {
                        parentList.notifyChange(tag, index, prevSize);
                    } else {
                        parentList.notifyChange(tag, index, prevSize);
                        parentList.notifyAdd(tag, index + prevSize, currSize - prevSize);
                    }
                }

                @Override
                public void onItemRangeChanged(ObservableList<T> ts, int start, int count) {
                    if (!attached || !isVisible()) {
                        return;
                    }
                    int totalStart = getIndex() + start;
                    List<T> subset = ts.subList(start, start + count);
                    for (int i = 0; i < count; i++) {
                        parentList.superSet(totalStart + i, subset.get(i));
                    }
                    parentList.notifyChange(tag, totalStart, count);
                }

                @Override
                public void onItemRangeInserted(ObservableList<T> ts,
                                                int start,
                                                int count) {
                    setSize(ts.size());
                    if (!attached || !isVisible()) {
                        return;
                    }
                    Section<T> next = getNextSection();
                    if (next != null) {
                        next.updateIndex();
                    }

                    List<T> subset = ts.subList(start, start + count);
                    int index = getIndex();
                    int totalStart = index + start;

                    parentList.superAddAll(totalStart, subset);
                    parentList.notifyAdd(tag, totalStart, count);
                }

                @Override
                public void onItemRangeMoved(ObservableList<T> ts,
                                             int from,
                                             int to,
                                             int count) {
                    onChanged(ts);
                    if (!attached || !isVisible()) {
                        return;
                    }
                    int index = getIndex();
                    parentList.notifyMoved(tag, from + index, to + index, count);
                }

                @Override
                public void onItemRangeRemoved(ObservableList<T> ts, int start, int count) {
                    setSize(ts.size());
                    if (!attached || !isVisible()) {
                        return;
                    }
                    Section<T> next = getNextSection();
                    if (next != null) {
                        next.updateIndex();
                    }
                    int totalStart = getIndex() + start;
                    parentList.superRemoveRange(totalStart, totalStart + count);
                    parentList.notifyRemove(tag, totalStart, count);
                }
            };
}

