package com.hansoolabs.and.rx

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class TextChanges(private val view: TextView): Observable<String>() {
    override fun subscribeActual(observer: Observer<in String>?) {
        observer?.let {
            if (!RxUtil.checkMainThread(observer))
                return
            val listener = Listener(view, it)
            observer.onSubscribe(listener)
        }
    }

    private class Listener(private val view: TextView,
                           private val observer: Observer<in String>)
        :MainThreadDisposable(), TextWatcher {

        private val subject = PublishSubject.create<String>()
        private var bag: Disposable? = null

        init {
            view.addTextChangedListener(this)
            subject.distinct()
                .subscribe {
                    observer.onNext(it)
                }
                .also { bag = it }
        }
        override fun onDispose() {
            view.removeTextChangedListener(this)
            bag?.dispose()
        }

        override fun afterTextChanged(s: Editable?) {
            if (!isDisposed) {
                subject.onNext(s?.toString() ?: "")
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }
}