package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val PROFILE = "profile"
private const val NAME = "Name"
private const val PHONE_NUMBER = "Phone Number"


class ProfileFragment : Fragment() {
    private var sharedPrefProfile: SharedPreferences? = null
    private var profile: String? = null
    private lateinit var jsonObject: JSONObject
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            profile = it.getString(PROFILE)
        }
        jsonObject = JSONObject(profile!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.name_profile).setOnClickListener {
            showAlertDialog(NAME, view)
        }
        view.findViewById<TextView>(R.id.phone_profile).setOnClickListener {
            showAlertDialog(PHONE_NUMBER, view)
        }
        setProfile(view)
    }

    private fun setProfile(view: View) {

        val imageView: CircleImageView = view.findViewById(R.id.card_image_profile)
        val imageUrl = jsonObject.getString("image_url")
        if (imageUrl != "null" && imageUrl != "") Glide.with(view).load(imageUrl).into(imageView)
        view.findViewById<TextView>(R.id.card_name_profile).text = jsonObject.getString("name")
        view.findViewById<TextView>(R.id.name_profile).text = jsonObject.getString("name")
        view.findViewById<TextView>(R.id.email_profile).text = jsonObject.getString("email")
        view.findViewById<TextView>(R.id.phone_profile).text = jsonObject.getString("phone_number")
        view.findViewById<TextView>(R.id.card_designation_profile).text =
            jsonObject.getString("designation")
        view.findViewById<TextView>(R.id.designation_profile).text =
            jsonObject.getString("designation")
        view.findViewById<TextView>(R.id.doj_profile).text = jsonObject.getString("date_joined")
    }

    private fun showAlertDialog(field: String, view: View) {
        val edittext = EditText(context)
        var textview: TextView? = null
        when (field) {
            NAME -> textview = view.findViewById(R.id.name_profile)
            PHONE_NUMBER -> textview = view.findViewById(R.id.phone_profile)
        }
        edittext.setText(textview?.text)
        edittext.maxLines = 1
        if (field == NAME) {
            edittext.filters = arrayOf(InputFilter.LengthFilter(25))
        } else if (field == PHONE_NUMBER) {
            edittext.filters = arrayOf(InputFilter.LengthFilter(10))
        }
        val layout = FrameLayout(requireContext())
        layout.setPaddingRelative(45, 25, 45, 0)
        layout.addView(edittext)

        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Edit $field")
            setView(layout)
            setPositiveButton("Save", null)
            setNegativeButton("Discard", null)
        }.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newValue = edittext.text.toString()
                if (newValue.isEmpty()) {
                    Snackbar.make(view, "Field cannot be Empty!", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (field == PHONE_NUMBER && newValue.length != 10) {
                    Snackbar.make(view, "Phone number Invalid", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                textview?.text = newValue
                when (field) {
                    NAME -> jsonObject.put("name", newValue)
                    PHONE_NUMBER -> jsonObject.put("phone_number", newValue)
                }
                view.findViewById<TextView>(R.id.card_name_profile).text =
                    jsonObject.getString("name")
                updateProfile(view)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun updateProfile(view: View) {
        sharedPrefProfile =
            activity?.getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val id = sharedPrefProfile?.getInt(ID_KEY, 0)
        if (token == null || id == null) {
            findNavController().popBackStack()
            return
        }
        val url = getString(R.string.url_subdomain) + getString(R.string.profile_api_url, id)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonObject.toString().toRequestBody(mediaType)
            val request =
                Request.Builder().url(url).patch(body).addHeader("Authorization", token).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    activity?.runOnUiThread {
                        Snackbar.make(
                            view,
                            "Please check Internet Connection!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    Log.d("debug", "profile update failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d("debug", "profile update successful")
                        (activity as? MainActivity)?.fetchProfile()
                    } else {
                        Log.d("debug", "profile update unsuccessful ${response.code}")
                        activity?.runOnUiThread {
                            Snackbar.make(view, "Profile Update Failed", Snackbar.LENGTH_SHORT)
                                .show()
                        }
                    }
                    response.close()
                }
            })
        }
    }
}