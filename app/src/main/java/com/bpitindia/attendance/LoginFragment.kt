package com.bpitindia.attendance

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"
private const val SHARED_PREFERENCES_ID_KEY = "id_key"

class LoginFragment : Fragment() {
    private lateinit var button: TextView
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPassword: TextView
    private lateinit var inputMethodManager: InputMethodManager
    private var token: String = ""
    private var idKey: Int = 0
    private var sharedPreferences: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val stringObject: String? = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
        val id = sharedPreferences?.getInt(SHARED_PREFERENCES_ID_KEY, 0)
        if (stringObject != null) {
            (activity as MainActivity).fetchProfile(stringObject, id!!)
//            val bundle = Bundle()
//            bundle.putString("token", stringObject)
            findNavController().navigate(R.id.action_loginFragment_to_subjectListFragment)
        }
    }

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
        val url = getString(R.string.login_api_url)
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
        val editor = sharedPreferences?.edit()
        lifecycleScope.launch(Dispatchers.IO) {
            val body: RequestBody =
                FormBody.Builder().add("email", mailID).add("password", pass).build()
            val request: Request = Request.Builder().url(url).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    button.visibility = TextView.VISIBLE
                    activity?.runOnUiThread {
                        Snackbar.make(view, "Some error occurred", Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "login failed")
                }

                override fun onResponse(call: Call, response: Response) {

                    if (response.isSuccessful) {
                        val jsonObject = response.body?.string()?.let { JSONObject(it) }
                        val isFirstLogin = jsonObject?.getBoolean("is_first_login")
                        val key = jsonObject?.getString("token")
                        token = "Token $key"
                        idKey = jsonObject?.getInt("id")!!
                        editor?.putString(SHARED_PREFERENCES_TOKEN_KEY, token)
                        editor?.putInt(SHARED_PREFERENCES_ID_KEY, idKey)
                        editor?.apply()
                        Log.d("debug", "first token: $token id $idKey")
                        (activity as MainActivity).fetchProfile(token, idKey)
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
                            Snackbar.make(view, "Invalid Credentials", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    response.body?.close()
                }

            })
        }

    }

}