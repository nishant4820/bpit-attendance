@file:Suppress("DEPRECATION")

package com.bpitindia.attendance

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants
import com.bpitindia.attendance.utils.Constants.BATCH
import com.bpitindia.attendance.utils.Constants.BRANCH
import com.bpitindia.attendance.utils.Constants.GROUP
import com.bpitindia.attendance.utils.Constants.IS_LAB
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SECTION
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.SUBJECT
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.bpitindia.attendance.utils.canUpdateOnServer
import com.bpitindia.attendance.utils.saveAttendanceLocally
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class EditAttendanceFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var classBatch: String? = null
    private var specialization: Int? = null
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataTextView: TextView
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
            classBatch = it.getString(Constants.CLASS_BATCH)
            specialization = it.getInt(Constants.SPECIALIZATION)
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

        val params = mapOf(
            "batch" to batch.orEmpty(),
            "branch" to branch.orEmpty(),
            "subject" to subject.orEmpty(),
            "section" to section.toString(),
            "class_batch" to classBatch.toString(),
            "specialization" to specialization.toString(),
            "group" to group.orEmpty()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            repository.remote.getLastSubmittedAttendance(token, params)
                .enqueue(object : Callback<StudentsResponse> {
                    override fun onResponse(
                        call: Call<StudentsResponse>,
                        response: Response<StudentsResponse>
                    ) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            activity?.runOnUiThread {
                                if (body?.size == 0) {
                                    noDataTextView.visibility = TextView.VISIBLE
                                    progressBar.visibility = ProgressBar.GONE
                                }
                                progressBar.visibility = ProgressBar.GONE
                                recyclerView.apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = body?.let {
                                        EditAttendanceAdapter(it)
                                    }
                                    setHasFixedSize(true)
                                }
                                addMenu(view)
                            }
                            Log.d(LOG_TAG, "edit attendance data fetch successful")
                        } else {
                            Log.d(
                                LOG_TAG,
                                "edit attendance data fetch unsuccessful code: ${response.code()}"
                            )
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                findNavController().popBackStack()
                            }
                        }
                    }

                    override fun onFailure(call: Call<StudentsResponse>, t: Throwable) {
                        Log.d(LOG_TAG, "fetch last attendance failed")
                        t.printStackTrace()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.GONE
                            findNavController().popBackStack()
                        }
                    }

                })
        }
    }

    private fun markAttendance(view: View) {
        val progressDialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
        progressDialog.setTitle("Submitting Attendance")
        progressDialog.setMessage("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.show()

        val dataSet = (recyclerView.adapter as EditAttendanceAdapter).dataSet
        val body = StudentRequestBody()
        body.record = dataSet

        sharedPrefProfile = activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        lifecycleScope.launch {
            // Check if the device is connected to the internet and server is available
            if (canUpdateOnServer(context,repository)) {
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.remote.editAttendance(token, body).enqueue(object : Callback<Any> {
                        override fun onResponse(call: Call<Any>, response: Response<Any>) {
                            activity?.runOnUiThread {
                                progressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Snackbar.make(view, "Attendance Updated", Snackbar.LENGTH_SHORT).show()
                                    Log.d(LOG_TAG, "Last attendance updated")
                                    findNavController().popBackStack()
                                } else {
                                    Log.d(LOG_TAG, "Last attendance update unsuccessful code: ${response.code()}")
                                    when (response.code()) {
                                        401 -> {
                                            findNavController().popBackStack()
                                        }
                                        else -> {
                                            Snackbar.make(view, getString(R.string.server_error_message), Snackbar.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            activity?.runOnUiThread {
                                progressDialog.dismiss()
                                val msg = if (t.message.toString().startsWith(getString(R.string.error_prefix)))
                                    getString(R.string.internet_message) else getString(R.string.server_error_message)
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            }
                            Log.d(LOG_TAG, "upload edited attendance failed")
                        }
                    })
                }
            } else {
                // Store the attendance data locally when offline or server not available
                saveAttendanceLocally(dataSet, repository, requireContext(),lifecycleScope)
            }
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
                            lifecycleScope.launch {
                                markAttendance(view)
                            }
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