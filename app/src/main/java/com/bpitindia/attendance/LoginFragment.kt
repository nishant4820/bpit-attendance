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
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class LoginFragment : Fragment() {
    private lateinit var button: Button
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private var token: String = ""
    private var sharedPreferences: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences =
            activity?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val stringObject: String? = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
        if (stringObject != null) {
            (activity as MainActivity).fetchProfile(stringObject)
            val bundle = Bundle()
            bundle.putString("token", stringObject)
            findNavController().navigate(R.id.action_loginFragment_to_subjectListFragment, bundle)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as MainActivity).setDrawerLocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button = view.findViewById(R.id.login)
        emailEditText = view.findViewById(R.id.email_edit_text)
        passwordEditText = view.findViewById(R.id.password_edit_text)
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                button.performClick()
                true
            }
            false
        }
        progressBar = view.findViewById(R.id.loading)
        button.setOnClickListener {
            logIn()
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


    private fun logIn() {
        progressBar.visibility = ProgressBar.VISIBLE
        val url = getString(R.string.login_api_url)
        val client = OkHttpClient()
        val mailID: String = emailEditText.text.toString()
        val pass: String = passwordEditText.text.toString()

        if (mailID.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mailID).matches()) {
            Toast.makeText(context, "Invalid Email ID", Toast.LENGTH_SHORT).show()
            progressBar.visibility = ProgressBar.INVISIBLE
            return
        } else if (pass.isEmpty()) {
            Toast.makeText(context, "Enter Password", Toast.LENGTH_SHORT).show()
            progressBar.visibility = ProgressBar.INVISIBLE
            return
        }
        val editor = sharedPreferences?.edit()
        lifecycleScope.launch(Dispatchers.IO) {
            val body: RequestBody =
                FormBody.Builder().add("email", mailID).add("password", pass).build()
            val request: Request = Request.Builder().url(url).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Log In Failed", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "login failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    if (response.isSuccessful) {
                        val key = response.body?.string()
                            ?.let { JSONObject(it).getString("token") }
                        token = "Token $key"
                        editor?.putString(SHARED_PREFERENCES_TOKEN_KEY, token)
                        editor?.apply()
                        Log.d("debug", "first token: $token")
                        (activity as MainActivity).fetchProfile(token)
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT)
                                .show()
                            val bundle = Bundle()
                            bundle.putString("token", token)
                            findNavController().navigate(
                                R.id.action_loginFragment_to_subjectListFragment,
                                bundle
                            )
                        }

                    } else {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

            })
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).setDrawerUnlocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

}