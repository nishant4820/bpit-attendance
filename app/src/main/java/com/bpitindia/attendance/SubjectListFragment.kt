package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

private const val TOKEN = "token"
private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class SubjectListFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var token: String? = null
    var jsonArray: JSONArray = JSONArray()
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            token = it.getString(TOKEN)
        }
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subject_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ProgressBar>(R.id.subject_progress_bar).visibility = ProgressBar.VISIBLE
        fetchSubjects(view)
    }

    private fun fetchSubjects(view: View) {
        val url = getString(R.string.subject_api_url)
        val client = OkHttpClient()
        Log.d("debug", "fetchSub Token $token")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val request: Request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", token!!)
                    .get()
                    .build()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                "Subject API Failed!! Contact Developer",
                                Toast.LENGTH_SHORT
                            ).show()
                            view.findViewById<ProgressBar>(R.id.subject_progress_bar).visibility = ProgressBar.INVISIBLE
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {

                            jsonArray = JSONArray(response.body?.string())
                            Log.d("debug", "subject json size: ${jsonArray.length()}")
                            activity?.runOnUiThread {
                                view.findViewById<RecyclerView>(R.id.subjectList).apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = SubjectAdapter(jsonArray, token!!)
                                }
                                view.findViewById<ProgressBar>(R.id.subject_progress_bar).visibility = ProgressBar.INVISIBLE
                            }
                        }
                        else {
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Subject Fetching Failed", Toast.LENGTH_SHORT).show()
                                view.findViewById<ProgressBar>(R.id.subject_progress_bar).visibility = ProgressBar.INVISIBLE
                            }
                        }
                    }

                })
            }
        }

    }
}