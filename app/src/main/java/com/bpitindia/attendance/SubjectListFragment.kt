package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.FacultySubject
import com.bpitindia.attendance.data.models.FacultySubjectsResponse
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class SubjectListFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var noSubjectTextView: TextView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as? MainActivity)?.fetchProfile()
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().navigate(R.id.action_subjectListFragment_to_loginFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as MainActivity).setDrawerUnlocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subject_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        floatingActionButton = view.findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.action_subjectListFragment_to_addSubjectFragment)
        }
        noSubjectTextView = view.findViewById(R.id.no_subject)
        shimmerFrameLayout = view.findViewById(R.id.shimmer_view_container)
        fetchSettings()
    }

    override fun onStart() {
        super.onStart()
        view?.let { fetchSubjects(it) }
    }

    private fun fetchSubjects(view: View) {
        shimmerFrameLayout.visibility = ShimmerFrameLayout.VISIBLE
        shimmerFrameLayout.startShimmer()
        noSubjectTextView.visibility = TextView.INVISIBLE
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.remote.getFacultySubjects(token!!)
                    .enqueue(object : Callback<FacultySubjectsResponse> {
                        override fun onResponse(
                            call: Call<FacultySubjectsResponse>,
                            response: Response<FacultySubjectsResponse>
                        ) {
                            if (response.isSuccessful) {

                                val body = response.body()
                                Log.d(LOG_TAG, "subject fetch successful")
                                activity?.runOnUiThread {
                                    if (body?.size == 0) {
                                        noSubjectTextView.visibility = TextView.VISIBLE
                                    }
                                    view.findViewById<RecyclerView>(R.id.subjectList).apply {
                                        layoutManager = LinearLayoutManager(activity)
                                        adapter = SubjectAdapter(body!!)
                                    }
                                    shimmerFrameLayout.stopShimmer()
                                    shimmerFrameLayout.visibility = ShimmerFrameLayout.GONE
                                }
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val subjects = body!!.map {facultySubject ->
                                        facultySubject
                                    }
                                    Log.d(LOG_TAG, "onResponse: $subjects")
                                    val newIds:List<Int> = subjects.map { it.id }
                                    repository.local.facultySubjectDao().deleteRecordsNotIn(newIds)
                                    repository.local.facultySubjectDao().insertFacultySubjects(subjects)
                                }
                            } else {
                                Log.d(
                                    LOG_TAG,
                                    "subject fetch unsuccessful code: ${response.code()}"
                                )
                                activity?.runOnUiThread {
                                    shimmerFrameLayout.stopShimmer()
                                    shimmerFrameLayout.visibility = ShimmerFrameLayout.GONE
                                    when (response.code()) {
                                        401 -> {
                                            activity?.deleteSharedPreferences(
                                                SHARED_PREFERENCES_PROFILE
                                            )
                                            Toast.makeText(
                                                context,
                                                getString(R.string.session_expired_message),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            findNavController().navigate(R.id.action_subjectListFragment_to_loginFragment)
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
                        }
                        override fun onFailure(
                            call: Call<FacultySubjectsResponse>,
                            t: Throwable
                        ) {
                            Log.d(LOG_TAG, "fetch student failed")
                            t.printStackTrace()
                            activity?.runOnUiThread {
                                val msg = getString(R.string.working_offline)
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            }
                            lifecycleScope.launch(Dispatchers.IO) {
                                val body = repository.local.facultySubjectDao().fetchFacultySubjects()
                                val facultySubjectsResponse:FacultySubjectsResponse = FacultySubjectsResponse().apply {
                                    addAll(body.map {facultySubject ->
                                        FacultySubject(
                                            id = facultySubject.id,
                                            batch = facultySubject.batch,
                                            branchCode = facultySubject.subjectCode,
                                            branchName = facultySubject.branchName,
                                            branchSlug = facultySubject.branchSlug,
                                            section = facultySubject.section,
                                            classBatch = facultySubject.classBatch,
                                            specialization = facultySubject.specialization,
                                            specializationName = facultySubject.specializationName,
                                            isLab = facultySubject.isLab,
                                            group = facultySubject.group,
                                            subjectCode = facultySubject.subjectCode,
                                            subjectName = facultySubject.subjectName,
                                            semester =facultySubject.semester
                                        )
                                    })
                                }
                                activity?.runOnUiThread {
                                    if (body?.size == 0) {
                                        noSubjectTextView.visibility = TextView.VISIBLE
                                    }
                                    view.findViewById<RecyclerView>(R.id.subjectList).apply {
                                        layoutManager = LinearLayoutManager(activity)
                                        adapter = SubjectAdapter(facultySubjectsResponse)
                                    }
                                    shimmerFrameLayout.stopShimmer()
                                    shimmerFrameLayout.visibility = ShimmerFrameLayout.GONE
                                }
                            }

                            Log.d(LOG_TAG, "subject fetch failed, working offline")
                        }
                    })
            }
        }
    }

    private fun fetchSettings() {
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            repository.remote.getSettings(token).enqueue(object : Callback<Any> {
                override fun onResponse(
                    call: Call<Any>,
                    response: Response<Any>
                ) {
                    if (response.isSuccessful) {
                        try {
                            val jsonObject = JSONObject(Gson().toJson(response.body()))
                            val subjectAssignment = jsonObject.getBoolean("SUBJECTS_ASSIGNMENT")
                            Log.d(LOG_TAG, "subject assignment: $subjectAssignment")
                            if (subjectAssignment) {
                                activity?.runOnUiThread {
                                    floatingActionButton.visibility = FloatingActionButton.VISIBLE
                                }
                            }
                            Log.d(LOG_TAG, "settings fetch successful")
                        } catch (e: JSONException) {
                            Log.d(LOG_TAG, "SUBJECTS_ASSIGNMENT value doesn't exist")
                        }
                    } else {
                        Log.d(LOG_TAG, "settings fetch unsuccessful code: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(LOG_TAG, "settings fetch failed")
                }
            })
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).setDrawerLocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}