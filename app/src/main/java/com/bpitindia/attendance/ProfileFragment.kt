package com.bpitindia.attendance

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val PROFILE = "profile"

class ProfileFragment : Fragment() {
    private var profile: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            profile = it.getString(PROFILE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as MainActivity).setDrawerLocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.logout_button).setOnClickListener {
            (activity as MainActivity).logout()
        }
        setProfile(view)
    }

    private fun setProfile(view: View) {
        val jsonObject = profile?.let { JSONObject(it) }
        if (jsonObject != null) {
            view.findViewById<TextView>(R.id.name_profile).text = jsonObject.getString("name")
            view.findViewById<TextView>(R.id.email_profile).text = jsonObject.getString("email")
            view.findViewById<TextView>(R.id.phone_profile).text = jsonObject.getString("phone_number")
            view.findViewById<TextView>(R.id.designation_profile).text = jsonObject.getString("designation")
            view.findViewById<TextView>(R.id.doj_profile).text = jsonObject.getString("date_joined")
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).setDrawerUnlocked()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}