package com.bpitindia.attendance.data.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

class StudentsResponse : ArrayList<Student>()

@Parcelize
data class Student(
    @SerializedName("enrollment_number")
    @Expose
    var enrollmentNumber: String? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null,

    @SerializedName("class_roll_number")
    @Expose
    var classRollNumber: String? = null,

    @SerializedName("attendance_count")
    @Expose
    var attendanceCount: Int? = null,

    @SerializedName("attendance_data")
    @Expose
    var attendanceData: List<Int>? = null,

    @SerializedName("status")
    @Expose
    var status: Boolean? = null,

    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("batch")
    @Expose
    var batch: String? = null,

    @SerializedName("date")
    @Expose
    var date: String? = null,

    @SerializedName("subject")
    @Expose
    var subject: String? = null
) : Parcelable

data class StudentRequestBody(

    @SerializedName("record")
    @Expose
    var record: StudentsResponse? = null
)