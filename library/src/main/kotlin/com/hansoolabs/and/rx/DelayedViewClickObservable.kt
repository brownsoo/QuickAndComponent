package com.hansoolabs.and.rx

import android.view.View
import com.jakewharton.rxbinding2.internal.Notification
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Delayed click to prevent fast touches
 * Created by brownsoo on 2017. 9. 11..
 */

class DelayedViewClickObservable(private val view: View) : Observable<Any>() {

    override fun subscribeActual(observer: Observer<in Any>?) {
        observer?.let {
            if (!RxUtil.checkMainThread(observer))
                return
            val listener = Listener(view, it)
            observer.onSubscribe(listener)
        }
    }

    private class Listener(
        private val view: View,
        private val observer: Observer<Any>
    ) : MainThreadDisposable(), View.OnClickListener {

        private val subject = PublishSubject.create<Any>()
        private var bag: Disposable? = null

        init {
            view.setOnClickListener(this)
            subject.throttleFirst(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    observer.onNext(Notification.INSTANCE)
                }
                .also { bag = it }
        }

        override fun onDispose() {
            view.setOnClickListener(null)
            bag?.dispose()
        }

        override fun onClick(view: View?) {
            if (!isDisposed) {
                subject.onNext(Notification.INSTANCE)
            }
        }

    }

}