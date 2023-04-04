package com.bpitindia.attendance

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

const val SHARED_PREFERENCES_PROFILE = "profile_information"
const val TOKEN_KEY = "token"
const val ID_KEY = "id_key"
const val AUTHORIZATION_HEADER = "Authorization"

class MainActivity : AppCompatActivity(), MyDrawerLocker {
    private lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var sharedPrefProfile: SharedPreferences? = null
    var profileJSONString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.my_drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        actionBarDrawerToggle =
            ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // to make the Navigation drawer icon always appear on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        setupDrawerContent(navigationView)
        setDrawerLocked()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.getHeaderView(0).setOnClickListener {
            drawerLayout.closeDrawers()
            val bundle = Bundle()
            bundle.putString("profile", profileJSONString)
            Navigation.findNavController(this@MainActivity, R.id.fragmentContainerView)
                .navigate(R.id.profileFragment, bundle)
        }
        navigationView.setNavigationItemSelectedListener {
            drawerLayout.closeDrawers()
            val navController =
                Navigation.findNavController(this@MainActivity, R.id.fragmentContainerView)
            when (it.itemId) {
                R.id.my_profile -> {
                    val bundle = Bundle()
                    bundle.putString("profile", profileJSONString)
                    navController.navigate(R.id.profileFragment, bundle)
                }
                R.id.change_password -> {
                    navController.navigate(R.id.action_subjectListFragment_to_changePasswordFragment)
                }
                R.id.check_update -> {
                    checkForUpdates()
                }
                R.id.about -> {
                    navController.navigate(R.id.action_subjectListFragment_to_aboutFragment)
                }
            }

            true
        }
        navigationView.findViewById<TextView>(R.id.logout).setOnClickListener {
            drawerLayout.closeDrawers()
            logout(it)
        }
    }

    private fun checkForUpdates() {

        val url = getString(R.string.url_complete) + getString(R.string.update_version_api_path)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "check update failed " + e.message)
                    runOnUiThread {
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonObject = response.body?.string()?.let { JSONObject(it) }
                        val newVersion = jsonObject?.getInt("versionCode")
                        val currentVersion = BuildConfig.VERSION_CODE
                        Log.d(
                            "debug", "latest version: $newVersion current version: $currentVersion"
                        )
                        val apkURL = jsonObject?.getString("url")
                        runOnUiThread {
                            if (newVersion!! > currentVersion) {
                                AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("Update App?")
                                    setMessage("It is recommended that you update to the latest version.")
                                    setPositiveButton("UPDATE") { _, _ ->
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkURL)))
                                    }
                                    setNegativeButton("NO, THANKS", null)
                                }.show()
                            } else {
                                AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("No Update Available!")
                                    setMessage("Version: ${BuildConfig.VERSION_NAME}\nContact developer for any bugs.")
                                    setPositiveButton("Continue", null)
                                    setNeutralButton("Contact") { _, _ ->
                                        val intent = Intent(Intent.ACTION_SENDTO)
                                        intent.data = Uri.parse("mailto:")
                                        intent.putExtra(
                                            Intent.EXTRA_EMAIL,
                                            arrayOf("nishant88cseb20@bpitindia.edu.in","shubhamjindal@bpitindia.com")
                                        )
                                        intent.putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "Issue in Attendance Application"
                                        )
                                        intent.putExtra(
                                            Intent.EXTRA_TEXT,
                                            "<Describe your issue here>\n\n"
                                        )
                                        startActivity(Intent.createChooser(intent, "Send Email using:"))
                                    }
                                }.show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (response.code == 404) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.server_error_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        Log.d("debug", "check update response unsuccessful code: ${response.code}")
                    }
                    response.close()
                }

            })
        }

    }

    private fun logout(view: View) {
        sharedPrefProfile = getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val url = getString(R.string.url_complete) + getString(R.string.logout_api_path)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request =
                Request.Builder().url(url).get().addHeader("Authorization", token.toString())
                    .build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        val msg = if (e.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "logout failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    val navController = Navigation.findNavController(
                        this@MainActivity, R.id.fragmentContainerView
                    )
                    if (response.isSuccessful) {
                        Log.d("debug", "logout successful")
                        deleteSharedPreferences(SHARED_PREFERENCES_PROFILE)
                        runOnUiThread {
                            Snackbar.make(view, "Logout Successful", Snackbar.LENGTH_SHORT).show()
                            navController.navigate(R.id.action_subjectListFragment_to_loginFragment)
                        }
                    } else {
                        runOnUiThread {
                            if (response.code == 404) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.server_error_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Session Expired! Log in again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                deleteSharedPreferences(SHARED_PREFERENCES_PROFILE)
                                navController.navigate(R.id.action_subjectListFragment_to_loginFragment)
                            }
                        }
                        Log.d("debug", "logout response unsuccessful code: ${response.code}")
                    }
                    response.close()

                }

            })
        }
    }

    fun fetchProfile() {
        sharedPrefProfile =
            getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)!!
        val id = sharedPrefProfile?.getInt(ID_KEY, 0)!!
        val url = getString(R.string.url_complete) + getString(R.string.faculty_profile_api_path, id)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().addHeader("Authorization", token).build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "profile load failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d("debug", "profile loading successful")
                        profileJSONString = response.body?.string().toString()
                        val jsonObject = JSONObject(profileJSONString!!)
                        runOnUiThread {
                            val view = navigationView.getHeaderView(0)
                            val imageView =
                                view.findViewById<CircleImageView>(R.id.profile_image_header)
                            val imageUrl = jsonObject.getString("image_url")
                            if (imageUrl != "null" && imageUrl != "") Glide.with(view)
                                .load(imageUrl).into(imageView)
                            view.findViewById<TextView>(R.id.header_name).text =
                                jsonObject.getString("name")
                            view.findViewById<TextView>(R.id.header_email).text =
                                jsonObject.getString("email")
                            view.findViewById<TextView>(R.id.header_designation).text =
                                jsonObject.getString("designation")
                        }

                    } else {
                        Log.d("debug", "profile loading unsuccessful code: ${response.code}")
                    }
                    response.close()
                }

            })
        }
    }

    override fun setDrawerLocked() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        Log.d("debug", "drawer locked")
    }

    override fun setDrawerUnlocked() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        Log.d("debug", "drawer unlocked")
    }

}