package com.hansoolabs.and.utils

import com.hansoolabs.and.error.BaseException
import com.hansoolabs.and.error.BaseError
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Created by hansu on 2017-08-16.
 */

object ErrorUtil {

    fun toCommonException(throwable: Throwable,
                          defaultValue: BaseException?): BaseException? {

        if (throwable is BaseException) {
            return throwable
        }

        if (throwable is ConnectException) {
            return BaseException(
                    BaseError.Sector.Network,
                    BaseError.Code.ConnectionError,
                    "Connect exception",
                    null,
                    throwable)
        }
        if (throwable is SocketTimeoutException) {
            return BaseException(
                    BaseError.Sector.Network,
                    BaseError.Code.Timeout,
                    "Timeout exception",
                    null,
                    throwable)
        }
        return defaultValue
    }

}