package com.bpitindia.attendance.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize

class StudentsResponse : ArrayList<Student>()

@Entity(tableName = "students", primaryKeys = ["enrollmentNumber","date"])
@Parcelize
data class Student(
    @SerializedName("enrollment_number")
    @Expose
    var enrollmentNumber: String,

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
    var date: String,

    @SerializedName("subject")
    @Expose
    var subject: String? = null
) : Parcelable

data class StudentRequestBody(

    @SerializedName("record")
    @Expose
    var record: StudentsResponse? = null
)

class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int>? {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }
}