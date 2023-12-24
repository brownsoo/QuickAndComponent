package com.hansoolabs.and.rx

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableSource
import io.reactivex.rxjava3.core.CompletableTransformer
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableTransformer
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeSource
import io.reactivex.rxjava3.core.MaybeTransformer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleSource
import io.reactivex.rxjava3.core.SingleTransformer
import org.reactivestreams.Publisher

/**
 * Rx transformer
 * Created by brownsoo on 2017. 8. 20..
 */

open class Transformer<T : Any, R : Any> :
    ObservableTransformer<T, R>,
    FlowableTransformer<T, R>,
    SingleTransformer<T, R>,
    MaybeTransformer<T, R>,
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