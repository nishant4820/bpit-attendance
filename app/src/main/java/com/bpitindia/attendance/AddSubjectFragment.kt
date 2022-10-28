package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class AddSubjectFragment : Fragment() {
    private var token: String? = null
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_subject, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.add_subject_recycler_view)
        progressBar = view.findViewById(R.id.add_subject_progress_bar)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = SubjectAddAdapter {
                assignSubject(it)
            }
        }
        loadSubjects()
    }

    private fun loadSubjects() {
        progressBar.visibility = ProgressBar.VISIBLE
        val url = getString(R.string.all_subject_api_url)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request : Request = Request.Builder()
                .url(url)
                .addHeader("Authorization", token!!)
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        Toast.makeText(context, "Some error occurred!!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    // change response fetching from main thread to io thread

                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        if (response.isSuccessful) {
                            val jsonArray = JSONArray(response.body?.string())
                            (recyclerView.adapter as SubjectAddAdapter).dataSet = jsonArray
                        }
                        else {
                                Toast.makeText(context, "Subject Fetching Failed!!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    response.body?.close()
                }

            })
        }
    }

    private fun assignSubject(jsonObject: JSONObject) {
        val code = jsonObject.getString("subject_code")
        Toast.makeText(context, "$code clicked", Toast.LENGTH_SHORT).show()
    }
}