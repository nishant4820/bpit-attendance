package com.bpitindia.attendance.data.models


data class SubjectItem(val subjectCode: String, val subjectName: String) {

    override fun toString(): String {
        return "$subjectCode $subjectName"
    }
}

data class BranchItem(val branchCode: String, val branchName: String, val branchSlug: String) {

    override fun toString(): String {
        return branchSlug
    }
}

data class SpecializationItem(val id: Int, val specializationName: String) {

    override fun toString(): String {
        return specializationName
    }
}