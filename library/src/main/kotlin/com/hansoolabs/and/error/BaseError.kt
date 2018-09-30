package com.hansoolabs.and.error

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * Created by brownsoo on 2017. 8. 3..
 */

class BaseError(@NonNull val sector: Sector,
                @NonNull val code: Code,
                @NonNull val message: String,
                @Nullable val facingMessage: String?,
                @Nullable val cause: Throwable?) : Parcelable {

    enum class Sector(val code: Int, val message: String) {
        Network(1000, "Network error"),
        Server(2000, "Server generated an error"),
        Internal(3000, "Internal error"),
        Initialization(4000, "Initialization error"),
    }

    enum class Code(@Suppress("UNUSED_PARAMETER") name: String) {
        IllegalState("Illegal State"),
        InvalidArgument("Invalid Argument"),

        ConnectionError("Connection Error"),
        Timeout("Timeout"),
        RequestCancelled("Request Cancelled"),

        SessionExpired("Session Expired"),
        ConcurrentLogin("Concurrent Login"),
        DeactivatedUser("Deactivated User"),

        UnknownError("Unknown Error")
    }

    constructor(@NonNull sector: Sector,
                @NonNull code: Code,
                @NonNull message: String) : this(sector, code, message, null, null)

    constructor(@NonNull sector: Sector,
                @NonNull code: Code,
                @NonNull message: String,
                @Nullable facingMessage: String?) : this(sector, code, message, facingMessage, null)

    constructor(parcel: Parcel) : this(
            Sector.values()[parcel.readInt()],
            Code.values()[parcel.readInt()],
            parcel.readString(),
            parcel.readString(),
            parcel.readSerializable() as Throwable)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(sector.ordinal)
        parcel.writeInt(code.ordinal)
        parcel.writeString(message)
        parcel.writeString(facingMessage)
        parcel.writeSerializable(cause)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BaseError> {
        override fun createFromParcel(parcel: Parcel): BaseError {
            return BaseError(parcel)
        }

        override fun newArray(size: Int): Array<BaseError?> {
            return arrayOfNulls(size)
        }
    }
}