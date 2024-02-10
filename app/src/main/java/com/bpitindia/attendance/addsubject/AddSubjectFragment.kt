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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bpitindia.attendance.AUTHORIZATION_HEADER
import com.bpitindia.attendance.ID_KEY
import com.bpitindia.attendance.MyActivityMethodProvider
import com.bpitindia.attendance.R
import com.bpitindia.attendance.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.TOKEN_KEY
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddSubjectFragment : Fragment() {

    private lateinit var subjectTextView: AutoCompleteTextView
    private lateinit var sectionTextView: AutoCompleteTextView
    private lateinit var branchTextView: AutoCompleteTextView
    private lateinit var batchTextView: AutoCompleteTextView
    private lateinit var semesterTextView: AutoCompleteTextView
    private lateinit var groupTextView: AutoCompleteTextView
    private lateinit var subjectLayout: TextInputLayout
    private lateinit var sectionLayout: TextInputLayout
    private lateinit var branchLayout: TextInputLayout
    private lateinit var batchLayout: TextInputLayout
    private lateinit var semesterLayout: TextInputLayout
    private lateinit var groupLayout: TextInputLayout
    private lateinit var theoryLabRadioGroup: RadioGroup
    private lateinit var labButton: RadioButton
    private lateinit var electiveSwitch: SwitchMaterial
    private lateinit var progressBar: ProgressBar
    private lateinit var addSubjectButton: TextView
    private var sharedPrefProfile: SharedPreferences? = null
    private var subjectArray: JSONArray = JSONArray()
    private var branchArray: JSONArray = JSONArray()
    private var selectedSubject: String? = null
    private var selectedBranch: String? = null
    private var subjectList: List<SubjectItem> = listOf()
    private var branchList: List<BranchItem> = listOf()
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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_subject, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subjectTextView = view.findViewById(R.id.subject_text_view)
        sectionTextView = view.findViewById(R.id.section_text_view)
        branchTextView = view.findViewById(R.id.branch_text_view)
        batchTextView = view.findViewById(R.id.batch_text_view)
        semesterTextView = view.findViewById(R.id.semester_text_view)
        groupTextView = view.findViewById(R.id.group_text_view)
        subjectLayout = view.findViewById(R.id.subject_box)
        sectionLayout = view.findViewById(R.id.section_box)
        branchLayout = view.findViewById(R.id.branch_box)
        batchLayout = view.findViewById(R.id.batch_box)
        semesterLayout = view.findViewById(R.id.semester_box)
        groupLayout = view.findViewById(R.id.group_box)
        theoryLabRadioGroup = view.findViewById(R.id.theory_lab_radio_group)
        labButton = view.findViewById(R.id.lab_button)
        electiveSwitch = view.findViewById(R.id.elective_switch)
        progressBar = view.findViewById(R.id.add_subject_progressBar)
        addSubjectButton = view.findViewById(R.id.add_subject_button)
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        lifecycleScope.launch {
            val progressDialog = ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
            progressDialog.setTitle("Fetching Subjects")
            progressDialog.setMessage("Please Wait...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.setCancelable(false)
            progressDialog.show()
            val fetchSuccessful = fetchSubjectsAndBranches()
            progressDialog.dismiss()
            if (!fetchSuccessful) {
                findNavController().popBackStack()
            }

            val subjectAdapter =
                SubjectAutoCompleteAdapter(
                    requireContext(),
                    subjectList
                )
            subjectTextView.setAdapter(subjectAdapter)
            subjectTextView.validator = object : AutoCompleteTextView.Validator {
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
            subjectTextView.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    inputMethodManager.hideSoftInputFromWindow(subjectTextView.windowToken, 0)
                }
            }
            subjectTextView.setOnItemClickListener { parent, _, position, _ ->
                inputMethodManager.hideSoftInputFromWindow(subjectTextView.windowToken, 0)
                subjectLayout.error = null
                val selectedItem = parent.adapter.getItem(position) as SubjectItem
                selectedSubject = selectedItem.subjectCode
            }
            subjectTextView.setOnEditorActionListener { _, actionId, event ->
                if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    inputMethodManager.hideSoftInputFromWindow(subjectTextView.windowToken, 0)
                    true
                } else false
            }

            val sectionAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                resources.getStringArray(R.array.section_array)
            )
            sectionTextView.setAdapter(sectionAdapter)
            sectionTextView.setOnItemClickListener { _, _, _, _ -> sectionLayout.error = null }

            val branchAdapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_dropdown_item_1line, branchList
            )
            branchTextView.setAdapter(branchAdapter)
            branchTextView.setOnItemClickListener { parent, _, position, _ ->
                branchLayout.error = null
                val selectedItem = parent.adapter.getItem(position) as BranchItem
                selectedBranch = selectedItem.branch_code
            }

            val currYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()).toInt()
            val batchAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                makeBatchArray(currYear)
            )
            batchTextView.setAdapter(batchAdapter)
            batchTextView.setOnItemClickListener { _, _, _, _ -> batchLayout.error = null }

            val semesterAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                resources.getStringArray(R.array.semester_array)
            )
            semesterTextView.setAdapter(semesterAdapter)
            semesterTextView.setOnItemClickListener { _, _, _, _ -> semesterLayout.error = null }

            theoryLabRadioGroup.setOnCheckedChangeListener { _, checkedId ->

                when (checkedId) {
                    R.id.theory_button -> {
                        groupLayout.visibility = View.GONE
                        groupTextView.setText("")
                    }
                    R.id.lab_button -> {
                        groupLayout.visibility = View.VISIBLE
                    }
                }

            }

            val groupAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                resources.getStringArray(R.array.group_array)
            )
            groupTextView.setAdapter(groupAdapter)
            groupTextView.setOnItemClickListener { _, _, _, _ -> groupLayout.error = null }

            addSubjectButton.setOnClickListener {
                addSubject(view)
            }
        }
    }

    private suspend fun fetchSubjectsAndBranches(): Boolean {
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return false
        }
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        val urlBranch = getString(R.string.url_complete) + getString(R.string.get_branches_api_path)
        val urlSubject =
            getString(R.string.url_complete) + getString(R.string.get_all_subjects_api_path)
        val requestBranch =
            Request.Builder().url(urlBranch).addHeader("Authorization", token).get().build()
        val requestSubjects =
            Request.Builder().url(urlSubject).addHeader("Authorization", token).get().build()
        return withContext(Dispatchers.IO) {
            try {
                val responseBranch = client.newCall(requestBranch).execute()
                val responseSubject = client.newCall(requestSubjects).execute()
                if (responseBranch.isSuccessful && responseSubject.isSuccessful) {
                    branchArray = JSONArray(responseBranch.body?.string())
                    branchList = createBranchItemList(branchArray)
                    subjectArray = JSONArray(responseSubject.body?.string())
                    subjectList = createSubjectItemList(subjectArray)
                    responseBranch.close()
                    responseSubject.close()
                    true
                } else {
                    responseBranch.close()
                    responseSubject.close()
                    false
                }
            } catch (e: IOException) {
                activity?.runOnUiThread {
                    val msg = if (e.message.toString()
                            .startsWith(getString(R.string.error_prefix))
                    ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                Log.d("debug", "fetch branch or subject failed in add subject fragment")
                false
            }
        }
    }

    private fun addSubject(view: View) {
        val subjectCode = subjectTextView.text.toString()
        val section = sectionTextView.text.toString()
        val branchCode = branchTextView.text.toString()
        val batch = batchTextView.text.toString()
        val semester = semesterTextView.text.toString()
        val isLab = labButton.isChecked
        val isElective = electiveSwitch.isChecked
        val group = if (isLab) groupTextView.text.toString() else "null"

        if (subjectCode.isEmpty() || selectedSubject.isNullOrEmpty()) {
            subjectLayout.error = getString(R.string.required)
            return
        }
        if (section.isEmpty()) {
            sectionLayout.error = getString(R.string.required)
            return
        }
        if (branchCode.isEmpty() || selectedBranch.isNullOrEmpty()) {
            branchLayout.error = getString(R.string.required)
            return
        }
        if (batch.isEmpty()) {
            batchLayout.error = getString(R.string.required)
            return
        }
        if (semester.isEmpty()) {
            semesterLayout.error = getString(R.string.required)
            return
        }
        if (isLab && group.isEmpty()) {
            groupLayout.error = getString(R.string.required)
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        addSubjectButton.visibility = TextView.INVISIBLE
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        val url = getString(R.string.url_complete) + getString(R.string.assigned_subjects_api_path)
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val id = sharedPrefProfile?.getInt(ID_KEY, 0)!!
        if (token == null || id == 0) {
            findNavController().popBackStack()
            return
        }
        val newSubject = JSONObject()
        newSubject.apply {
            put("batch", batch)
            put("branch_code", selectedBranch)
            put("group", group)
            put("is_lab", isLab)
            put("section", section)
            put("subject_code", selectedSubject)
            put("semester", semester)
            put("is_elective", isElective)
        }
        val jsonArray = JSONArray()
        jsonArray.put(newSubject)
        val jsonBody = JSONObject()
        jsonBody.put("subjects", jsonArray)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)
        val request: Request =
            Request.Builder().url(url).addHeader(AUTHORIZATION_HEADER, token).post(body).build()
        lifecycleScope.launch(Dispatchers.IO) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        addSubjectButton.visibility = TextView.VISIBLE
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "Assign Subject Request Failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        addSubjectButton.visibility = TextView.VISIBLE
                        if (response.isSuccessful) {
                            Log.d("debug", "Assign Subject Successful")
                            Snackbar.make(view, "Subject Added", Snackbar.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        } else {
                            Log.d(
                                "debug", "Assign subject unsuccessful code: ${response.code}"
                            )
                            when (response.code) {
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
                        response.close()
                    }
                }

            })
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
}