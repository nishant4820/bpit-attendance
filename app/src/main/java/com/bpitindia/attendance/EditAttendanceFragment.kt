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
private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class EditAttendanceFragment : Fragment() {
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var token: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    var jsonArray: JSONArray = JSONArray()
    private var dataPresent: Boolean = true
    private var sharedPreferences: SharedPreferences? = null
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
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
        if (token == null) {
            findNavController().navigate(R.id.action_editAttendanceFragment_to_loginFragment)
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
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = EditAttendanceAdapter()
            setHasFixedSize(true)
        }
        fetchData(view)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_edit_attendance_fragment, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.upload_edit_attendance) {
                    if (dataPresent) {
                        AlertDialog.Builder(context).apply {
                            setTitle("Submit")
                            setMessage("Are you sure you want to update attendance?")
                            setPositiveButton("Confirm") { _, _ ->
                                markAttendance(view)
                            }
                            setNegativeButton("Cancel") { _, _ -> }
                        }.create().show()
                    } else {
                        AlertDialog.Builder(context).apply {
                            setTitle("Cannot Update")
                            setMessage("No attendance is taken yet")
                            setPositiveButton("Continue") { _, _ -> }
                        }.create().show()
                    }
                    return true
                }
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

    }

    private fun fetchData(view: View) {
        progressBar.visibility = ProgressBar.VISIBLE
        noDataTextView.visibility = TextView.GONE
        val httpUrlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host(getString(R.string.api_host))
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
        Log.d("debug", "edit attendance fetch ${token.toString()}")
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
                        progressBar.visibility = ProgressBar.GONE
                    }
                    Log.d("debug", "Some error occurred!!")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            jsonArray = JSONArray(response.body?.string())
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.GONE
                                (recyclerView.adapter as EditAttendanceAdapter).dataSet = jsonArray
                            }
                        } catch (e: Exception) {
                            dataPresent = false
                            activity?.runOnUiThread {
                                noDataTextView.visibility = TextView.VISIBLE
                                progressBar.visibility = ProgressBar.GONE
                            }
                        }
                    } else {
                        activity?.deleteSharedPreferences(SHARED_PREFERENCES_NAME)
                        activity?.runOnUiThread {
                            Snackbar.make(
                                view,
                                "Session Expired!! Log in again.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().navigate(R.id.action_editAttendanceFragment_to_loginFragment)
                        }
                    }
                    response.body?.close()
                }

            })
        }
    }

    private fun markAttendance(view: View) {
        val dataSet = (recyclerView.adapter as EditAttendanceAdapter).dataSet
        val obj = JSONObject()
        obj.put("record", dataSet)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = obj.toString().toRequestBody(mediaType)
        val url = getString(R.string.submit_attendance_api_url)
        val client = OkHttpClient()
        Log.d("debug", "edit attendance upload ${token.toString()}")

        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(url)
                .addHeader(AUTHORIZATION, token!!)
                .patch(body)
                .build()
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
                            Snackbar.make(view, "Attendance Updated", Snackbar.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        } else {
                            activity?.deleteSharedPreferences(SHARED_PREFERENCES_NAME)
                            Snackbar.make(
                                view,
                                "Session Expired!! Log in again.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().navigate(R.id.action_editAttendanceFragment_to_loginFragment)
                        }
                    }

                    response.body?.close()
                }

            })
        }

    }
}