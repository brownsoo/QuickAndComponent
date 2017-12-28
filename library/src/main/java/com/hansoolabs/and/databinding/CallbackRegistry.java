package com.hansoolabs.and.databinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloned from com.android.databinding
 */

public class CallbackRegistry<C, T, A> implements Cloneable {
    private static final String TAG = "CallbackRegistry";
    private List<C> mCallbacks = new ArrayList();
    private long mFirst64Removed = 0L;
    private long[] mRemainderRemoved;
    private int mNotificationLevel;
    private final CallbackRegistry.NotifierCallback<C, T, A> mNotifier;

    public CallbackRegistry(CallbackRegistry.NotifierCallback<C, T, A> notifier) {
        this.mNotifier = notifier;
    }

    public synchronized void notifyCallbacks(T sender, int arg, A arg2) {
        ++this.mNotificationLevel;
        this.notifyRecurse(sender, arg, arg2);
        --this.mNotificationLevel;
        if (this.mNotificationLevel == 0) {
            if (this.mRemainderRemoved != null) {
                for (int i = this.mRemainderRemoved.length - 1; i >= 0; --i) {
                    long removedBits = this.mRemainderRemoved[i];
                    if (removedBits != 0L) {
                        this.removeRemovedCallbacks((i + 1) * 64, removedBits);
                        this.mRemainderRemoved[i] = 0L;
                    }
                }
            }

            if (this.mFirst64Removed != 0L) {
                this.removeRemovedCallbacks(0, this.mFirst64Removed);
                this.mFirst64Removed = 0L;
            }
        }

    }

    private void notifyFirst64(T sender, int arg, A arg2) {
        int maxNotified = Math.min(64, this.mCallbacks.size());
        this.notifyCallbacks(sender, arg, arg2, 0, maxNotified, this.mFirst64Removed);
    }

    private void notifyRecurse(T sender, int arg, A arg2) {
        int callbackCount = this.mCallbacks.size();
        int remainderIndex =
                this.mRemainderRemoved == null ? -1 : this.mRemainderRemoved.length - 1;
        this.notifyRemainder(sender, arg, arg2, remainderIndex);
        int startCallbackIndex = (remainderIndex + 2) * 64;
        this.notifyCallbacks(sender, arg, arg2, startCallbackIndex, callbackCount, 0L);
    }

    private void notifyRemainder(T sender, int arg, A arg2, int remainderIndex) {
        if (remainderIndex < 0) {
            this.notifyFirst64(sender, arg, arg2);
        } else {
            long bits = this.mRemainderRemoved[remainderIndex];
            int startIndex = (remainderIndex + 1) * 64;
            int endIndex = Math.min(this.mCallbacks.size(), startIndex + 64);
            this.notifyRemainder(sender, arg, arg2, remainderIndex - 1);
            this.notifyCallbacks(sender, arg, arg2, startIndex, endIndex, bits);
        }

    }

    private void notifyCallbacks(T sender,
                                 int arg,
                                 A arg2,
                                 int startIndex,
                                 int endIndex,
                                 long bits) {
        long bitMask = 1L;

        for (int i = startIndex; i < endIndex; ++i) {
            if ((bits & bitMask) == 0L) {
                this.mNotifier.onNotifyCallback(this.mCallbacks.get(i), sender, arg, arg2);
            }

            bitMask <<= 1;
        }

    }

