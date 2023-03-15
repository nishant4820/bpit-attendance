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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class SubjectListFragment : Fragment() {
    var jsonArray: JSONArray = JSONArray()
    private var sharedPrefInterceptor: SharedPreferences? = null
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        progressBar = view.findViewById(R.id.subject_progress_bar)
    }

    override fun onStart() {
        super.onStart()
        view?.let { fetchSubjects(it) }
    }

    private fun fetchSubjects(view: View) {
        progressBar.visibility = ProgressBar.VISIBLE
        sharedPrefInterceptor =
            activity?.getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val tunnelURL: String? = sharedPrefInterceptor?.getString(URL_KEY, null)
        if (tunnelURL == null) {
            Snackbar.make(
                view,
                "Something went wrong. Please try again later!",
                Snackbar.LENGTH_SHORT
            ).show()
            (activity as MainActivity).getUrl()
            return
        }
        val url = tunnelURL + getString(R.string.subject_api_url)
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val client = OkHttpClient()

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
                            Snackbar.make(
                                view,
                                "Please check Internet Connection!",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = ProgressBar.INVISIBLE
                        }
                        Log.d("debug", "subject fetch failed")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {

                            jsonArray = JSONArray(response.body?.string())
                            Log.d("debug", "subject fetch successful")
                            activity?.runOnUiThread {
                                view.findViewById<RecyclerView>(R.id.subjectList).apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = SubjectAdapter(jsonArray)
                                }
                                progressBar.visibility = ProgressBar.INVISIBLE
                            }
                        } else {
                            Log.d("debug", "subject fetch unsuccessful code: ${response.code}")
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                when (response.code) {
                                    401 -> {
                                        activity?.deleteSharedPreferences(SHARED_PREFERENCES_PROFILE)
                                        Toast.makeText(
                                            context,
                                            "Session Expired! Log in again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        findNavController().navigate(R.id.action_subjectListFragment_to_loginFragment)
                                    }
                                    404 -> {
                                        Snackbar.make(
                                            view,
                                            "Something went wrong. Please try again later!",
                                            Snackbar.LENGTH_SHORT
                                        ).show()

                                        (activity as MainActivity).apply {
                                            deleteSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR)
                                            getUrl()
                                        }
                                    }
                                    else -> {
                                        Snackbar.make(
                                            view,
                                            "Something went wrong. Please try again later!",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        response.close()
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