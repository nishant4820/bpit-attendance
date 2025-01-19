package com.bpitindia.attendance.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.bpitindia.attendance.data.local.AppDatabase
import com.bpitindia.attendance.data.local.AttendanceDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context):AppDatabase{
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "attendance_data"
        ).build()
    }

    @Provides
    fun provideAttendanceDataDao(database: AppDatabase):AttendanceDataDao{
        return database.attendanceDataDao()
    }
}