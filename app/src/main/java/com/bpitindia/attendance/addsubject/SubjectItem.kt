package com.bpitindia.attendance.addsubject

data class SubjectItem(val subject_code: String, val subject_name: String) {

    override fun toString(): String {
        return "$subject_code $subject_name"
    }
}
