package com.bpitindia.attendance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bpitindia.attendance.data.models.Converters
import com.bpitindia.attendance.data.models.Student

@Database(entities = [Student::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun attendanceDataDao():AttendanceDataDao
}