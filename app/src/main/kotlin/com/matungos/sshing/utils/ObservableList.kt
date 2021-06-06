package com.matungos.sshing.utils

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Deprecated
 * Used only for serialization compatibility
 */
open class ObservableList<T> {

    @SerializedName("list")
    @Expose
    var list: MutableList<T> = ArrayList()

}
