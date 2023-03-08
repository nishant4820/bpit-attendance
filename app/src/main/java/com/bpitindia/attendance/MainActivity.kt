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
import java.util.*

const val SHARED_PREFERENCES_INTERCEPTOR = "interceptor_url"
const val URL_KEY = "url"
const val LAST_UPDATED_KEY = "time"
const val SHARED_PREFERENCES_PROFILE = "profile_information"
const val TOKEN_KEY = "token"
const val ID_KEY = "id_key"

class MainActivity : AppCompatActivity(), MyDrawerLocker {
    private lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var sharedPrefInterceptor: SharedPreferences? = null
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

    override fun onStart() {
        super.onStart()
        requestUrl()
    }

    private fun requestUrl() {
        sharedPrefInterceptor =
            getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val tunnelURL: String? = sharedPrefInterceptor?.getString(URL_KEY, null)
        if (tunnelURL == null) {
            getUrl()
            return
        }
        val lastUpdated = sharedPrefInterceptor?.getLong(LAST_UPDATED_KEY, 0)!!
        val timeNow = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).time.time
        val hours = (timeNow - lastUpdated) / 3600000.0
        Log.d("debug", "last updated: $lastUpdated, current: $timeNow, hours: $hours")
        if (hours >= 4) {
            healthCheck(tunnelURL)
        }
    }

    private fun healthCheck(tunnelUrl: String) {
        val url = "$tunnelUrl/health/"
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "health check failed " + e.message)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Please check Internet Connection!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("debug", "health response: ${response.code}")
                    if (response.code == 404) {
                        getUrl()
                    }
                }

            })
        }
    }

    fun getUrl() {
        sharedPrefInterceptor =
            getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val editor = sharedPrefInterceptor?.edit()
        val url = getString(R.string.public_api)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "get url from interceptor failed " + e.message)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Please check Internet Connection!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonObject = response.body?.string()?.let { JSONObject(it) }
                        val tunnelURL = jsonObject!!.getString(URL_KEY)
                        // Sets the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object.
                        val timeMilliseconds =
                            Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).time.time
                        editor?.putString(URL_KEY, tunnelURL)
                        editor?.putLong(LAST_UPDATED_KEY, timeMilliseconds)
                        editor?.apply()
                        Log.d("debug", "ngrok url fetch successful: $tunnelURL")
//                        checkForUpdates(1)
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong. Please try again later!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.d("debug", "get url response unsuccessful")
                    }

                }

            })
        }
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
                    Log.d("debug", "profile string $profileJSONString")
                    bundle.putString("profile", profileJSONString)
                    navController.navigate(R.id.profileFragment, bundle)
                }
                R.id.change_password -> {
                    navController.navigate(R.id.action_subjectListFragment_to_changePasswordFragment)
                }
                R.id.check_update -> {
                    checkForUpdates(2)
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

    private fun checkForUpdates(callID: Int) {
        /*
            call id used for detecting from where the function is called.
            1 -> call made by onCreate activity
            2 -> call from check for update menu item in nav drawer
         */

        sharedPrefInterceptor =
            getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val tunnelURL: String? = sharedPrefInterceptor?.getString(URL_KEY, null)
        if (tunnelURL == null) {
            Toast.makeText(
                this,
                "Something went wrong. Please try again later!",
                Toast.LENGTH_SHORT
            ).show()
            getUrl()
            return
        }
        val url = tunnelURL + getString(R.string.update_version_api_url)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "check update failed " + e.message)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Please check Internet Connection!",
                            Toast.LENGTH_SHORT
                        ).show()
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
                                    setNegativeButton("NO, THANKS") { _, _ -> }
                                }.show()
                            } else if (callID == 2) {
                                AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("No Update Available!")
                                    setMessage("Version: ${BuildConfig.VERSION_NAME}\nContact developer for any bugs.")
                                    setPositiveButton("Continue") { _, _ -> }
                                }.show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (response.code == 404) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Something went wrong. Please try again later!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                deleteSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR)
                                getUrl()
                            }
                        }
                        Log.d("debug", "check update response unsuccessful")
                    }

                }

            })
        }

    }

    private fun logout(view: View) {
        sharedPrefInterceptor =
            getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        sharedPrefProfile = getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val tunnelURL: String? = sharedPrefInterceptor?.getString(URL_KEY, null)
        if (tunnelURL == null) {
            Snackbar.make(
                view,
                "Something went wrong. Please try again later!",
                Snackbar.LENGTH_SHORT
            ).show()
            getUrl()
            return
        }
        val url = tunnelURL + getString(R.string.logout_api_url)
        val client = OkHttpClient()
        Log.d("debug", "logout token $token")
        lifecycleScope.launch(Dispatchers.IO) {
            val request =
                Request.Builder().url(url).get().addHeader("Authorization", token.toString())
                    .build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Snackbar.make(
                            view, "Please check Internet Connection!", Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    Log.d("debug", "logout failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    val navController = Navigation.findNavController(
                        this@MainActivity, R.id.fragmentContainerView
                    )
                    Log.d("debug", "logout response: ${response.code}")
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
                                    "Something went wrong. Please try again later!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                deleteSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR)
                                getUrl()
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
                        Log.d("debug", "logout response unsuccessful")
                    }
                    response.body?.close()

                }

            })
        }
    }

    fun fetchProfile(token: String, id: Int) {
        sharedPrefInterceptor =
            getSharedPreferences(SHARED_PREFERENCES_INTERCEPTOR, Context.MODE_PRIVATE)
        val tunnelURL: String? = sharedPrefInterceptor?.getString(URL_KEY, null)
        val url = tunnelURL + getString(R.string.profile_api_url, id)
        val client = OkHttpClient()
        Log.d("debug", "profile $token")
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().addHeader("Authorization", token).build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d("debug", "profile load failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("debug", "profile response ${response.message}")
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
                        Log.d("debug", "profile loading failed ${response.message}")
                    }
                    response.body?.close()
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