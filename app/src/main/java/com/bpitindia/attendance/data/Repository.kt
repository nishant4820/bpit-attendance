package com.bpitindia.attendance.data

import androidx.room.RoomDatabase
import com.bpitindia.attendance.data.local.AppDatabase
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class Repository @Inject constructor(
    remoteDataSource: RemoteDataSource,
    localDataSource: AppDatabase
) {
    val remote = remoteDataSource
    val local = localDataSource
}