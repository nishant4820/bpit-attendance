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
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class SubjectListFragment : Fragment() {
    var jsonArray: JSONArray = JSONArray()
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var noSubjectTextView: TextView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
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
        methodProvider?.fetchProfile()
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
        val url = getString(R.string.url_complete) + getString(R.string.assigned_subjects_api_path)
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()

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
                            val msg = if (e.message.toString()
                                    .startsWith(getString(R.string.error_prefix))
                            ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                            Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            shimmerFrameLayout.stopShimmer()
                            shimmerFrameLayout.visibility = ShimmerFrameLayout.INVISIBLE
                        }
                        Log.d("debug", "subject fetch failed")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            jsonArray = JSONArray(response.body?.string())
                            response.close()
                            Log.d("debug", "subject fetch successful")
                            activity?.runOnUiThread {
                                if (jsonArray.length() == 0) {
                                    noSubjectTextView.visibility = TextView.VISIBLE
                                }
                                view.findViewById<RecyclerView>(R.id.subjectList).apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = SubjectAdapter(jsonArray)
                                }
                                shimmerFrameLayout.stopShimmer()
                                shimmerFrameLayout.visibility = ShimmerFrameLayout.INVISIBLE
                            }
                        } else {
                            Log.d("debug", "subject fetch unsuccessful code: ${response.code}")
                            activity?.runOnUiThread {
                                shimmerFrameLayout.stopShimmer()
                                shimmerFrameLayout.visibility = ShimmerFrameLayout.INVISIBLE
                                when (response.code) {
                                    401 -> {
                                        response.close()
                                        activity?.deleteSharedPreferences(SHARED_PREFERENCES_PROFILE)
                                        Toast.makeText(
                                            context,
                                            "Session Expired! Log in again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        findNavController().navigate(R.id.action_subjectListFragment_to_loginFragment)
                                    }
                                    else -> {
                                        response.close()
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
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        val url = getString(R.string.url_complete) + getString(R.string.settings_api_path)
        val request =
            Request.Builder().url(url).addHeader("Authorization", token).get().build()
        lifecycleScope.launch(Dispatchers.IO) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "settings fetch failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val jsonObject = JSONObject(response.body!!.string())
                            val subjectAssignment = jsonObject.getBoolean("SUBJECTS_ASSIGNMENT")
                            if (subjectAssignment) {
                                activity?.runOnUiThread {
                                    floatingActionButton.visibility = FloatingActionButton.VISIBLE
                                }
                            }
                            Log.d("debug", "settings fetch successful")
                        } catch (e: JSONException) {
                            Log.d("debug", "SUBJECTS_ASSIGNMENT value doesn't exist")
                        }
                    } else {
                        Log.d("debug", "settings fetch unsuccessful code: ${response.code}")
                    }
                    response.close()
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