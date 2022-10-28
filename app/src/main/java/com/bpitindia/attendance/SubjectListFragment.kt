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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class SubjectListFragment : Fragment() {
    private var token: String? = null
    var jsonArray: JSONArray = JSONArray()
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var floatingActionButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
        Log.d("debug", "subject $token")
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
        progressBar = view.findViewById(R.id.subject_progress_bar)
        floatingActionButton = view.findViewById(R.id.floating_action_button)
        floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.action_subjectListFragment_to_addSubjectFragment)
        }
        fetchSubjects(view)
    }

    private fun fetchSubjects(view: View) {
        progressBar.visibility = ProgressBar.VISIBLE
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
                                "Some error occurred!!",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = ProgressBar.INVISIBLE
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
                                progressBar.visibility = ProgressBar.INVISIBLE
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
                                findNavController().navigate(R.id.action_subjectListFragment_to_loginFragment)
                            }
                        }
                        response.body?.close()
                    }

                })
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).setDrawerLocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}