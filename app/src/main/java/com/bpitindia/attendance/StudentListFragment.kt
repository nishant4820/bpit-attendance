package com.bpitindia.attendance

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val BATCH = "batch"
private const val SECTION = "section"
private const val BRANCH = "branch"
private const val AUTHORIZATION = "Authorization"
private const val IS_LAB = "is_lab"
private const val GROUP = "group"
private const val SUBJECT = "subject"
private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class StudentListFragment : Fragment() {
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var token: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    var jsonArray: JSONArray = JSONArray()
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            batch = it.getString(BATCH)
            section = it.getString(SECTION)
            branch = it.getString(BRANCH)
            isLab = it.getBoolean(IS_LAB)
            group = it.getString(GROUP)
            subject = it.getString(SUBJECT)

        }
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_student_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ProgressBar>(R.id.student_progress_bar).visibility = ProgressBar.VISIBLE
        attendanceMap.clear()
        fetchStudents(view)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_student_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                AlertDialog.Builder(context).apply {
                    setTitle("Submit?")
                    setMessage(
                        getString(
                            R.string.confirm_dialog,
                            jsonArray.length(),
                            present,
                            absent
                        )
                    )
                    setPositiveButton("Confirm") { _, _ ->
                        markAttendance()
                    }
                    setNegativeButton("Cancel") { _, _ -> }
                }.create().show()
                return true
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun fetchStudents(view: View) {
        val httpUrlBuilder: HttpUrl.Builder = HttpUrl.Builder()
            .scheme("https")
            .host(getString(R.string.api_host))
            .addPathSegment("api")
            .addPathSegment("student")
            .addPathSegment("attendance")
            .addPathSegment("list")
            .addQueryParameter("batch", batch.toString())
            .addQueryParameter("branch", branch)
            .addQueryParameter("subject", subject)
            .addQueryParameter("section", section)
        if (isLab == true) httpUrlBuilder.addQueryParameter("group", group)
        val httpUrl = httpUrlBuilder.build()
        val client = OkHttpClient()
        Log.d("debug", "student fragment token ${token.toString()}")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val request: Request = Request.Builder()
                    .url(httpUrl)
                    .addHeader(AUTHORIZATION, token!!)
                    .get()
                    .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                "API Failed!! Contact Developer",
                                Toast.LENGTH_SHORT
                            ).show()
                            view.findViewById<ProgressBar>(R.id.student_progress_bar).visibility =
                                ProgressBar.INVISIBLE
                        }
                        Log.d("debug", "fetch student api failed")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            jsonArray = JSONArray(response.body?.string())
                            for (i in 0 until jsonArray.length()) {
                                val student = jsonArray.getJSONObject(i)
                                attendanceMap[student.getString("enrollment_number")] = false
                            }
                            absent = jsonArray.length()
                            Log.d("debug", "student json size: ${jsonArray.length()}")
                            activity?.runOnUiThread {
                                setRecyclerView(view.findViewById(R.id.studentList))
                                view.findViewById<ProgressBar>(R.id.student_progress_bar).visibility =
                                    ProgressBar.INVISIBLE
                            }
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "Student List Fetching Failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                                view.findViewById<ProgressBar>(R.id.student_progress_bar).visibility =
                                    ProgressBar.INVISIBLE
                            }
                        }
                    }
                })
            }
        }
    }

    private fun setRecyclerView(view: RecyclerView) {
        view.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = StudentAdapter(jsonArray)
            present = 0
            absent = jsonArray.length()
        }
    }

    private fun markAttendance() {
        val jsonArray = JSONArray()
        attendanceMap.forEach { (key, value) ->
            val attendanceJSONObject = JSONObject()
            attendanceJSONObject.put("enrollment_number", key)
            attendanceJSONObject.put("subject", subject)
            attendanceJSONObject.put("batch", batch)
            attendanceJSONObject.put("status", value)
            jsonArray.put(attendanceJSONObject)
        }
        val url = getString(R.string.submit_attendance_api_url)
        val client = OkHttpClient()

        lifecycleScope.launch {
            val obj = JSONObject()
            obj.put("record", jsonArray)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = obj.toString().toRequestBody(mediaType)
            Log.d("debug", "token student fragment= $token")
            val request: Request =
                Request.Builder().url(url).addHeader("Authorization", token!!).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            context,
                            "Upload API Failed!! Contact Developer",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.d("debug", "upload api failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("debug response", response.body!!.string())

                    activity?.runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Attendance Submitted", Toast.LENGTH_SHORT)
                                .show()
                            findNavController().popBackStack()
                        } else {
                            Toast.makeText(context, "Error Occurred!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    response.body?.close()
                }

            })
        }
    }
}