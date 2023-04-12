package com.bpitindia.attendance

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Insets
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
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
private const val SEMESTER = "semester"

class StatisticsFragment : Fragment() {
    private var batch: String? = null
    private var section: String? = null
    private var branch: String? = null
    private var group: String? = null
    private var isLab: Boolean? = null
    private var subject: String? = null
    private var semester: Int? = null
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var noDataTextView: TextView
    private lateinit var tableLayout: FixedHeaderTableLayout
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
            semester = it.getInt(SEMESTER)
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
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tableLayout = view.findViewById(R.id.FixedHeaderTableLayout)
        progressBar = view.findViewById(R.id.stats_progress_bar)
        noDataTextView = view.findViewById(R.id.no_data)
        val monthFormat = SimpleDateFormat("MM", Locale.getDefault())
        val currentMonth = monthFormat.format(Date())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val currentYear = yearFormat.format(Date())
        val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val currentMonthYear = monthYearFormat.format(Date())
        Log.d("debug", "current month $currentMonth")
        val menuHost: MenuHost = requireActivity()
        val currMonthInt = currentMonth.toInt()
        val currYearInt = currentYear.toInt()
        val list = makeListForDropdown(currMonthInt, currYearInt)

        menuHost.addMenuProvider(object : MenuProvider {
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
                        parent: AdapterView<*>?,
                        view2: View?,
                        position: Int,
                        id: Long
                    ) {
                        val monthYear: String = parent?.getItemAtPosition(position) as String
                        tableLayout.removeAllViews()
                        fetchData(monthYear)
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
                "JAN $currYearInt", "FEB $currYearInt", "MAR $currYearInt", "APR $currYearInt",
                "MAY $currYearInt", "JUN $currYearInt", "JUL $currYearInt", "AUG $currYearInt"
            )
        } else {
            val yearX = if (currMonthInt in 7..12) currYearInt else currYearInt - 1
            val yearY = yearX + 1
            listOf(
                "AUG $yearX", "SEP $yearX", "OCT $yearX", "NOV $yearX",
                "DEC $yearX", "JAN $yearY", "FEB $yearY", "MAR $yearY"
            )
        }
        return list
    }

    private fun fetchData(monthYear: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        noDataTextView.visibility = TextView.GONE
        Log.d("debug", "stats month year $monthYear")
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        val httpUrlBuilder: HttpUrl.Builder = HttpUrl.Builder()
            .scheme(getString(R.string.url_scheme))
            .host(getString(R.string.url_host))
            .addPathSegment(getString(R.string.api_gateway))
            .addPathSegment("api")
            .addPathSegment("student")
            .addPathSegment("attendance")
            .addPathSegment("stats")
            .addQueryParameter("batch", batch.toString())
            .addQueryParameter("branch", branch)
            .addQueryParameter("subject", subject)
            .addQueryParameter("section", section)
            .addQueryParameter("month", findMonth(monthYear))
            .addQueryParameter("year", findYear(monthYear))
            .addQueryParameter("group", group)
        val httpUrl = httpUrlBuilder.build()
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request: Request = Request.Builder()
                .url(httpUrl)
                .addHeader(AUTHORIZATION, token)
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "fetch stats failed")
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        findNavController().popBackStack()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d("debug", "fetch stats successful")
                        val jsonObject = response.body?.string()
                            ?.let { JSONObject(it) }
                        response.close()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            try {
                                val arrayJSONColumns = jsonObject?.getJSONArray("columns")
                                val studentData = jsonObject?.getJSONArray("student_data")
                                displayData(arrayJSONColumns!!, studentData!!)
                            } catch (_: Exception) {
                                noDataTextView.text = getString(R.string.no_data, monthYear)
                                noDataTextView.visibility = TextView.VISIBLE
                            }
                        }
                    } else {
                        Log.d("debug", "fetch stats unsuccessful code: ${response.code}")
                        response.close()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            findNavController().popBackStack()
                        }
                    }
                }
            })

        }
    }

    private fun displayData(columns: JSONArray, studentData: JSONArray) {

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
        for (i in 0 until columns.length()) {
            val colName = columns.getString(i).formatDate("yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yy")
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
        for (i in 0 until studentData.length()) {
            val jsonObj = studentData.getJSONObject(i)
            val name = "${jsonObj.getString("class_roll_number")}. ${
                jsonObj.getString("name").uppercase()
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
        for (i in 0 until studentData.length()) {
            val jsonArray = studentData.getJSONObject(i).getJSONArray("attendance_data")
            val mainRow = FixedHeaderTableRow(context)
            var prev = -1
            for (j in 0 until jsonArray.length()) {
                val cumulativeSum = jsonArray.getInt(j)
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
                prev = cumulativeSum
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
            val wid = requireContext().resources.displayMetrics.widthPixels
            wid
        }
    }

    private fun String.formatDate(
        fromDateFormat: String,
        toDateFormat: String,
        timeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    ): String {
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