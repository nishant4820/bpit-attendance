package com.bpitindia.attendance

import android.content.Context
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
private const val OTP = "otp"

class ResetPasswordFragment : Fragment() {
    private var email: String? = null
    private var otp: String? = null
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmNewPasswordEditText: TextInputEditText
    private lateinit var resetButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString(EMAIL)
            otp = it.getString(OTP)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newPasswordEditText = view.findViewById(R.id.new_password_edittext)
        confirmNewPasswordEditText = view.findViewById(R.id.confirm_new_password_edittext)
        resetButton = view.findViewById(R.id.reset_button)
        progressBar = view.findViewById(R.id.reset_progressBar)
        resetButton.setOnClickListener {
            resetPassword(view)
        }
        newPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val length = s.toString().length
                if (length < 8) {
                    newPasswordEditText.error = "Minimum Length should be 8"
                } else {
                    newPasswordEditText.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        confirmNewPasswordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                resetPassword(view)
                true
            } else false
        }

    }

    private fun resetPassword(view: View) {
        inputMethodManager.hideSoftInputFromWindow(confirmNewPasswordEditText.windowToken, 0)
        val newPassword: String = newPasswordEditText.text.toString()
        if (newPassword.length < 8) {
            Snackbar.make(view, "Password should be of atleast 8 length", Snackbar.LENGTH_LONG)
                .show()
            return
        }
        val confirmNewPassword: String = confirmNewPasswordEditText.text.toString()
        if (confirmNewPassword != newPassword) {
            Snackbar.make(view, "Passwords do not match.", Snackbar.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        resetButton.visibility = TextView.INVISIBLE
        val url = getString(R.string.reset_password_api_url)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val bodyJSONObject = JSONObject()
            bodyJSONObject.apply {
                put("current_password", "")
                put("new_password", newPassword)
                put("new_password_confirm", confirmNewPassword)
                put("password_otp", otp!!)
                put("forget", true)
                put("email", email)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = bodyJSONObject.toString().toRequestBody(mediaType)
            val request: Request = Request.Builder().url(url).put(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        resetButton.visibility = TextView.VISIBLE
                        Snackbar.make(view, "Some error occurred", Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "Reset Password Request Failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    val jsonObject: JSONObject? = response.body?.string()?.let { JSONObject(it) }
                    if (response.isSuccessful) {
                        val msg = jsonObject?.getString("message")
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            resetButton.visibility = TextView.VISIBLE
                            Snackbar.make(view, msg!!, Snackbar.LENGTH_SHORT).show()
                            val bundle = Bundle()
                            bundle.putString("fromFragment", "reset")
                            findNavController().navigate(
                                R.id.action_resetPasswordFragment_to_resetSuccessfulFragment,
                                bundle
                            )
                        }
                    } else {
                        val error: String = jsonObject?.getJSONArray("error")?.get(0) as String
                        Log.d("debug", "error $error")
                        Log.d("debug", "email: $email")
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            resetButton.visibility = TextView.VISIBLE
                            Snackbar.make(view, error, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    response.body?.close()
                }

            })
        }
    }

}