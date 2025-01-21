package com.bpitindia.attendance

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Insets
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.Statistics
import com.bpitindia.attendance.data.models.Student
import com.bpitindia.attendance.utils.Constants.BATCH
import com.bpitindia.attendance.utils.Constants.BRANCH
import com.bpitindia.attendance.utils.Constants.CLASS_BATCH
import com.bpitindia.attendance.utils.Constants.GROUP
import com.bpitindia.attendance.utils.Constants.IS_LAB
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SECTION
import com.bpitindia.attendance.utils.Constants.SEMESTER
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.SPECIALIZATION
import com.bpitindia.attendance.utils.Constants.SUBJECT
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableRow
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var semester: Int? = null
    private var classBatch: String? = null
    private var specialization: Int? = null
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataTextView: TextView
    private lateinit var tableLayout: FixedHeaderTableLayout
    private lateinit var downloadButton: FloatingActionButton

    private var selectedMonthYear: String = ""
    private var attendanceTimestamps: List<String>? = null
    private var studentRecords: List<Student>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            batch = it.getString(BATCH)
            section = it.getString(SECTION)
            branch = it.getString(BRANCH)
            isLab = it.getBoolean(IS_LAB)
            group = it.getString(GROUP)
            subject = it.getString(SUBJECT)
            semester = it.getInt(SEMESTER)
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tableLayout = view.findViewById(R.id.FixedHeaderTableLayout)
        progressBar = view.findViewById(R.id.stats_progress_bar)
        noDataTextView = view.findViewById(R.id.no_data)
        downloadButton = view.findViewById(R.id.fabDownload)
        downloadButton.setOnClickListener {
            initiateDownloadProcess()
        }

        val currDate = Date()
        val currentMonthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(currDate)
        val currMonthInt = SimpleDateFormat("MM", Locale.getDefault()).format(currDate).toInt()
        val currYearInt = SimpleDateFormat("yyyy", Locale.getDefault()).format(currDate).toInt()
        val list = makeListForDropdown(currMonthInt, currYearInt)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_stats_fragment, menu)
                val spinner = menu.findItem(R.id.spinner).actionView as Spinner
                val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    context!!, R.layout.simple_spinner_item, list
                )
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = arrayAdapter
                spinner.setSelection(list.indexOf(currentMonthYear.uppercase()))
                spinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view2: View?, position: Int, id: Long
                    ) {
                        selectedMonthYear = parent?.getItemAtPosition(position) as String
                        tableLayout.removeAllViews()
                        fetchData()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun makeListForDropdown(currMonthInt: Int, currYearInt: Int): List<String> {
        val list: List<String> = if (semester?.rem(2) == 0) {
            listOf(
                "JAN $currYearInt",
                "FEB $currYearInt",
                "MAR $currYearInt",
                "APR $currYearInt",
                "MAY $currYearInt",
                "JUN $currYearInt",
                "JUL $currYearInt",
                "AUG $currYearInt"
            )
        } else {
            val yearX = if (currMonthInt in 7..12) currYearInt else currYearInt - 1
            val yearY = yearX + 1
            listOf(
                "AUG $yearX",
                "SEP $yearX",
                "OCT $yearX",
                "NOV $yearX",
                "DEC $yearX",
                "JAN $yearY",
                "FEB $yearY",
                "MAR $yearY"
            )
        }
        return list
    }

    private fun fetchData() {
        progressBar.visibility = ProgressBar.VISIBLE
        noDataTextView.visibility = TextView.GONE
        downloadButton.visibility = View.GONE
        Log.d(LOG_TAG, "stats month year $selectedMonthYear")
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        val params = mapOf(
            "month" to findMonth(selectedMonthYear),
            "year" to findYear(selectedMonthYear),
            "batch" to batch.orEmpty(),
            "branch" to branch.orEmpty(),
            "subject" to subject.orEmpty(),
            "section" to section.toString(),
            "class_batch" to classBatch.toString(),
            "specialization" to specialization.toString(),
            "group" to group.orEmpty()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            repository.remote.getAttendanceStats(token, params)
                .enqueue(object : Callback<Statistics> {
                    override fun onResponse(
                        call: Call<Statistics>, response: Response<Statistics>
                    ) {
                        if (response.isSuccessful) {
                            Log.d(LOG_TAG, "fetch stats successful")
                            val body = response.body()
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.GONE
                                attendanceTimestamps = body?.columns
                                studentRecords = body?.studentData
                                Log.d(LOG_TAG, "onResponse: $studentRecords")
                                Log.d(LOG_TAG, "onResponse: $attendanceTimestamps")
                                if (attendanceTimestamps != null && studentRecords != null) {
                                    downloadButton.visibility = View.VISIBLE
                                    displayData(attendanceTimestamps!!, studentRecords!!)
                                } else {
                                    noDataTextView.text =
                                        getString(R.string.no_data, selectedMonthYear)
                                    noDataTextView.visibility = TextView.VISIBLE
                                }
                            }
                        } else {
                            Log.d(LOG_TAG, "fetch stats unsuccessful code: ${response.code()}")
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.GONE
                                findNavController().popBackStack()
                            }
                        }
                    }

                    override fun onFailure(call: Call<Statistics>, t: Throwable) {
                        Log.d(LOG_TAG, "fetch stats failed")
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.GONE
                            findNavController().popBackStack()
                        }
                    }
                })

        }
    }

    private fun displayData(columns: List<String>, studentData: List<Student>) {

        val cornerTableLayout = FixedHeaderSubTableLayout(context)
        val nameTV = TextView(context)
        nameTV.text = getString(R.string.name)
        nameTV.gravity = Gravity.CENTER
        nameTV.setPadding(5, 5, 5, 5)
        nameTV.textSize = 20.0f
        nameTV.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary2))
        nameTV.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        val cornerRow = FixedHeaderTableRow(context)
        cornerRow.addView(nameTV)
        cornerTableLayout.addView(cornerRow)

        val columnHeaderLayout = FixedHeaderSubTableLayout(context)
        val columnHeader = FixedHeaderTableRow(context)
        for (element in columns) {
            val colName = element.formatDate("yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yy")
            val tv = TextView(context)
            tv.gravity = Gravity.CENTER
            tv.text = colName
            tv.setPadding(20, 5, 20, 5)
            tv.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary2))
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            columnHeader.addView(tv)
        }
        columnHeaderLayout.addView(columnHeader)

        val width = getScreenWidth(requireActivity()) * 2 / 5
        val rowHeaderLayout = FixedHeaderSubTableLayout(context)
        for (element in studentData) {
            val name = "${element.classRollNumber}. ${
                element.name?.uppercase()
            }"
            val headerRow = FixedHeaderTableRow(context)
            val tv = TextView(context)
            tv.gravity = Gravity.START
            tv.text = name
            tv.layoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            tv.ellipsize = TextUtils.TruncateAt.END
            tv.maxLines = 1
            tv.setPadding(20, 20, 20, 20)
            tv.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            headerRow.addView(tv)
            rowHeaderLayout.addView(headerRow)
        }

        val mainTableLayout = FixedHeaderSubTableLayout(context)
        for (element in studentData) {
            val jsonArray = element.attendanceData
            val mainRow = FixedHeaderTableRow(context)
            var prev = -1
            for (j in 0 until (jsonArray?.size ?: 0)) {
                val cumulativeSum = jsonArray?.get(j)
                val tv = TextView(context)
                tv.gravity = Gravity.CENTER
                tv.text = cumulativeSum.toString()
                tv.setPadding(20, 20, 20, 20)
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                if (cumulativeSum == prev) tv.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.absent_color
                    )
                )
                prev = cumulativeSum ?: 0
                mainRow.addView(tv)
            }
            mainTableLayout.addView(mainRow)
        }

        tableLayout.addViews(
            mainTableLayout,
            columnHeaderLayout,
            rowHeaderLayout,
            cornerTableLayout
        )

    }

    private fun initiateDownloadProcess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeCSV()
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is already granted, perform the operation
            writeCSVLegacy()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            // Show permission rationale
            showPermissionRationale()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission is granted, perform the operation
                requestPermission()
            } else if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                // Show permission rationale
                showPermissionRationale()
            } else {
                // Permission is permanently denied, take to settings
                showSettingsDialog()
            }
        }

    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Storage Permission Needed")
            setMessage("We need storage permission to download files.")
            setPositiveButton("OK") { dialog, _ ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Storage Permission Needed")
            setMessage("We need storage permission to download files. You may grant them in app settings.")
            setPositiveButton("Go to Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.setData(uri)
                startActivity(intent)
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun createCsvContent(): String {
        val csvContent = StringBuilder()

        // Add header row
        csvContent.append("Roll No.,Name,")
        csvContent.append(attendanceTimestamps?.joinToString(",") {
            it.formatDate("yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yy")
        })
        csvContent.append("\n")

        // Populate data rows
        studentRecords?.forEach { student ->
            val row = StringBuilder()
            row.append("${student.classRollNumber},${student.name?.uppercase()},")
            row.append(student.attendanceData?.joinToString(",") { it.toString() } ?: "")
            csvContent.append(row.toString())
            csvContent.append("\n")
        }
        return csvContent.toString()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeCSV() {
        try {
            val csvContent = createCsvContent()

            // Write to file in Downloads folder using MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$selectedMonthYear Attendance Records.csv")
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = activity?.contentResolver
            val uri = resolver?.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                    outputStream.flush()
                }
                showDownloadConfirmation(it)
            } ?: run {
                showErrorSnackbar()
            }
        } catch (e: Exception) {
            showErrorSnackbar()
            e.printStackTrace()
        }
    }

    private fun writeCSVLegacy() {
        try {
            val csvContent = createCsvContent()

            // Create the file in legacy storage (Downloads directory)
            val downloadsDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val csvFile = File(downloadsDirectory, "$selectedMonthYear Attendance Records.csv")

            // Write CSV content to the file
            csvFile.bufferedWriter().use { out ->
                out.write(csvContent)
            }
            val fileUri = FileProvider.getUriForFile(
                requireContext(),
                "${activity?.packageName}.provider",
                csvFile
            )
            showDownloadConfirmation(fileUri)
        } catch (e: Exception) {
            showErrorSnackbar()
            e.printStackTrace()
        }
    }

    private fun showDownloadConfirmation(uri: Uri) {
        view?.let { view ->
            Snackbar.make(view, "File downloaded successfully", Snackbar.LENGTH_LONG)
                .setAction("Open") { openFile(uri) }
                .show()
        }
    }

    private fun openFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorSnackbar() {
        view?.let { view ->
            Snackbar.make(view, "Error occurred while saving the file", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun getScreenWidth(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val wid = requireContext().resources.displayMetrics.widthPixels
            wid
        }
    }

    private fun String.formatDate(
        fromDateFormat: String,
        toDateFormat: String,
        timeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    ): String {
        Log.d(LOG_TAG, "formatDate: $fromDateFormat")
        Log.d(LOG_TAG, "formatDate: $toDateFormat")
        Log.d(LOG_TAG, "formatDate: $timeZone")
        val parser = SimpleDateFormat(fromDateFormat, Locale.getDefault())
        parser.timeZone = timeZone
        val date = parser.parse(this)!!

        val formatter = SimpleDateFormat(toDateFormat, Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(date)
    }

    private fun findMonth(monthYear: String): String {
        val mm: String = when (monthYear.substring(0, 3)) {
            "JAN" -> "1"
            "FEB" -> "2"
            "MAR" -> "3"
            "APR" -> "4"
            "MAY" -> "5"
            "JUN" -> "6"
            "JUL" -> "7"
            "AUG" -> "8"
            "SEP" -> "9"
            "OCT" -> "10"
            "NOV" -> "11"
            "DEC" -> "12"
            else -> "0"
        }
        return mm
    }

    private fun findYear(monthYear: String): String {
        return monthYear.substring(4)
    }

}