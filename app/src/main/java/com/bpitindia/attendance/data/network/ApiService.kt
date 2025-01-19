package com.bpitindia.attendance.data.network

import com.bpitindia.attendance.data.models.FacultyProfile
import com.bpitindia.attendance.data.models.FacultySubjectsBody
import com.bpitindia.attendance.data.models.FacultySubjectsResponse
import com.bpitindia.attendance.data.models.LoginResponse
import com.bpitindia.attendance.data.models.Statistics
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants.AUTHORIZATION_HEADER
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface ApiService {

    @GET("health/")
    suspend fun health(): Response<Any>

    @POST("api/auth/user/login/")
    fun login(@Body body: Map<String, String>): Call<LoginResponse>

    @GET("api/settings/")
    fun getSettings(@Header(AUTHORIZATION_HEADER) authorization: String): Call<Any>

    @GET("api/version/check_updates?platform=android")
    fun checkForUpdates(): Call<Any>

    @GET("api/faculty/profile/{id}")
    fun getFacultyProfile(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Path("id") id: Int
    ): Call<FacultyProfile>

    @GET("api/faculty/subjects/")
    fun getFacultySubjects(@Header(AUTHORIZATION_HEADER) authorization: String): Call<FacultySubjectsResponse>

    @POST("api/faculty/subjects/")
    fun addFacultySubjects(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Body body: FacultySubjectsBody
    ): Call<Any>

    @GET("api/subjects/")
    fun getAllSubjects(@Header(AUTHORIZATION_HEADER) authorization: String): Call<Any>

    @GET("api/student/attendance/list")
    fun getStudentList(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @QueryMap params: Map<String, String?>
    ): Call<StudentsResponse>

    @POST("api/student/attendance/submit/")
    fun submitAttendance(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Body body: StudentRequestBody
    ): Call<Any>

    @PATCH("api/student/attendance/submit/")
    fun editAttendance(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Body body: StudentRequestBody
    ): Call<Any>

    @GET("api/student/attendance/stats")
    fun getAttendanceStats(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @QueryMap params: Map<String, String?>
    ): Call<Statistics>

    @GET("api/student/attendance/list/last")
    fun getLastSubmittedAttendance(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @QueryMap params: Map<String, String?>
    ): Call<StudentsResponse>
}