package com.bpitindia.attendance

import android.content.Context
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
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

private const val EMAIL = "email"

class ForgotPasswordFragment : Fragment() {
    private var email: String? = null
    private lateinit var resetEmailEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var button: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString(EMAIL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetEmailEditText = view.findViewById(R.id.reset_email_edit_text)
        progressBar = view.findViewById(R.id.reset_progressBar)
        resetEmailEditText.setText(email)
        button = view.findViewById(R.id.request_otp_button)
        resetEmailEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                requestOTP(view)
                true
            }
            false
        }
        resetEmailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() || Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    resetEmailEditText.error = null
                } else {
                    resetEmailEditText.error = "Invalid Email ID"
                }
            }

        })
        button.setOnClickListener {
            requestOTP(view)
        }
    }

    private fun requestOTP(view: View) {
        inputMethodManager.hideSoftInputFromWindow(resetEmailEditText.windowToken, 0)
        val url = getString(R.string.forgot_password_api_url)
        val client = OkHttpClient()
        val mailID: String = resetEmailEditText.text.toString()
        if (mailID.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mailID).matches()) {
            Toast.makeText(context, "Invalid Email ID", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        button.visibility = TextView.INVISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val body: RequestBody = FormBody.Builder().add("email", mailID).build()
            val request: Request = Request.Builder().url(url).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        button.visibility = TextView.VISIBLE
                        Toast.makeText(context, "Some error occurred", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "OTP Request Failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val msg =
                            response.body?.string()?.let { JSONObject(it).getString("message") }
                        Log.d("debug", "msg $msg")

                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            button.visibility = TextView.VISIBLE
                            if (msg == "Invalid Email") {
                                Snackbar.make(view, "User not found", Snackbar.LENGTH_SHORT).show()
                            } else {
                                val bundle = Bundle()
                                bundle.putString("email", mailID)
                                findNavController().navigate(
                                    R.id.action_forgotPasswordFragment_to_validateOtpFragment,
                                    bundle
                                )
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            button.visibility = TextView.VISIBLE
                            Toast.makeText(context, "Failed to Request OTP", Toast.LENGTH_SHORT)
                                .show()
                        }
                        Log.d("debug", "api unsuccessful")
                    }
                    response.body?.close()
                }

            })
        }
    }
}