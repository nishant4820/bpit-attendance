package com.bpitindia.attendance.utils

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.StudentsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

suspend fun saveAttendanceLocally(
    dataset: StudentsResponse,
    repository: Repository,
    coroutineScope: LifecycleCoroutineScope
):Boolean {
    var saved = false
    coroutineScope.launch(Dispatchers.IO) {
        try {
            repository.local.attendanceDataDao().insertLocalData(dataset)
            saved=true

        } catch (e: Exception) {
            Log.e("LOG_TAG", "not saved locally: ${e.message}")
            saved=false

        }
    }
    return saved
}