    public synchronized void add(C callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        } else {
            int index = this.mCallbacks.lastIndexOf(callback);
            if (index < 0 || this.isRemoved(index)) {
                this.mCallbacks.add(callback);
            }

        }
    }

    private boolean isRemoved(int index) {
        if (index < 64) {
            long maskIndex1 = 1L << index;
            return (this.mFirst64Removed & maskIndex1) != 0L;
        } else if (this.mRemainderRemoved == null) {
            return false;
        } else {
            int maskIndex = index / 64 - 1;
            if (maskIndex >= this.mRemainderRemoved.length) {
                return false;
            } else {
                long bits = this.mRemainderRemoved[maskIndex];
                long bitMask = 1L << index % 64;
                return (bits & bitMask) != 0L;
            }
        }
    }

    private void removeRemovedCallbacks(int startIndex, long removed) {
        int endIndex = startIndex + 64;
        long bitMask = -9223372036854775808L;

        for (int i = endIndex - 1; i >= startIndex; --i) {
            if ((removed & bitMask) != 0L) {
                this.mCallbacks.remove(i);
            }

            bitMask >>>= 1;
        }

    }

    public synchronized void remove(C callback) {
        if (this.mNotificationLevel == 0) {
            this.mCallbacks.remove(callback);
        } else {
            int index = this.mCallbacks.lastIndexOf(callback);
            if (index >= 0) {
                this.setRemovalBit(index);
            }
        }

    }

    private void setRemovalBit(int index) {
        if (index < 64) {
            long remainderIndex = 1L << index;
            this.mFirst64Removed |= remainderIndex;
        } else {
            int remainderIndex1 = index / 64 - 1;
            if (this.mRemainderRemoved == null) {
                this.mRemainderRemoved = new long[this.mCallbacks.size() / 64];
            } else if (this.mRemainderRemoved.length < remainderIndex1) {
                long[] bitMask = new long[this.mCallbacks.size() / 64];
                System.arraycopy(this.mRemainderRemoved, 0, bitMask, 0,
                        this.mRemainderRemoved.length);
                this.mRemainderRemoved = bitMask;
            }

            long bitMask1 = 1L << index % 64;
            this.mRemainderRemoved[remainderIndex1] |= bitMask1;
        }

    }

    public synchronized ArrayList<C> copyCallbacks() {
        ArrayList callbacks = new ArrayList(this.mCallbacks.size());
        int numListeners = this.mCallbacks.size();

        for (int i = 0; i < numListeners; ++i) {
            if (!this.isRemoved(i)) {
                callbacks.add(this.mCallbacks.get(i));
            }
        }

        return callbacks;
    }

    public synchronized void copyCallbacks(List<C> callbacks) {
        callbacks.clear();
        int numListeners = this.mCallbacks.size();

        for (int i = 0; i < numListeners; ++i) {
            if (!this.isRemoved(i)) {
                callbacks.add(this.mCallbacks.get(i));
            }
        }

    }

    public synchronized boolean isEmpty() {
        if (this.mCallbacks.isEmpty()) {
            return true;
        } else if (this.mNotificationLevel == 0) {
            return false;
        } else {
            int numListeners = this.mCallbacks.size();

            for (int i = 0; i < numListeners; ++i) {
                if (!this.isRemoved(i)) {
                    return false;
                }
            }

            return true;
        }
    }

    public synchronized void clear() {
        if (this.mNotificationLevel == 0) {
            this.mCallbacks.clear();
        } else if (!this.mCallbacks.isEmpty()) {
            for (int i = this.mCallbacks.size() - 1; i >= 0; --i) {
                this.setRemovalBit(i);
            }
        }

    }

    public synchronized CallbackRegistry<C, T, A> clone() {
        CallbackRegistry clone = null;

        try {
            clone = (CallbackRegistry) super.clone();
            clone.mFirst64Removed = 0L;
            clone.mRemainderRemoved = null;
            clone.mNotificationLevel = 0;
            clone.mCallbacks = new ArrayList();
            int e = this.mCallbacks.size();

            for (int i = 0; i < e; ++i) {
                if (!this.isRemoved(i)) {
                    clone.mCallbacks.add(this.mCallbacks.get(i));
                }
            }
        } catch (CloneNotSupportedException var4) {
            var4.printStackTrace();
        }

        return clone;
    }

    public abstract static class NotifierCallback<C, T, A> {
        public NotifierCallback() {
        }

        public abstract void onNotifyCallback(C var1, T var2, int var3, A var4);
    }
}

