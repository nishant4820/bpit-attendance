@file:Suppress("DEPRECATION")

package com.bpitindia.attendance

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
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

private const val BATCH = "batch"
private const val SECTION = "section"
private const val BRANCH = "branch"
private const val AUTHORIZATION = "Authorization"
private const val IS_LAB = "is_lab"
private const val GROUP = "group"
private const val SUBJECT = "subject"

class EditAttendanceFragment : Fragment() {
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataTextView: TextView

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
        return inflater.inflate(R.layout.fragment_edit_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.edit_attendance_progress_bar)
        recyclerView = view.findViewById(R.id.edit_attendance_recycler_view)
        noDataTextView = view.findViewById(R.id.no_data_edit)
        fetchData(view)
    }

    private fun fetchData(view: View) {
        progressBar.visibility = ProgressBar.VISIBLE
        noDataTextView.visibility = TextView.GONE
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        val httpUrlBuilder = HttpUrl.Builder()
            .scheme(getString(R.string.url_scheme))
            .host(getString(R.string.url_host))
            .port(getString(R.string.url_port).toInt())
            .addPathSegment("api")
            .addPathSegment("student")
            .addPathSegment("attendance")
            .addPathSegment("list")
            .addPathSegment("last")
            .addQueryParameter("batch", batch.toString())
            .addQueryParameter("branch", branch)
            .addQueryParameter("subject", subject)
            .addQueryParameter("section", section)
            .addQueryParameter("group", group)
        val httpUrl = httpUrlBuilder.build()
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(httpUrl)
                .addHeader(AUTHORIZATION, token)
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "fetch last attendance failed")
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.GONE
                        findNavController().popBackStack()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val jsonArray = JSONArray(response.body?.string())
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.GONE
                                recyclerView.apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = EditAttendanceAdapter(jsonArray)
                                    setHasFixedSize(true)
                                }
                                addMenu(view)
                            }
                        } catch (e: Exception) {
                            activity?.runOnUiThread {
                                noDataTextView.visibility = TextView.VISIBLE
                                progressBar.visibility = ProgressBar.GONE
                            }
                        }
                        Log.d("debug", "edit attendance data fetch successful")
                    } else {
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().popBackStack()
                        }
                        Log.d(
                            "debug",
                            "edit attendance data fetch unsuccessful code: ${response.code}"
                        )
                    }
                    response.close()
                }

            })
        }
    }

    private fun markAttendance(view: View) {
        val progressDialog = android.app.ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
        progressDialog.setTitle("Submitting Attendance")
        progressDialog.setMessage("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.show()
        val dataSet = (recyclerView.adapter as EditAttendanceAdapter).dataSet
        val obj = JSONObject()
        obj.put("record", dataSet)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = obj.toString().toRequestBody(mediaType)
        val url = getString(R.string.url_complete) + getString(R.string.submit_attendance_api_path)
        val client = OkHttpClient()
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(url)
                .addHeader(AUTHORIZATION, token)
                .patch(body)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressDialog.dismiss()
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "upload edited attendance failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    activity?.runOnUiThread {
                        progressDialog.dismiss()
                        if (response.isSuccessful) {
                            Snackbar.make(view, "Attendance Updated", Snackbar.LENGTH_SHORT).show()
                            Log.d("debug", "Last attendance updated")
                            findNavController().popBackStack()
                        } else {
                            Log.d(
                                "debug",
                                "Last attendance update unsuccessful code: ${response.code}"
                            )
                            when (response.code) {
                                401 -> {
                                    findNavController().popBackStack()
                                }
                                else -> {
                                    Snackbar.make(
                                        view,
                                        getString(R.string.server_error_message),
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }

                    response.close()
                }

            })
        }

    }

    private fun addMenu(view: View) {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_edit_attendance_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.upload_edit_attendance) {
                    AlertDialog.Builder(context).apply {
                        setTitle("Submit")
                        setMessage("Are you sure you want to update attendance?")
                        setPositiveButton("Confirm") { _, _ ->
                            markAttendance(view)
                        }
                        setNegativeButton("Cancel") { _, _ -> }
                    }.create().show()
                    return true
                }
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}