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
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.data.models.LoginResponse
import com.bpitindia.attendance.utils.Constants.ID_KEY
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {
    @Inject
    lateinit var repository: Repository
    private lateinit var button: TextView
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPassword: TextView
    private lateinit var inputMethodManager: InputMethodManager
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
        emailLayout = view.findViewById(R.id.email)
        passwordLayout = view.findViewById(R.id.password)
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty() || Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    emailLayout.error = null
                } else {
                    emailLayout.error = "Invalid Email ID"
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }

    private fun logIn(view: View) {
        inputMethodManager.hideSoftInputFromWindow(button.windowToken, 0)
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
            try {
                repository.remote.login(mapOf("email" to mailID, "password" to pass))
                    .enqueue(object : Callback<LoginResponse> {
                        override fun onResponse(
                            call: Call<LoginResponse>,
                            response: Response<LoginResponse>
                        ) {
                            if (response.isSuccessful) {
                                val body = response.body()
                                val isFirstLogin = body?.isFirstLogin
                                val key = body?.token
                                val token = "Token $key"
                                val idKey = body?.id!!
                                sharedPrefProfile = activity?.getSharedPreferences(
                                    SHARED_PREFERENCES_PROFILE,
                                    Context.MODE_PRIVATE
                                )
                                val editor = sharedPrefProfile?.edit()
                                editor?.putString(TOKEN_KEY, token)
                                editor?.putInt(ID_KEY, idKey)
                                editor?.apply()
                                Log.d(LOG_TAG, "login successful")
                                activity?.runOnUiThread {
                                    progressBar.visibility = ProgressBar.INVISIBLE
                                    button.visibility = TextView.VISIBLE
                                    if (isFirstLogin == true) {
                                        Snackbar.make(
                                            view,
                                            "Change Password after First Login",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                        findNavController().navigate(R.id.action_loginFragment_to_changePasswordFragment)
                                    } else {
                                        Snackbar.make(
                                            view,
                                            "Login Successful",
                                            Snackbar.LENGTH_SHORT
                                        )
                                            .show()
                                        findNavController().navigate(R.id.action_loginFragment_to_subjectListFragment)
                                    }
                                }

                            } else {
                                activity?.runOnUiThread {
                                    progressBar.visibility = ProgressBar.INVISIBLE
                                    button.visibility = TextView.VISIBLE
                                    if (response.code() == 400 || response.code() == 401) {
                                        Snackbar.make(
                                            view,
                                            "Invalid Credentials",
                                            Snackbar.LENGTH_SHORT
                                        )
                                            .show()
                                    } else if (response.code() == 404) {
                                        Log.d(LOG_TAG, "code 404")
                                        Snackbar.make(
                                            view,
                                            getString(R.string.server_error_message),
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                Log.d(LOG_TAG, "login unsuccessful code: ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                button.visibility = TextView.VISIBLE
                                val msg = if (t.message.toString()
                                        .startsWith(getString(R.string.error_prefix))
                                ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            }
                            Log.d(LOG_TAG, "login failed")
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
            Log.d(LOG_TAG, "checking health of url")
            val serverHealth = healthCheck()
            if (serverHealth) {
                (activity as? MainActivity)?.checkForUpdates(false)
            }
            sharedPrefProfile =
                activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
            val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
            val id = sharedPrefProfile?.getInt(ID_KEY, 0)
            progressDialog.dismiss()
            if (serverHealth && token != null && id != null) {
                findNavController().navigate(R.id.action_loginFragment_to_subjectListFragment)
            }
        }
    }

    private suspend fun healthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            var healthSuccessful = false
            try {
                val response = repository.remote.health()
                if (response.isSuccessful) {
                    healthSuccessful = true
                    Log.d(LOG_TAG, "health response successful")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            getString(R.string.server_error_message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.d(LOG_TAG, "health response unsuccessful")
                }
            } catch (e: ConnectException) {
                Log.d(LOG_TAG, "server unavailable")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        getString(R.string.server_error_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: UnknownHostException) {
                Log.d(LOG_TAG, "no internet")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        getString(R.string.internet_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, "some error occurred")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        getString(R.string.server_error_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            healthSuccessful
        }
    }


}