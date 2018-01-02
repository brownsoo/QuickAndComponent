package com.hansoolabs.and.utils

import io.reactivex.*
import org.reactivestreams.Publisher

/**
 * take a item T under the condition R observable emits item that is
 * same with 'untilEvent' value
 * Created by brownsoo on 2017. 8. 20..
 */

class DelayUntilTransformer<T, R> private constructor(
        private val eventOb: Observable<R>,
        private val untilEvent: R) : Transformer<T, T>() {

    override fun apply(upstream: Completable): CompletableSource {
        return upstream.andThen {
            eventOb.filter { untilEvent == it }
                    .take(1)
                    .ignoreElements()
        }
    }

    override fun apply(upstream: Flowable<T>): Publisher<T> {
        return upstream.flatMap { t ->
            eventOb.filter { untilEvent == it }
                    .take(1)
                    .toFlowable(BackpressureStrategy.ERROR)
                    .map { t }
        }
    }

    override fun apply(upstream: Single<T>): SingleSource<T> {
        return upstream.flatMap { t ->
            eventOb.filter { untilEvent == it }
                    .take(1)
                    .map { t }
                    .firstOrError()
        }
    }

    override fun apply(upstream: Maybe<T>): MaybeSource<T> {
        return upstream.flatMap { t ->
            eventOb.filter { untilEvent == it }
                    .take(1)
                    .map { t }
                    .firstElement()
        }
    }

    override fun apply(upstream: Observable<T>): ObservableSource<T> {
        return upstream.flatMap { t ->
            eventOb.filter { untilEvent == it }
                    .take(1)
                    .map { t }
        }
    }

    companion object {
        @JvmStatic
        fun <T, R> create(eventOb: Observable<R>, untilEvent: R): DelayUntilTransformer<T, R> =
                DelayUntilTransformer(eventOb, untilEvent)
    }
}
