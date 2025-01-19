package com.bpitindia.attendance

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.databinding.FragmentStudentListBinding
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.bpitindia.attendance.utils.canUpdateOnServer
import com.bpitindia.attendance.utils.saveAttendanceLocally
import com.bpitindia.attendance.utils.submitAttendance
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LocalStudentListFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private lateinit var binding: FragmentStudentListBinding
    private var batch: String? = null
    private var subject: String? = null
    private var date: String?=null
    private var isMarkedAll: Boolean = false
    private var sharedPrefProfile: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            batch = it.getString("BATCH")
            subject = it.getString("NAME")
            date = it.getString("DATE")
        }
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
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
        fetchStudents(view)
    }


    private fun fetchStudents(view: View) {
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        binding.studentProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            Log.d(LOG_TAG, "fetching students")
            repository.local.attendanceDataDao().getLocalData(date!!, subject!!)
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    Log.d(LOG_TAG, "fetch student failed")
                    e.printStackTrace()
                    activity?.runOnUiThread {
                        binding.studentProgressBar.visibility = View.GONE
                        findNavController().popBackStack()
                    }
                }
                .collect {students->
                    activity?.runOnUiThread {
                        binding.studentProgressBar.visibility = View.GONE
                        if (students.size == 0) {
                            binding.noStudent.visibility = TextView.VISIBLE
                        }
                        binding.studentList.apply {
                            layoutManager = LinearLayoutManager(activity)
                            val response = StudentsResponse()
                            students.forEach { student ->
                                response.add(student)
                            }
                            adapter = students.let { StudentAdapter(response) }
                        }
                        addMenu(view)
                    }

                }

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
                when (menuItem.itemId) {
                    R.id.upload_attendance -> {
                        handleUploadAttendance(view)
                        return true
                    }
                    R.id.mark_all -> {
                        toggleMarkAll()
                        return true
                    }
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleUploadAttendance(view: View) {
        val present = attendanceMap.values.count { it }
        val absent = attendanceMap.size - present
        AlertDialog.Builder(context).apply {
            setTitle("Submit?")
            setMessage(getString(R.string.confirm_dialog, attendanceMap.size, present, absent))
            setPositiveButton("Confirm") { _, _ -> markAttendance(view) }
            setNegativeButton("Cancel", null)
        }.create().show()
    }

    private fun toggleMarkAll() {
        isMarkedAll = !isMarkedAll
        attendanceMap.forEach { (key, _) -> attendanceMap[key] = isMarkedAll }
        val message = if (isMarkedAll) "Marked All" else "Unmarked All"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        (binding.studentList.adapter as StudentAdapter).notifyDataSetChanged()
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
        val recordsArray = StudentsResponse()
        attendanceMap.forEach { (key, value) ->
            val attendanceRecord = Student(enrollmentNumber = "",date="")
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

        lifecycleScope.launch {
            if (canUpdateOnServer(context,repository)){
                if(!submitAttendance(body,sharedPrefProfile,lifecycleScope,repository, context, activity)){
                    if(saveAttendanceLocally(recordsArray, repository, lifecycleScope)){
                        showSnackbar("Attendance saved locally, sync with server later to update attendance")
                    }else{
                        showSnackbar("attendance not saved locally")
                    }

                }

            }
            else{
                showToast("Uploaded attendance successfully")
            }
            progressDialog.dismiss()

        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
}
