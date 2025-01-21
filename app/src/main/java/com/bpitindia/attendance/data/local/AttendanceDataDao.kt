package com.bpitindia.attendance.data.local

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.bpitindia.attendance.data.models.LocalAttendanceRecords
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDataDao {
    @Insert
    suspend fun insertLocalData(studentsResponse: StudentsResponse)

    @Delete
    suspend fun deleteLocalData(students: List<Student>)

    @Query("SELECT * FROM students WHERE date = :date AND subject = :subject")
    fun getLocalData(date: String, subject: String): Flow<List<Student>>

    @Query("SELECT DISTINCT subject, date, batch FROM students")
    fun getUniqueRecords(): Flow<List<LocalAttendanceRecords>>

    @Query("DELETE FROM students WHERE date = :date AND subject = :subject AND batch = :batch")
    suspend fun deleteRecord( subject: String,date: String,batch:String)

}