@file:Suppress("DEPRECATION")

package com.bpitindia.attendance

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class LoginFragment : Fragment() {
    private lateinit var button: TextView
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPassword: TextView
    private lateinit var inputMethodManager: InputMethodManager
    private var sharedPrefInterceptor: SharedPreferences? = null
    private var sharedPrefProfile: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button = view.findViewById(R.id.login)
        emailEditText = view.findViewById(R.id.email_edit_text)
        passwordEditText = view.findViewById(R.id.password_edit_text)
        forgotPassword = view.findViewById(R.id.forgotPasswordText)
        view.findViewById<TextView>(R.id.versionLogin).text = BuildConfig.VERSION_NAME
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                logIn(view)
                true
            } else false
        }
        progressBar = view.findViewById(R.id.loading)
        button.setOnClickListener {
            logIn(view)
        }
        forgotPassword.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("email", emailEditText.text.toString())
            findNavController().navigate(
                R.id.action_loginFragment_to_forgotPasswordFragment, bundle
            )
        }
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() || Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    emailEditText.error = null
                } else {
                    emailEditText.error = "Invalid Email ID"
                }

            }
        })

    }

    private fun logIn(view: View) {
        inputMethodManager.hideSoftInputFromWindow(button.windowToken, 0)
        sharedPrefInterceptor =
            activity?.getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val tunnelURL = sharedPrefInterceptor?.getString(URL_KEY, null)
        if (tunnelURL == null) {
            Snackbar.make(
                view,
                "Something went wrong. Please try again later!",
                Snackbar.LENGTH_SHORT
            ).show()
            (activity as MainActivity).getUrl()
            return
        }
        val url = tunnelURL + getString(R.string.login_api_url)
        val client = OkHttpClient()
        val mailID: String = emailEditText.text.toString().lowercase()
        val pass: String = passwordEditText.text.toString()

        if (mailID.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mailID).matches()) {
            Snackbar.make(view, "Invalid Email ID", Snackbar.LENGTH_SHORT).show()
            return
        } else if (pass.isEmpty()) {
            Snackbar.make(view, "Enter Password", Snackbar.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        button.visibility = TextView.INVISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val body: RequestBody =
                FormBody.Builder().add("email", mailID).add("password", pass).build()
            val request: Request = Request.Builder().url(url).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        button.visibility = TextView.VISIBLE
                        Snackbar.make(
                            view,
                            "Please check Internet Connection!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    Log.d("debug", "login failed")
                }

                override fun onResponse(call: Call, response: Response) {

                    if (response.isSuccessful) {
                        val jsonObject = response.body?.string()?.let { JSONObject(it) }
                        val isFirstLogin = jsonObject?.getBoolean("is_first_login")
                        val key = jsonObject?.getString("token")
                        val token = "Token $key"
                        val idKey = jsonObject?.getInt("id")!!
                        sharedPrefProfile = activity?.getSharedPreferences(
                            SHARED_PREFERENCES_PROFILE,
                            Context.MODE_PRIVATE
                        )
                        val editor = sharedPrefProfile?.edit()
                        editor?.putString(TOKEN_KEY, token)
                        editor?.putInt(ID_KEY, idKey)
                        editor?.apply()
                        Log.d("debug", "login successful")
                        (activity as MainActivity).fetchProfile()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            button.visibility = TextView.VISIBLE
                            if (isFirstLogin!!) {
                                Snackbar.make(
                                    view, "Change Password after First Login", Snackbar.LENGTH_LONG
                                ).show()
                                findNavController().navigate(R.id.action_loginFragment_to_changePasswordFragment)
                            } else {
                                Snackbar.make(view, "Login Successful", Snackbar.LENGTH_SHORT)
                                    .show()
                                findNavController().navigate(R.id.action_loginFragment_to_subjectListFragment)
                            }
                        }

                    } else {
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            button.visibility = TextView.VISIBLE
                            if (response.code == 400 || response.code == 401) {
                                Snackbar.make(view, "Invalid Credentials", Snackbar.LENGTH_SHORT)
                                    .show()
                            } else if (response.code == 404) {
                                Snackbar.make(
                                    view,
                                    "Something went wrong. Please try again later!",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                (activity as MainActivity).getUrl()
                            }
                        }
                        Log.d("debug", "login unsuccessful code: ${response.code}")
                    }
                    response.close()
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        waitForServer()
    }

    private fun waitForServer() {
        lifecycleScope.launch {
            val progressDialog =
                ProgressDialog(context, R.style.AppCompatAlertDialogStyle)
            progressDialog.setTitle("Connecting to Server")
            progressDialog.setMessage("Please Wait...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.setCancelable(false)
            progressDialog.show()
            sharedPrefInterceptor =
                activity?.getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
            val tunnelURL: String? = sharedPrefInterceptor?.getString(URL_KEY, null)
            if (tunnelURL == null) {
                Log.d("debug", "url doesn't exist, fetching url")
                getUrl()
            } else {
                Log.d("debug", "checking health of url")
                val lastUpdated = sharedPrefInterceptor?.getLong(LAST_UPDATED_KEY, 0)!!
                val timeNow = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).time.time
                val hours = (timeNow - lastUpdated) / 3600000.0
                Log.d("debug", "last updated: $lastUpdated, current: $timeNow, hours: $hours")
//                if (hours >= 4) {
                healthCheck(tunnelURL)
//                }
            }
            progressDialog.dismiss()
            sharedPrefProfile =
                activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
            val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
            val id = sharedPrefProfile?.getInt(ID_KEY, 0)
            if (token != null && id != null) {
                (activity as MainActivity).fetchProfile()
                findNavController().navigate(R.id.action_loginFragment_to_subjectListFragment)
            }
        }
    }

    private suspend fun getUrl() {
        sharedPrefInterceptor =
            activity?.getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val editor = sharedPrefInterceptor?.edit()
        val url = getString(R.string.public_api)
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonObject = response.body?.string()?.let { JSONObject(it) }
                    val tunnelURL = jsonObject!!.getString(URL_KEY)
                    val timeMilliseconds =
                        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).time.time
                    editor?.putString(URL_KEY, tunnelURL)
                    editor?.putLong(LAST_UPDATED_KEY, timeMilliseconds)
                    editor?.apply()
                    Log.d("debug", "ngrok url fetch successful: $tunnelURL")
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            context,
                            "Something went wrong. Please try again later!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.d("debug", "get url response unsuccessful code: ${response.code}")
                }
                response.close()
            } catch (e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Please check Internet Connection!", Toast.LENGTH_SHORT)
                        .show()
                }
                Log.d("debug", "get url from interceptor failed in login fragment")
            }
        }
    }

    private suspend fun healthCheck(tunnelUrl: String) {
        val url = "$tunnelUrl/health/"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("debug", "health response successful")
                } else {
                    Log.d(
                        "debug",
                        "url unhealthy, fetching new url. Response code: ${response.code}"
                    )
                    getUrl()
                }
                response.close()
            } catch (e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Please check Internet Connection!", Toast.LENGTH_SHORT)
                        .show()
                }
                Log.d("debug", "health check failed in login fragment")
            }
        }
    }

}