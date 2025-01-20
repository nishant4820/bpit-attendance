package com.bpitindia.attendance.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat.getString
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import com.bpitindia.attendance.R
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.LocalAttendanceRecords
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import retrofit2.Callback
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.util.concurrent.CountDownLatch


/*
This function is used to submit new attendance,
it takes studentRequestBody, sharedPreferences to give token and coroutineScope as parameter
and return whether attendance was uploaded or not
 */
suspend fun submitAttendance(
    studentRequestBody: StudentRequestBody,
    sharedPrefProfile: SharedPreferences?,
    lifecycleScope:LifecycleCoroutineScope,
    repository: Repository,
    context: Context?,
    activity: FragmentActivity?
){
    Log.d("TAG", "submitAttendance: $studentRequestBody")
    val token = sharedPrefProfile?.getString(TOKEN_KEY,null)?:return
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            if(canUpdateOnServer(context!!,repository)){
                repository.remote.submitAttendance(token!!, studentRequestBody)
                    .enqueue(object : Callback<Any> {
                        override fun onResponse(
                            call: Call<Any>,
                            response: Response<Any>
                        ) {
                            if (response.isSuccessful) {
                                activity?.runOnUiThread{
                                    Toast.makeText(context,"Attendance submitted",LENGTH_SHORT).show()
                                }
                            } else {
                                lifecycleScope.launch {
                                    saveAttendanceLocally(studentRequestBody.record!!,repository,context!!,lifecycleScope)
                                }
                                activity?.runOnUiThread{
                                    when (response.code()) {
                                        401 -> {
                                            Toast.makeText(context, getString(context!!,R.string.session_expired_message), LENGTH_SHORT).show()
                                        }
                                        else -> {
                                            Toast.makeText(context, getString(context!!,R.string.server_error_message), LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            lifecycleScope.launch {
                                saveAttendanceLocally(studentRequestBody.record!!,repository,context!!,lifecycleScope)
                            }
                            val msg = if (t.message.toString()
                                    .startsWith(getString(context!!,R.string.error_prefix))
                            ) {
                                getString(context!!, R.string.internet_message)
                            } else {
                                getString(context!!, R.string.server_error_message)
                            }
                            activity?.runOnUiThread{
                                Toast.makeText(context,msg, LENGTH_SHORT).show()
                            }
                            Log.d(LOG_TAG, "upload attendance failed")
                        }
                    })
            }else{
                saveAttendanceLocally(studentRequestBody.record!!,repository,context!!,lifecycleScope)
            }
        }catch(e:Exception){
            Log.d(LOG_TAG, "submitAttendance: catch $e")
            activity?.runOnUiThread{
                Toast.makeText(context, getString(context!!,R.string.server_error_message), LENGTH_SHORT).show()

            }
        }
    }
}


