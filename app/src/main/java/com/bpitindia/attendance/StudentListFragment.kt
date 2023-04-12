package com.bpitindia.attendance

import android.annotation.SuppressLint
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
import android.widget.TextView
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

class StudentListFragment : Fragment() {
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var isMarkedAll: Boolean = false
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var noStudentTextView: TextView
    private var methodProvider: MyActivityMethodProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            methodProvider = context as MyActivityMethodProvider
        } catch (_: ClassCastException) {
        }
    }

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
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
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
        noStudentTextView = view.findViewById(R.id.no_student)
        attendanceMap.clear()
        fetchStudents(view)
    }

    private fun fetchStudents(view: View) {
        val httpUrlBuilder: HttpUrl.Builder = HttpUrl.Builder()
            .scheme(getString(R.string.url_scheme))
            .host(getString(R.string.url_host))
            .addPathSegment(getString(R.string.api_gateway))
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
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(httpUrl)
                .addHeader(AUTHORIZATION, token!!)
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "fetch student failed")
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        findNavController().popBackStack()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(response.body?.string())
                        response.close()
                        for (i in 0 until jsonArray.length()) {
                            val student = jsonArray.getJSONObject(i)
                            attendanceMap[student.getString("enrollment_number")] = false
                        }
                        Log.d("debug", "student fetch successful")
                        activity?.runOnUiThread {
                            if (jsonArray.length() == 0) {
                                noStudentTextView.visibility = TextView.VISIBLE
                            }
                            view.findViewById<RecyclerView>(R.id.studentList).apply {
                                layoutManager = LinearLayoutManager(activity)
                                adapter = StudentAdapter(jsonArray)
                            }
                            progressBar.visibility = ProgressBar.INVISIBLE
                            addMenu(view)
                        }
                    } else {
                        response.close()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().popBackStack()
                        }
                    }
                }
            })
        }

    }

    private fun addMenu(view: View) {
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
                                attendanceMap.size,
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

    @Suppress("DEPRECATION")
    private fun markAttendance(view: View) {
        val progressDialog = android.app.ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
        progressDialog.setTitle("Submitting Attendance")
        progressDialog.setMessage("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.show()
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
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val url = getString(R.string.url_complete) + getString(R.string.submit_attendance_api_path)
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()

        lifecycleScope.launch {
            val obj = JSONObject()
            obj.put("record", jsonArray)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = obj.toString().toRequestBody(mediaType)
            val request: Request =
                Request.Builder().url(url).addHeader("Authorization", token!!).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    progressDialog.dismiss()
                    activity?.runOnUiThread {
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "upload attendance failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    progressDialog.dismiss()
                    activity?.runOnUiThread {
                        if (response.isSuccessful) {
                            response.close()
                            Snackbar.make(view, "Attendance Submitted", Snackbar.LENGTH_SHORT)
                                .show()
                            findNavController().popBackStack()
                        } else {
                            when (response.code) {
                                401 -> {
                                    response.close()
                                    Toast.makeText(
                                        context,
                                        "Session Expired! Log in again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    findNavController().popBackStack()
                                }
                                else -> {
                                    response.close()
                                    Snackbar.make(
                                        view,
                                        getString(R.string.server_error_message),
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        }
                    }
                }

            })
        }
    }
}