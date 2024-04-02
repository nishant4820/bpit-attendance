package com.bpitindia.attendance.data.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Statistics(
    @SerializedName("columns")
    @Expose
    var columns: List<String>? = null,

    @SerializedName("student_data")
    @Expose
    var studentData: List<Student>? = null
) : Parcelable