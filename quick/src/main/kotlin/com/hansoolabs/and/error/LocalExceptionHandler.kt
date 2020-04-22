package com.hansoolabs.and.error

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * Exception Handler to handle locally specific case
 * Created by brownsoo on 2017. 8. 3..
 */

interface LocalExceptionHandler {
    fun handle(@NonNull throwable: Throwable, @Nullable data: Bundle?): Boolean
}