package com.hansoolabs.and.utils

import io.reactivex.*
import org.reactivestreams.Publisher

/**
 * Rx transformer
 * Created by brownsoo on 2017. 8. 20..
 */

open class Transformer<T, R> :
        ObservableTransformer<T, R>,
        FlowableTransformer<T, R>,
        SingleTransformer<T, R>,
        MaybeTransformer<T,R>,
        CompletableTransformer {

    override fun apply(upstream: Observable<T>): ObservableSource<R> {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun apply(upstream: Flowable<T>): Publisher<R> {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun apply(upstream: Single<T>): SingleSource<R> {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun apply(upstream: Maybe<T>): MaybeSource<R> {
        throw UnsupportedOperationException("Not implemented")
    }

    override fun apply(upstream: Completable): CompletableSource {
        throw UnsupportedOperationException("Not implemented")
    }

}