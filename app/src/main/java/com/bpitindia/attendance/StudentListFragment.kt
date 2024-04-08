package com.bpitindia.attendance

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.databinding.FragmentStudentListBinding
import com.bpitindia.attendance.utils.Constants.BATCH
import com.bpitindia.attendance.utils.Constants.BRANCH
import com.bpitindia.attendance.utils.Constants.CLASS_BATCH
import com.bpitindia.attendance.utils.Constants.GROUP
import com.bpitindia.attendance.utils.Constants.IS_LAB
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SECTION
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.SPECIALIZATION
import com.bpitindia.attendance.utils.Constants.SUBJECT
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StudentListFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private lateinit var binding: FragmentStudentListBinding
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var classBatch: String? = null
    private var specialization: Int? = null
    private var isMarkedAll: Boolean = false
    private var sharedPrefProfile: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            batch = it.getString(BATCH)
            section = it.getString(SECTION)
            branch = it.getString(BRANCH)
            isLab = it.getBoolean(IS_LAB)
            group = it.getString(GROUP)
            subject = it.getString(SUBJECT)
            classBatch = it.getString(CLASS_BATCH)
            specialization = it.getInt(SPECIALIZATION)
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
    ): View {
        binding = FragmentStudentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attendanceMap.clear()
        fetchStudents()
    }

    private fun fetchStudents() {
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        binding.studentProgressBar.visibility = View.VISIBLE
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
            Log.d(LOG_TAG, "fetching students")
            repository.remote.getStudentList(token, params)
                .enqueue(object : Callback<StudentsResponse> {
                    override fun onResponse(
                        call: Call<StudentsResponse>,
                        response: Response<StudentsResponse>
                    ) {
                        if (response.isSuccessful) {
                            Log.d(LOG_TAG, "students fetch successful")
                            val body = response.body()
                            for (i in 0 until (body?.size ?: 0)) {
                                val student = body?.get(i)
                                attendanceMap[student?.enrollmentNumber!!] = false
                            }
                            activity?.runOnUiThread {
                                binding.studentProgressBar.visibility = View.GONE
                                if (body?.size == 0) {
                                    binding.noStudent.visibility = TextView.VISIBLE
                                }
                                binding.studentList.apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = body?.let { StudentAdapter(it) }
                                }
                                addMenu()
                            }
                        } else {
                            Log.d(LOG_TAG, "students fetch unsuccessful code: ${response.code()}")
                            activity?.runOnUiThread {
                                binding.studentProgressBar.visibility = View.GONE
                                findNavController().popBackStack()
                            }
                        }
                    }

                    override fun onFailure(call: Call<StudentsResponse>, t: Throwable) {
                        Log.d(LOG_TAG, "fetch student failed")
                        t.printStackTrace()
                        activity?.runOnUiThread {
                            binding.studentProgressBar.visibility = View.GONE
                            findNavController().popBackStack()
                        }
                    }

                })
        }

    }

    private fun addMenu() {
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
                            markAttendance()
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
                        Snackbar.make(binding.root, "Unmarked All", Snackbar.LENGTH_SHORT).show()
                    } else {
                        attendanceMap.forEach { (key, _) ->
                            attendanceMap[key] = true
                        }
                        isMarkedAll = !isMarkedAll
                        Snackbar.make(binding.root, "Marked All", Snackbar.LENGTH_SHORT).show()
                    }
                    (binding.studentList.adapter as StudentAdapter).notifyDataSetChanged()
                    return true
                }
                return false
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    @Suppress("DEPRECATION")
    private fun markAttendance() {
        val progressDialog = android.app.ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
        progressDialog.setTitle("Submitting Attendance")
        progressDialog.setMessage("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.show()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDateAndTime = sdf.format(Date())
        val recordsArray = StudentsResponse()
        attendanceMap.forEach { (key, value) ->
            val attendanceRecord = Student()
            attendanceRecord.enrollmentNumber = key
            attendanceRecord.subject = subject
            attendanceRecord.batch = batch
            attendanceRecord.status = value
            attendanceRecord.date = currentDateAndTime
            recordsArray.add(attendanceRecord)
        }
        val body = StudentRequestBody()
        body.record = recordsArray

        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)

        lifecycleScope.launch {
            repository.remote.submitAttendance(token!!, body)
                .enqueue(object : Callback<Any> {
                    override fun onResponse(
                        call: Call<Any>,
                        response: Response<Any>
                    ) {
                        progressDialog.dismiss()
                        activity?.runOnUiThread {
                            if (response.isSuccessful) {
                                Snackbar.make(
                                    binding.root,
                                    "Attendance Submitted",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .show()
                                findNavController().popBackStack()
                            } else {
                                when (response.code()) {
                                    401 -> {
                                        Toast.makeText(
                                            context,
                                            getString(R.string.session_expired_message),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        findNavController().popBackStack()
                                    }

                                    else -> {
                                        Snackbar.make(
                                            binding.root,
                                            getString(R.string.server_error_message),
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                            }
                        }
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        progressDialog.dismiss()
                        activity?.runOnUiThread {
                            val msg = if (t.message.toString()
                                    .startsWith(getString(R.string.error_prefix))
                            ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        }
                        Log.d(LOG_TAG, "upload attendance failed")
                    }
                })
        }
    }
}