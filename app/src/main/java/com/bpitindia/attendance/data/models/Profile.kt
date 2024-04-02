package com.bpitindia.attendance.data.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FacultyProfile(
    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("email")
    @Expose
    var email: String? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null,

    @SerializedName("phone_number")
    @Expose
    var phoneNumber: String? = null,

    @SerializedName("is_staff")
    @Expose
    var isStaff: Boolean? = null,

    @SerializedName("is_superuser")
    @Expose
    var isSuperuser: Boolean? = null,

    @SerializedName("is_active")
    @Expose
    var isActive: Boolean? = null,

    @SerializedName("designation")
    @Expose
    var designation: String? = null,

    @SerializedName("date_joined")
    @Expose
    var dateJoined: String? = null,

    @SerializedName("image_url")
    @Expose
    var imageUrl: String? = null
) : Parcelable