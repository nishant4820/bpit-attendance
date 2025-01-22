@file:Suppress("DEPRECATION")

package com.bpitindia.attendance.addsubject

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bpitindia.attendance.MyActivityMethodProvider
import com.bpitindia.attendance.R
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.BranchItem
import com.bpitindia.attendance.data.models.FacultySubject
import com.bpitindia.attendance.data.models.FacultySubjectsBody
import com.bpitindia.attendance.data.models.FacultySubjectsResponse
import com.bpitindia.attendance.data.models.SpecializationItem
import com.bpitindia.attendance.data.models.SubjectItem
import com.bpitindia.attendance.databinding.FragmentAddSubjectBinding
import com.bpitindia.attendance.utils.Constants.AUTHORIZATION_HEADER
import com.bpitindia.attendance.utils.Constants.BASE_URL
import com.bpitindia.attendance.utils.Constants.ID_KEY
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AddSubjectFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private lateinit var binding: FragmentAddSubjectBinding
    private var sharedPrefProfile: SharedPreferences? = null
    private var subjectArray: JSONArray = JSONArray()
    private var branchArray: JSONArray = JSONArray()
    private var specializationArray: JSONArray = JSONArray()
    private var selectedSubject: String? = null
    private var selectedBranch: String? = null
    private var selectedSpecialization: Int = 0
    private var subjectList: List<SubjectItem> = listOf()
    private var branchList: List<BranchItem> = listOf()
    private var specializationList: List<SpecializationItem> = listOf()
    private var subjectStringList: List<String> = listOf()
    private var methodProvider: MyActivityMethodProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            methodProvider = context as MyActivityMethodProvider
        } catch (_: ClassCastException) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        lifecycleScope.launch {
            val progressDialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
            progressDialog.setTitle("Fetching Subjects")
            progressDialog.setMessage("Please Wait...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.setCancelable(false)
            progressDialog.show()
            val fetchSuccessful = fetchSubjectBranchesSpecialization()
            progressDialog.dismiss()
            if (!fetchSuccessful) {
                Toast.makeText(context, "Unable to fetch Subjects", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }

            binding.subjectTextView.apply {
                val subjectAdapter =
                    SubjectAutoCompleteAdapter(
                        requireContext(),
                        subjectList
                    )
                setAdapter(subjectAdapter)
                validator = object : AutoCompleteTextView.Validator {
                    override fun isValid(text: CharSequence?): Boolean {
                        return subjectStringList.contains(text.toString())
                    }

                    override fun fixText(invalidText: CharSequence?): CharSequence {
                        Snackbar.make(
                            requireContext(),
                            view,
                            "Select Subject from List only",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        return ""
                    }
                }
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                    }
                }
                setOnItemClickListener { parent, _, position, _ ->
                    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                    binding.subjectBox.error = null
                    binding.subjectBox.helperText = null
                    val selectedItem = parent.adapter.getItem(position) as SubjectItem
                    selectedSubject = selectedItem.subjectCode
                }
                setOnEditorActionListener { _, actionId, event ->
                    if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                        true
                    } else false
                }
            }

            binding.apply {

                val sectionAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    resources.getStringArray(R.array.section_array)
                )
                sectionTextView.setAdapter(sectionAdapter)
                sectionTextView.setOnItemClickListener { _, _, _, _ -> sectionBox.error = null }

                val branchAdapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_dropdown_item_1line, branchList
                )
                branchTextView.setAdapter(branchAdapter)
                branchTextView.setOnItemClickListener { parent, _, position, _ ->
                    branchBox.error = null
                    val selectedItem = parent.adapter.getItem(position) as BranchItem
                    selectedBranch = selectedItem.branchCode
                }

                val currYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()).toInt()
                val batchAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    makeBatchArray(currYear)
                )
                batchTextView.setAdapter(batchAdapter)
                batchTextView.setOnItemClickListener { _, _, _, _ -> batchBox.error = null }

                val semesterAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    resources.getStringArray(R.array.semester_array)
                )
                semesterTextView.setAdapter(semesterAdapter)
                semesterTextView.setOnItemClickListener { parent, _, position, id ->
                    semesterBox.error = null
                    val selectedSemester = (parent.adapter.getItem(position) as String).toInt()
                    if (selectedSemester in 1..5) {
                        sectionBox.visibility = View.VISIBLE
                        classBatchBox.visibility = View.GONE
                        classBatchTextView.setText("")
                    } else {
                        classBatchBox.visibility = View.VISIBLE
                        sectionBox.visibility = View.GONE
                        sectionTextView.setText("")
                    }
                }

                val classBatchAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    resources.getStringArray(R.array.class_batch_array)
                )
                classBatchTextView.setAdapter(classBatchAdapter)
                classBatchTextView.setOnItemClickListener { _, _, _, _ ->
                    classBatchBox.error = null
                }

                theoryLabRadioGroup.setOnCheckedChangeListener { _, checkedId ->

                    when (checkedId) {
                        R.id.theory_button -> {
                            groupBox.visibility = View.GONE
                            groupTextView.setText("")
                        }

                        R.id.lab_button -> {
                            groupBox.visibility = View.VISIBLE
                        }
                    }

                }

                val groupAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    resources.getStringArray(R.array.group_array)
                )
                groupTextView.setAdapter(groupAdapter)
                groupTextView.setOnItemClickListener { _, _, _, _ -> groupBox.error = null }

                val specializationAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    specializationList
                )
                specializationTextView.setAdapter(specializationAdapter)
                specializationTextView.setOnItemClickListener { parent, _, position, _ ->
                    specializationBox.error = null
                    val selectedItem = parent.adapter.getItem(position) as SpecializationItem
                    selectedSpecialization = selectedItem.id
                }

                addSubjectButton.setOnClickListener {
                    addSubject(view)
                }
            }
        }
    }

    private suspend fun fetchSubjectBranchesSpecialization(): Boolean {
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return false
        }
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        val urlBranch = BASE_URL + getString(R.string.get_branches_api_path)
        val urlSubject = BASE_URL + getString(R.string.get_all_subjects_api_path)
        val urlSpecialization = BASE_URL + getString(R.string.get_all_specialization_api_path)
        val requestBranch =
            Request.Builder().url(urlBranch).addHeader(AUTHORIZATION_HEADER, token).get().build()
        val requestSubjects =
            Request.Builder().url(urlSubject).addHeader(AUTHORIZATION_HEADER, token).get().build()
        val requestSpecialization =
            Request.Builder().url(urlSpecialization).addHeader(AUTHORIZATION_HEADER, token).get()
                .build()
        return withContext(Dispatchers.IO) {
            try {
                val responseBranch = client.newCall(requestBranch).execute()
                val responseSubject = client.newCall(requestSubjects).execute()
                val responseSpecialization = client.newCall(requestSpecialization).execute()
                if (responseBranch.isSuccessful && responseSubject.isSuccessful && responseSpecialization.isSuccessful) {
                    branchArray = JSONArray(responseBranch.body?.string())
                    branchList = createBranchItemList(branchArray)
                    subjectArray = JSONArray(responseSubject.body?.string())
                    subjectList = createSubjectItemList(subjectArray)
                    specializationArray = JSONArray(responseSpecialization.body?.string())
                    specializationList = createSpecializationItemList(specializationArray)
                    responseBranch.close()
                    responseSubject.close()
                    responseSpecialization.close()
                    true
                } else {
                    responseBranch.close()
                    responseSubject.close()
                    responseSpecialization.close()
                    false
                }
            } catch (e: IOException) {
                activity?.runOnUiThread {
                    val msg = if (e.message.toString()
                            .startsWith(getString(R.string.error_prefix))
                    ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                Log.d(
                    LOG_TAG,
                    "fetch branch or subject or specialization failed in add subject fragment"
                )
                false
            }
        }
    }

    private fun addSubject(view: View) {
        binding.apply {
            val subjectCode = subjectTextView.text.toString()
            val branchCode = branchTextView.text.toString()
            val batch = batchTextView.text.toString()
            val semester = semesterTextView.text.toString()
            var section: String? = sectionTextView.text.toString()
            var classBatch: String? = classBatchTextView.text.toString()
            val isLab = labButton.isChecked
            val specialization = specializationTextView.text.toString()
            val group = if (isLab) groupTextView.text.toString() else "null"

            if (subjectCode.isEmpty() || selectedSubject.isNullOrEmpty()) {
                subjectBox.error = getString(R.string.required)
                return
            }
            if (branchCode.isEmpty() || selectedBranch.isNullOrEmpty()) {
                branchBox.error = getString(R.string.required)
                return
            }
            if (batch.isEmpty()) {
                batchBox.error = getString(R.string.required)
                return
            }
            if (semester.isEmpty()) {
                semesterBox.error = getString(R.string.required)
                return
            } else {
                if (semester.toInt() in 1..5) {
                    if (section?.isEmpty() == true) {
                        sectionBox.error = getString(R.string.required)
                        return
                    }
                } else {
                    if (classBatch?.isEmpty() == true) {
                        classBatchBox.error = getString(R.string.required)
                        return
                    }
                }
            }
            if (isLab && group.isEmpty()) {
                groupBox.error = getString(R.string.required)
                return
            }
            if (specialization.isEmpty() || selectedSpecialization == 0) {
                specializationBox.error = getString(R.string.required)
                return
            }
            progressBar.visibility = ProgressBar.VISIBLE
            addSubjectButton.visibility = TextView.INVISIBLE
            sharedPrefProfile =
                activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
            val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
            val id = sharedPrefProfile?.getInt(ID_KEY, 0)!!
            if (token == null || id == 0) {
                findNavController().popBackStack()
                return
            }
            if (section?.isEmpty() == true) section = null
            if (classBatch?.isEmpty() == true) classBatch = null

            val body = FacultySubjectsBody().apply {
                this.subjects = FacultySubjectsResponse().apply {
                    add(FacultySubject(id=0).apply {
                        this.batch = batch
                        this.branchCode = selectedBranch
                        this.group = group
                        this.isLab = isLab
                        this.section = section
                        this.subjectCode = selectedSubject
                        this.semester = semester
                        this.classBatch = classBatch
                        this.specialization = selectedSpecialization
                    })
                }
            }

            lifecycleScope.launch(Dispatchers.IO) {
                repository.remote.addFacultySubjects(token, body)
                    .enqueue(object : retrofit2.Callback<Any> {
                        override fun onResponse(
                            call: retrofit2.Call<Any>,
                            response: retrofit2.Response<Any>
                        ) {
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                addSubjectButton.visibility = TextView.VISIBLE
                                if (response.isSuccessful) {
                                    Log.d(LOG_TAG, "Assign Subject Successful")
                                    Snackbar.make(view, "Subject Added", Snackbar.LENGTH_SHORT)
                                        .show()
                                    findNavController().popBackStack()
                                } else {
                                    Log.d(
                                        LOG_TAG,
                                        "Assign subject unsuccessful code: ${response.code()}"
                                    )
                                    when (response.code()) {
                                        400 -> {
                                            Snackbar.make(
                                                view,
                                                "Give values for each field!!",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }

                                        403 -> {
                                            Snackbar.make(
                                                view,
                                                "Subject already assigned!!",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }

                                        else -> {
                                            Snackbar.make(
                                                view,
                                                getString(R.string.server_error_message),
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                            findNavController().popBackStack()
                                        }
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<Any>, t: Throwable) {
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                addSubjectButton.visibility = TextView.VISIBLE
                                val msg = if (t.message.toString()
                                        .startsWith(getString(R.string.error_prefix))
                                ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            }
                            Log.d(LOG_TAG, "Assign Subject Request Failed")
                        }

                    })
            }
        }
    }

    private fun makeBatchArray(currYearInt: Int): Array<String> {
        return arrayOf(
            "${currYearInt - 1}",
            "$currYearInt",
            "${currYearInt + 1}",
            "${currYearInt + 2}",
            "${currYearInt + 3}",
            "${currYearInt + 4}"
        )
    }

    private fun createBranchItemList(jsonArray: JSONArray): List<BranchItem> {
        val list = mutableListOf<BranchItem>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val branchItem = BranchItem(
                jsonObject.getString("branch_code"),
                jsonObject.getString("branch_name"),
                jsonObject.getString("branch_slug")
            )
            list.add(branchItem)
        }
        return list.toList()
    }

    private fun createSubjectItemList(jsonArray: JSONArray): List<SubjectItem> {
        val list = mutableListOf<SubjectItem>()
        val stringList = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val subjectItem = SubjectItem(
                jsonObject.getString("subject_code"),
                jsonObject.getString("subject_name")
            )
            list.add(subjectItem)
            stringList.add(subjectItem.toString())
        }
        subjectStringList = stringList
        return list.toList()
    }

    private fun createSpecializationItemList(jsonArray: JSONArray): List<SpecializationItem> {
        val list = mutableListOf<SpecializationItem>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val specializationItem = SpecializationItem(
                jsonObject.getInt("id"),
                jsonObject.getString("specialization_name")
            )
            list.add(specializationItem)
        }
        return list.toList()
    }
}