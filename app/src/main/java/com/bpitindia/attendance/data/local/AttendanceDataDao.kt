package com.bpitindia.attendance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bpitindia.attendance.data.models.LocalAttendanceRecords
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.data.models.StudentsResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDataDao {
    
    //this function stores data related to attendance of student, it keeps record of who were present or not
    @Insert
    suspend fun insertLocalDataStudents(studentsResponse: StudentsResponse)
    
    //this function is to fetch list of student for a particular subject and date
    @Query("SELECT * FROM students_attendance WHERE date = :date AND subject = :subject")
    fun getLocalDataStudents(date: String, subject: String): Flow<List<Student>>

    //this function is ti get list of subjects whose attendance is saved locally
    @Query("SELECT DISTINCT subject, date, batch FROM students_attendance")
    fun getUniqueRecordsLocalSubjects(): Flow<List<LocalAttendanceRecords>>

    //this function is to delete record of a particular subject and date 
    @Query("DELETE FROM students_attendance WHERE date = :date AND subject = :subject AND batch = :batch")
    suspend fun deleteLocalDataStudents(subject: String, date: String, batch:String)

}