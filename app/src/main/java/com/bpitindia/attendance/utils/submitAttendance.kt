package com.bpitindia.attendance.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.bpitindia.attendance.R
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.LocalAttendanceRecords
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import retrofit2.Callback
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


/*
This function is used to submit new attendance,
it takes studentRequestBody, sharedPreferences to give token and coroutineScope as parameter
and return whether attendance was uploaded or not
 */
fun submitAttendance(
    studentRequestBody: StudentRequestBody,
    sharedPrefProfile: SharedPreferences?,
    lifecycleScope:LifecycleCoroutineScope,
    repository: Repository,
    context: Context?,
    activity: FragmentActivity?
):Boolean{
    var submitted = false
    val token = sharedPrefProfile?.getString(TOKEN_KEY,null)?:return false

    lifecycleScope.launch{
        try {
            repository.remote.submitAttendance(token, studentRequestBody)
                .enqueue(object : Callback<Any> {
                    override fun onResponse(
                        call: Call<Any>,
                        response: Response<Any>
                    ) {
                        if (response.isSuccessful) {
                            submitted = true
                        }
                        activity?.runOnUiThread {
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Attendance Submitted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                when (response.code()) {
                                    401 -> {
                                        submitted = false
                                        Toast.makeText(
                                            context,
                                            getString(
                                                context!!,
                                                R.string.session_expired_message
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    else -> {
                                        submitted = false
                                        Toast.makeText(
                                            context,
                                            getString(context!!, R.string.server_error_message),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                            }
                        }
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        activity?.runOnUiThread {
                            val msg = if (t.message.toString()
                                    .startsWith(getString(context!!, R.string.error_prefix))
                            ) getString(context, R.string.internet_message) else getString(
                                context,
                                R.string.server_error_message
                            )
                            Toast.makeText(
                                context,
                                msg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.d(LOG_TAG, "submit attendance failed")
                    }
                })
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "some error occurred",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(LOG_TAG, "submitAttendance failed: ${e.message}")
        }
    }
    Log.d(LOG_TAG, "submitAttendance: $submitted")
    return submitted
}


