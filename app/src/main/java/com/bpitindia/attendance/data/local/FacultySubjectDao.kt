package com.bpitindia.attendance.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.bpitindia.attendance.data.models.FacultySubject

@Dao
interface FacultySubjectDao {
    @Query("DELETE FROM facultySubjects WHERE id NOT IN (:ids)")
    suspend fun deleteRecordsNotIn(ids: List<Int>)
    @Upsert
    suspend fun insertFacultySubjects(facultySubjects: List<FacultySubject>)

    @Query("SELECT * FROM facultySubjects")
    suspend fun fetchFacultySubjects():List<FacultySubject>

    @Query("DELETE FROM facultySubjects")
    suspend fun deleteAllFacultySubjects()

}