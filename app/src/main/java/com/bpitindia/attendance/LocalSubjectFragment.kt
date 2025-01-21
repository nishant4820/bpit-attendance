package com.bpitindia.attendance

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.LocalAttendanceRecords
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.data.models.StudentRequestBody
import com.bpitindia.attendance.data.models.StudentsResponse
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.submitAttendance
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LocalSubjectsFragment : Fragment() {

    @Inject
    lateinit var repository: Repository

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataTextView: TextView
    private var methodProvider: MyActivityMethodProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MyActivityMethodProvider) {
            methodProvider = context
        } else {
            Log.e(LOG_TAG, "Context must implement MyActivityMethodProvider")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.local_subject_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        observeData()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.rvLocalSubjects)
        noDataTextView = view.findViewById(R.id.no_local_data_edit)
        progressBar = view.findViewById(R.id.stats_progress_bar)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun toggleLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun observeData() {
        toggleLoading(true)

        lifecycleScope.launch {
            repository.local.attendanceDataDao()
                .getUniqueRecords()
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    Log.e(LOG_TAG, "Error fetching data: $e")
                    withContext(Dispatchers.Main) {
                        toggleLoading(false)
                        showSnackbar("Error fetching local data")
                    }
                }
                .collect { data ->
                    updateUI(data)
                    toggleLoading(false)
                }
        }
    }

    private fun updateUI(data: List<LocalAttendanceRecords>) {
        if (data.isEmpty()) {
            noDataTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noDataTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = LocalSubjectsAdapter(
                data
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        private const val LOG_TAG = "LocalSubjectsFragment"
    }
}

