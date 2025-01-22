package com.bpitindia.attendance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.bpitindia.attendance.data.models.StudentRecords

@Dao
interface StudentRecordsDao {
    // Insert a list of StudentRecords entries
    @Upsert
    suspend fun insertStudentRecords(studentRecords: List<StudentRecords>)

    // Delete all records in the students_records table
    @Query("DELETE FROM students_records WHERE facultySubjectId = :id")
    suspend fun deleteAllStudentRecords(id:Int)

    // Fetch all StudentRecords by facultySubjectId
    @Query("SELECT * FROM students_records WHERE facultySubjectId = :facultySubjectId")
    suspend fun getStudentRecordsByFacultySubjectId(facultySubjectId: Int): List<StudentRecords>
}
