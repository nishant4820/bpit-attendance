package com.bpitindia.attendance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bpitindia.attendance.data.models.Converters
import com.bpitindia.attendance.data.models.FacultySubject
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.data.models.StudentRecords

@Database(entities = [Student::class, FacultySubject::class, StudentRecords::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun attendanceDataDao():AttendanceDataDao
    abstract fun facultySubjectDao():FacultySubjectDao

    abstract fun studentRecordsDao(): StudentRecordsDao
}