package com.bpitindia.attendance.addsubject

data class BranchItem(val branch_code: String, val branch_name: String, val branch_slug: String) {

    override fun toString(): String {
        return branch_slug
    }
}
