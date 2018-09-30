package com.hansoolabs.and.error

import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * Created by brownsoo on 2017. 8. 3..
 */

class BaseException(val error: BaseError) : RuntimeException(error.sector.message) {

    constructor(@NonNull sector: BaseError.Sector,
                @NonNull code: BaseError.Code,
                @NonNull message: String) : this(BaseError(sector, code, message))

    constructor(@NonNull sector: BaseError.Sector,
                @NonNull code: BaseError.Code,
                @NonNull message: String,
                @Nullable facingMessage: String?) : this(BaseError(sector, code, message, facingMessage))

    constructor(@NonNull sector: BaseError.Sector,
                @NonNull code: BaseError.Code,
                @NonNull message: String,
                @Nullable facingMessage: String?,
                @Nullable cause: Throwable?) : this(BaseError(sector, code, message, facingMessage, cause))

    val facingMessage = error.facingMessage
}