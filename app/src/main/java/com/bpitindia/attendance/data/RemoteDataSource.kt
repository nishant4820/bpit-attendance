package com.bpitindia.attendance.data

import com.bpitindia.attendance.data.models.FacultySubjectsBody
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.network.ApiService
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun health() = apiService.health()

    fun login(body: Map<String, String>) = apiService.login(body)

    fun getSettings(token: String) = apiService.getSettings(token)

    fun checkForUpdates() = apiService.checkForUpdates()

//    fun getFacultyProfile(token: String, id: Int) = apiService.getFacultyProfile(token, id)

    fun getFacultySubjects(token: String) = apiService.getFacultySubjects(token)

//    fun getAllSubjects(token: String) = apiService.getAllSubjects(token)

    fun addFacultySubjects(token: String, body: FacultySubjectsBody) =
        apiService.addFacultySubjects(token, body)

    fun getStudentList(token: String, params: Map<String, String?>) =
        apiService.getStudentList(token, params)

    fun submitAttendance(token: String, body: StudentRequestBody) =
        apiService.submitAttendance(token, body)

    fun editAttendance(token: String, body: StudentRequestBody) =
        apiService.editAttendance(token, body)

    fun getAttendanceStats(token: String, params: Map<String, String?>) =
        apiService.getAttendanceStats(token, params)

    fun getLastSubmittedAttendance(token: String, params: Map<String, String?>) =
        apiService.getLastSubmittedAttendance(token, params)
}