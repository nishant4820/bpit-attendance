package com.bpitindia.attendance.addsubject

data class SubjectItem(val subjectCode: String, val subjectName: String) {

    override fun toString(): String {
        return "$subjectCode $subjectName"
    }
}
