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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChangePasswordFragment : Fragment() {
    private var sharedPrefProfile: SharedPreferences? = null
    private lateinit var changeButton: TextView
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmNewPasswordLayout: TextInputLayout
    private lateinit var currentPasswordEdittext: TextInputEditText
    private lateinit var newPasswordEdittext: TextInputEditText
    private lateinit var confirmNewPasswordEdittext: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changeButton = view.findViewById(R.id.change_button)
        newPasswordLayout = view.findViewById(R.id.change_new_password)
        confirmNewPasswordLayout = view.findViewById(R.id.change_confirm_new_password)
        currentPasswordEdittext = view.findViewById(R.id.current_password_edittext)
        newPasswordEdittext = view.findViewById(R.id.change_new_password_edittext)
        confirmNewPasswordEdittext = view.findViewById(R.id.change_confirm_new_password_edittext)
        progressBar = view.findViewById(R.id.change_progressBar)
        changeButton.setOnClickListener {
            changePassword(view)
        }
        newPasswordEdittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val length = s.toString().length
                if (length < 8) {
                    newPasswordLayout.error = "Minimum Length should be 8"
                } else {
                    newPasswordLayout.error = null
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        confirmNewPasswordEdittext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newPass = newPasswordEdittext.text.toString()
                if (s.toString().equals(newPass, false) || s.toString().isEmpty()) {
                    confirmNewPasswordLayout.error = null
                } else confirmNewPasswordLayout.error = "Passwords do not match."
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        confirmNewPasswordEdittext.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                changePassword(view)
                true
            } else false
        }
    }

    private fun changePassword(view: View) {
        inputMethodManager.hideSoftInputFromWindow(confirmNewPasswordEdittext.windowToken, 0)
        val currentPassword = currentPasswordEdittext.text.toString()
        val newPassword = newPasswordEdittext.text.toString()
        if (newPassword.length < 8) {
            Snackbar.make(view, "Password should be of atleast 8 length", Snackbar.LENGTH_LONG)
                .show()
            return
        }
        val confirmNewPassword = confirmNewPasswordEdittext.text.toString()
        if (confirmNewPassword != newPassword) {
            Snackbar.make(view, "Passwords do not match.", Snackbar.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        changeButton.visibility = TextView.INVISIBLE
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        if (token == null) {
            findNavController().popBackStack()
            return
        }
        val url = getString(R.string.url_complete) + getString(R.string.reset_password_api_path)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val bodyJSONObject = JSONObject()
            bodyJSONObject.apply {
                put("current_password", currentPassword)
                put("new_password", newPassword)
                put("new_password_confirm", confirmNewPassword)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = bodyJSONObject.toString().toRequestBody(mediaType)
            val request: Request =
                Request.Builder().url(url).addHeader(AUTHORIZATION_HEADER, token).put(body).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        progressBar.visibility = ProgressBar.INVISIBLE
                        changeButton.visibility = TextView.VISIBLE
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "Change Password Request Failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    val jsonObject: JSONObject? = response.body?.string()?.let { JSONObject(it) }
                    if (response.isSuccessful) {
                        val key = jsonObject?.getString("token")
                        val newToken = "Token $key"
                        val editor = sharedPrefProfile?.edit()
                        editor?.putString(TOKEN_KEY, newToken)
                        editor?.apply()
                        activity?.runOnUiThread {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            changeButton.visibility = TextView.VISIBLE
                            val bundle = Bundle()
                            bundle.putString("fromFragment", "change")
                            findNavController().navigate(
                                R.id.action_changePasswordFragment_to_resetSuccessfulFragment,
                                bundle
                            )
                        }
                        Log.d("debug", "Password Changed")

                    } else {
                        Log.d("debug", "change password unsuccessful code: ${response.code}")
                        if (response.code == 400) {
                            val error: String = jsonObject?.getJSONArray("error")?.get(0) as String
                            Log.d("debug", "error $error")
                            activity?.runOnUiThread {
                                progressBar.visibility = ProgressBar.INVISIBLE
                                changeButton.visibility = TextView.VISIBLE
                                when (error) {
                                    "Your current password was entered incorrectly. Please enter it again." -> {
                                        currentPasswordEdittext.setText("")
                                    }
                                    "The two password fields didn't match." -> {
                                        confirmNewPasswordEdittext.setText("")
                                    }
                                    "The new password cannot be same as old password." -> {
                                        newPasswordEdittext.setText("")
                                        confirmNewPasswordEdittext.setText("")
                                    }
                                }
                                Snackbar.make(view, error, Snackbar.LENGTH_LONG).show()
                            }
                        } else {
                            activity?.runOnUiThread {
                                findNavController().popBackStack()
                            }
                        }
                    }
                    response.close()
                }

            })
        }
    }
}