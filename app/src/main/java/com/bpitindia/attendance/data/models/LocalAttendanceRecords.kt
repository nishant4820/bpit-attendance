package com.bpitindia.attendance.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalAttendanceRecords(
    val subject:String?=null,
    val date:String?=null,
    val batch:String?=null
) : Parcelable