package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.chaos.view.PinView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

private const val EMAIL = "email"

class ValidateOtpFragment : Fragment() {
    private var email: String? = null
    private lateinit var button: TextView
    private lateinit var pinView: PinView
    private lateinit var progressBar: ProgressBar
    private lateinit var inputMethodManager: InputMethodManager
    private var sharedPrefInterceptor: SharedPreferences? = null
    private var sharedPrefProfile: SharedPreferences? = null

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
        return inflater.inflate(R.layout.fragment_validate_otp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.otp_message).text = getString(R.string.otp_message, email)
        pinView = view.findViewById(R.id.firstPinView)
        progressBar = view.findViewById(R.id.progressBarValidateOTP)
        button = view.findViewById(R.id.submit_otp_button)
        pinView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().length == 6) {
                    verifyOTP(view)
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
        pinView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                verifyOTP(view)
                true
            } else false
        }
        pinView.requestFocus()

        inputMethodManager.showSoftInput(pinView, 0)
        button.setOnClickListener {
            verifyOTP(view)
        }
    }

    private fun verifyOTP(view: View) {
        val otp: String = pinView.text.toString()
        if (otp.length != 6) {
            Snackbar.make(view, "Enter 6 digit OTP", Snackbar.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        button.visibility = TextView.INVISIBLE
        inputMethodManager.hideSoftInputFromWindow(pinView.windowToken, 0)
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
        val url = tunnelURL + getString(R.string.validate_otp_api_url)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val body: RequestBody = FormBody.Builder().add("email", email!!).add("otp", otp).build()
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
                    Log.d("debug", "Validate Request Failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        button.visibility = TextView.VISIBLE
                        if (response.isSuccessful) {
                            pinView.setText("")
                            val bundle = Bundle()
                            bundle.putString("email", email)
                            bundle.putString("otp", otp)
                            findNavController().navigate(
                                R.id.action_validateOtpFragment_to_resetPasswordFragment,
                                bundle
                            )
                        } else {
                            if (response.code == 401) {
                                pinView.setText("")
                                Snackbar.make(
                                    view,
                                    "Invalid OTP! Try again.",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            } else {
                                Snackbar.make(
                                    view,
                                    "Something went wrong. Please try again later!",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                (activity as MainActivity).getUrl()
                            }
                        }
                    }
                    response.body?.close()
                }
            })
        }
    }
}