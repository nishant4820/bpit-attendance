package com.bpitindia.attendance.data.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginResponse(
    @SerializedName("email")
    @Expose
    var email: String? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null,

    @SerializedName("token")
    @Expose
    var token: String? = null,

    @SerializedName("is_first_login")
    @Expose
    var isFirstLogin: Boolean? = null,

    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("image_url")
    @Expose
    var imageUrl: String? = null
) : Parcelable