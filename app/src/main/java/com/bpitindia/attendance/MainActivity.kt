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

private const val SHARED_PREFERENCES_NAME = "shared_pref"
private const val SHARED_PREFERENCES_TOKEN_KEY = "token"

class MainActivity : AppCompatActivity(), MyDrawerLocker {
    private lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var sharedPreferences: SharedPreferences? = null
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
        checkForUpdates(1)
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
            Log.d("debug", "profile string $profileJSONString")
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

        val url = getString(R.string.update_version_api_url)
        val client = OkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request =
                Request.Builder().url(url).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    val jsonObject = response.body?.string()
                        ?.let { JSONObject(it) }
                    val newVersion = jsonObject?.getInt("versionCode")
                    val currentVersion = BuildConfig.VERSION_CODE
                    Log.d("debug", "$newVersion $currentVersion")
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
                }

            })
        }

    }

    private fun logout(view: View) {
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(SHARED_PREFERENCES_TOKEN_KEY, null)
        val url = getString(R.string.logout_api_url)
        val client = OkHttpClient()
        Log.d("debug", "logout token $token")
        lifecycleScope.launch(Dispatchers.IO) {
            val request =
                Request.Builder().url(url).get().addHeader("Authorization", token!!).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Snackbar.make(view, "Log Out Failed", Snackbar.LENGTH_SHORT).show()
                    }
                    Log.d("debug", "logout failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d("debug", "logout successful")
                    deleteSharedPreferences(SHARED_PREFERENCES_NAME)
                    runOnUiThread {
                        Snackbar.make(view, "Logout Successful", Snackbar.LENGTH_SHORT).show()
                        val navController = Navigation.findNavController(
                            this@MainActivity,
                            R.id.fragmentContainerView
                        )
                        navController.navigate(R.id.action_subjectListFragment_to_loginFragment)
                    }
                    response.body?.close()
                }

            })
        }
    }

    fun fetchProfile(token: String, id: Int) {
        val url = getString(R.string.profile_api_url, id)
        val client = OkHttpClient()
        Log.d("debug", "profile $token")
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().addHeader("Authorization", token).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Some error occurred!!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                            if (imageUrl != "null" && imageUrl != "")
                                Glide.with(view).load(imageUrl).into(imageView)
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