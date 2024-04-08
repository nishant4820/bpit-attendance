package com.bpitindia.attendance.data.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


class FacultySubjectsResponse : ArrayList<FacultySubject>()

@Parcelize
data class FacultySubject(
    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("batch")
    @Expose
    var batch: String? = null,

    @SerializedName("branch_code")
    @Expose
    var branchCode: String? = null,

    @SerializedName("branch_name")
    @Expose
    var branchName: String? = null,

    @SerializedName("branch_slug")
    @Expose
    var branchSlug: String? = null,

    @SerializedName("section")
    @Expose
    var section: String? = null,

    @SerializedName("class_batch")
    @Expose
    var classBatch: String? = null,

    @SerializedName("specialization")
    @Expose
    var specialization: Int? = null,

    @SerializedName("specialization_name")
    @Expose
    var specializationName: String? = null,

    @SerializedName("is_lab")
    @Expose
    var isLab: Boolean? = null,

    @SerializedName("group")
    @Expose
    var group: String? = null,

    @SerializedName("subject_code")
    @Expose
    var subjectCode: String? = null,

    @SerializedName("subject_name")
    @Expose
    var subjectName: String? = null,

    @SerializedName("semester")
    @Expose
    var semester: String? = null
) : Parcelable

data class FacultySubjectsBody(

    @SerializedName("subjects")
    @Expose
    var subjects: FacultySubjectsResponse? = null
)