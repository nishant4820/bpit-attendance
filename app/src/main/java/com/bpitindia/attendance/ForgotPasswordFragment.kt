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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val EMAIL = "email"


class ForgotPasswordFragment : Fragment() {
    private var email: String? = null
    private lateinit var resetEmailEditText: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var button: TextView
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
            } else false
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
        val url = getString(R.string.url_complete) + getString(R.string.forgot_password_api_path)
        val client = methodProvider?.getOkHttpClient() ?: OkHttpClient()
        val mailID: String = resetEmailEditText.text.toString()
        if (mailID.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mailID).matches()) {
            Snackbar.make(view, "Invalid Email ID", Snackbar.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        button.visibility = TextView.INVISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val bodyJSONObject = JSONObject()
            bodyJSONObject.apply {
                put("email", mailID)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = bodyJSONObject.toString().toRequestBody(mediaType)
//            val body: RequestBody = FormBody.Builder().add("email", mailID).build()
            val request: Request = Request.Builder().url(url).post(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        button.visibility = TextView.VISIBLE
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "OTP Request Failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.close()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            button.visibility = TextView.VISIBLE
                            val bundle = Bundle()
                            bundle.putString("email", mailID)
                            findNavController().navigate(
                                R.id.action_forgotPasswordFragment_to_validateOtpFragment,
                                bundle
                            )
                        }
                        Log.d("debug", "OTP generated successfully")
                    } else {
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            button.visibility = TextView.VISIBLE
                            if (response.code == 401) {
                                Snackbar.make(view, "User not found", Snackbar.LENGTH_SHORT).show()
                            } else {
                                Snackbar.make(
                                    view,
                                    getString(R.string.server_error_message),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                        Log.d("debug", "OTP Request unsuccessful code: ${response.code}")
                        response.close()
                    }
                }

            })
        }
    }
}