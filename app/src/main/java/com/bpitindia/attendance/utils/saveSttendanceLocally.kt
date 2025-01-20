package com.bpitindia.attendance.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.lifecycle.LifecycleCoroutineScope
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun saveAttendanceLocally(
    dataset: StudentsResponse,
    repository: Repository,
    context: Context,
    coroutineScope: LifecycleCoroutineScope
) {
    Log.d(LOG_TAG, "saveAttendanceLocally: $dataset")
    coroutineScope.launch(Dispatchers.IO) {
        try {
            repository.local.attendanceDataDao().insertLocalData(dataset)
            withContext(Dispatchers.Main){
                Toast.makeText(context,"Data saved locally",LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main){
                Toast.makeText(context,"Something went wrong try again",LENGTH_SHORT).show()
            }
            Log.e("LOG_TAG", "not saved locally: ${e.message}")
        }
    }
}
