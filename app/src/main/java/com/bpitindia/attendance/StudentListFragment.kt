package com.bpitindia.attendance

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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
    private var isMarkedAll: Boolean = false
    var jsonArray: JSONArray = JSONArray()
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar

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
        if (token == null) {
            findNavController().navigate(R.id.action_studentListFragment_to_loginFragment)
        }
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
        progressBar = view.findViewById(R.id.student_progress_bar)
        progressBar.visibility = ProgressBar.VISIBLE
        attendanceMap.clear()
        fetchStudents(view)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_student_fragment, menu)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.upload_attendance) {
                    var present = 0
                    var absent = 0
                    attendanceMap.forEach { (_, value) ->
                        if (value) present++ else absent++
                    }
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
                            markAttendance(view)
                        }
                        setNegativeButton("Cancel") { _, _ -> }
                    }.create().show()
                    return true
                }
                if (menuItem.itemId == R.id.mark_all) {
                    if (isMarkedAll) {
                        attendanceMap.forEach { (key, _) ->
                            attendanceMap[key] = false
                        }
                        isMarkedAll = !isMarkedAll
                        Snackbar.make(view, "Unmarked All", Snackbar.LENGTH_SHORT).show()
                    } else {
                        attendanceMap.forEach { (key, _) ->
                            attendanceMap[key] = true
                        }
                        isMarkedAll = !isMarkedAll
                        Snackbar.make(view, "Marked All", Snackbar.LENGTH_SHORT).show()
                    }
                    (view.findViewById<RecyclerView>(R.id.studentList).adapter as StudentAdapter).notifyDataSetChanged()
                    return true
                }
                return false
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
        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(httpUrl)
                .addHeader(AUTHORIZATION, token!!)
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        Snackbar.make(view, "Some error occurred", Snackbar.LENGTH_SHORT).show()
                        progressBar.visibility = ProgressBar.INVISIBLE
                    }
                    Log.d("debug", "Some error occurred!!")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        jsonArray = JSONArray(response.body?.string())
                        for (i in 0 until jsonArray.length()) {
                            val student = jsonArray.getJSONObject(i)
                            attendanceMap[student.getString("enrollment_number")] = false
                        }
                        Log.d("debug", "student json size: ${jsonArray.length()}")
                        activity?.runOnUiThread {
                            setRecyclerView(view.findViewById(R.id.studentList))
                            progressBar.visibility = ProgressBar.INVISIBLE
                        }
                    } else {
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().navigate(R.id.action_studentListFragment_to_loginFragment)
                        }
                    }
                    response.body?.close()
                }
            })
        }

    }

    private fun setRecyclerView(view: RecyclerView) {
        view.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = StudentAdapter(jsonArray)
        }
    }

    private fun markAttendance(view: View) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDateAndTime = sdf.format(Date())
        val jsonArray = JSONArray()
        attendanceMap.forEach { (key, value) ->
            val attendanceJSONObject = JSONObject()
            attendanceJSONObject.put("enrollment_number", key)
            attendanceJSONObject.put("subject", subject)
            attendanceJSONObject.put("batch", batch)
            attendanceJSONObject.put("status", value)
            attendanceJSONObject.put("date", currentDateAndTime)
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
                        Snackbar.make(view, "Some error occurred", Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "upload api failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("debug response", response.body!!.string())

                    activity?.runOnUiThread {
                        if (response.isSuccessful) {
                            Snackbar.make(view, "Attendance Submitted", Snackbar.LENGTH_SHORT)
                                .show()
                            findNavController().popBackStack()
                        } else {
                            Snackbar.make(view, "Error Occurred!", Snackbar.LENGTH_SHORT).show()
                        }
                    }

                    response.body?.close()
                }

            })
        }
    }
}