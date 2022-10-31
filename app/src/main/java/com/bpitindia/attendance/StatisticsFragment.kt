package com.bpitindia.attendance

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Insets
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


private const val BATCH = "batch"
private const val SECTION = "section"
private const val BRANCH = "branch"
private const val AUTHORIZATION = "Authorization"
private const val IS_LAB = "is_lab"
private const val GROUP = "group"
private const val SUBJECT = "subject"
private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class StatisticsFragment : Fragment() {
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var token: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataTextView: TextView
    private lateinit var tableLayout: FixedHeaderTableLayout

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
            findNavController().navigate(R.id.action_statisticsFragment_to_loginFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tableLayout = view.findViewById(R.id.FixedHeaderTableLayout)
        progressBar = view.findViewById(R.id.stats_progress_bar)
        noDataTextView = view.findViewById(R.id.no_data)
        val sdf = SimpleDateFormat("MM", Locale.getDefault())
        val currentMonth = sdf.format(Date())
        Log.d("debug", "current month $currentMonth")
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_stats_fragment, menu)
                val spinner = menu.findItem(R.id.spinner).actionView as Spinner
                ArrayAdapter.createFromResource(
                    activity!!,
                    R.array.months_array,
                    android.R.layout.simple_spinner_item
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                }
                spinner.setSelection(Integer.parseInt(currentMonth)-1)
                spinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val month: String = parent?.getItemAtPosition(position) as String
                        tableLayout.removeAllViews()
                        fetchData(month)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun fetchData(month: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        noDataTextView.visibility = TextView.GONE
        Log.d("debug", "stats month $month")
        val httpUrlBuilder: HttpUrl.Builder = HttpUrl.Builder()
            .scheme("https")
            .host(getString(R.string.api_host))
            .addPathSegment("api")
            .addPathSegment("student")
            .addPathSegment("attendance")
            .addPathSegment("stats")
            .addQueryParameter("batch", batch.toString())
            .addQueryParameter("branch", branch)
            .addQueryParameter("subject", subject)
            .addQueryParameter("section", section)
            .addQueryParameter("month", findMonth(month))
            .addQueryParameter("group", group)
        val httpUrl = httpUrlBuilder.build()
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(httpUrl)
                .addHeader(AUTHORIZATION, token!!)
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            context,
                            "Some error occurred!!",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = ProgressBar.INVISIBLE
                    }
                    Log.d("debug", "Some error occurred!!")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonObject = response.body?.string()
                            ?.let { JSONObject(it) }
                        var msg: String? = ""
                        try {
                            msg = jsonObject?.getString("msg")
                        } catch (e: Exception) {
                        }
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            if (msg == "No data available") {
                                noDataTextView.text = getString(R.string.no_data, month)
                                noDataTextView.visibility = TextView.VISIBLE
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                return@runOnUiThread
                            }
                            val arrayJSONColumns = jsonObject?.getJSONArray("columns")
                            val studentData = jsonObject?.getJSONArray("student_data")
                            displayData(arrayJSONColumns, studentData)
                        }
                    } else {
                        activity?.deleteSharedPreferences(SHARED_PREFERENCES_NAME)
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                "Session Expired!! Log in again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().navigate(R.id.action_statisticsFragment_to_loginFragment)
                        }
                    }
                    response.body?.close()
                }
            })

        }
    }

    private fun displayData(columns: JSONArray?, studentData: JSONArray?) {

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
        for (i in 0 until columns!!.length()) {
            val colName = columns.getString(i).toDate()?.formatTo("dd-MM-yy")
            val tv = TextView(context)
            tv.gravity = Gravity.CENTER
            tv.text = colName
            tv.setPadding(20, 5, 20, 5)
            tv.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary2))
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            columnHeader.addView(tv)
        }
        columnHeaderLayout.addView(columnHeader)

        val rowHeaderLayout = FixedHeaderSubTableLayout(context)
        for (i in 0 until studentData!!.length()) {
            val jsonObj = studentData.getJSONObject(i)
            val name = "${jsonObj.getString("class_roll_number")}. ${
                jsonObj.getString("name").uppercase()
            }"
            val headerRow = FixedHeaderTableRow(context)
            val tv = TextView(context)
            tv.gravity = Gravity.START
            tv.text = name
            val width = getScreenWidth(requireActivity())*2/5
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
        for (i in 0 until studentData.length()) {
            val jsonArray = studentData.getJSONObject(i).getJSONArray("attendance_data")
            val mainRow = FixedHeaderTableRow(context)
            var cumulativeSum = 0
            for (j in 0 until jsonArray.length()) {
                val isPresent = jsonArray.getInt(j)
                cumulativeSum += isPresent
                val tv = TextView(context)
                tv.gravity = Gravity.CENTER
                tv.text = cumulativeSum.toString()
                tv.setPadding(20, 20, 20, 20)
                tv.setTypeface(tv.typeface, Typeface.BOLD)
                if (isPresent == 0) tv.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.absent_color
                    )
                )
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

    private fun getScreenWidth(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }

    private fun String.toDate(
        dateFormat: String = "dd-MM-yyyy HH:mm",
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): Date? {
        val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
        parser.timeZone = timeZone
        return parser.parse(this)
    }

    private fun Date.formatTo(
        dateFormat: String,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(this)
    }

    private fun findMonth(month: String): String {
        val mm: String = when (month) {
            "January" -> "1"
            "February" -> "2"
            "March" -> "3"
            "April" -> "4"
            "May" -> "5"
            "June" -> "6"
            "July" -> "7"
            "August" -> "8"
            "September" -> "9"
            "October" -> "10"
            "November" -> "11"
            "December" -> "12"
            else -> "0"
        }
        return mm
    }

}