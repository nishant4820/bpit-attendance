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
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.utils.Constants.BASE_URL
import com.bpitindia.attendance.utils.Constants.ID_KEY
import com.bpitindia.attendance.utils.Constants.LOG_TAG
import com.bpitindia.attendance.utils.Constants.SHARED_PREFERENCES_PROFILE
import com.bpitindia.attendance.utils.Constants.TOKEN_KEY
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MyDrawerLocker, MyActivityMethodProvider {
    @Inject
    lateinit var repository: Repository
    private lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private var sharedPrefProfile: SharedPreferences? = null
    var profileJSONString: String? = null
    private val okHttpClient = OkHttpClient()

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
                    checkForUpdates(true)
                }
                R.id.sync_data->{
                    navController.navigate(R.id.action_subjectListFragment_to_localSubjectListFragment)
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

    fun checkForUpdates(callFromNavigationView: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.remote.checkForUpdates().enqueue(object : retrofit2.Callback<Any> {
                override fun onResponse(
                    call: retrofit2.Call<Any>,
                    response: retrofit2.Response<Any>
                ) {
                    if (response.isSuccessful) {
                        val jsonObject = JSONObject(Gson().toJson(response.body()))
                        val newVersion = jsonObject.optInt("versionCode", 0)
                        val currentVersion = BuildConfig.VERSION_CODE
                        Log.d(
                            LOG_TAG, "latest version: $newVersion current version: $currentVersion"
                        )
                        val apkURL = jsonObject.optString("url")
                        runOnUiThread {
                            if (newVersion > currentVersion) {
                                AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("Update App?")
                                    setMessage("It is recommended that you update to the latest version.")
                                    setPositiveButton("UPDATE") { _, _ ->
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkURL)))
                                    }
                                    setNegativeButton("NO, THANKS", null)
                                }.show()
                            } else if (callFromNavigationView) {
                                AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("No Update Available!")
                                    setMessage("Version: ${BuildConfig.VERSION_NAME}\nContact developer for any bugs.")
                                    setPositiveButton("Continue", null)
                                    setNeutralButton("Contact") { _, _ ->
                                        val intent = Intent(Intent.ACTION_SENDTO)
                                        intent.data = Uri.parse("mailto:")
                                        intent.putExtra(
                                            Intent.EXTRA_EMAIL,
                                            arrayOf(
                                                "nishant88cseb20@bpitindia.edu.in",
                                                "shubhamjindal@bpitindia.com",
                                                "achalkaushik@bpitindia.com"
                                            )
                                        )
                                        intent.putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "Issue in Attendance Application"
                                        )
                                        intent.putExtra(
                                            Intent.EXTRA_TEXT,
                                            "<Describe your issue here>\n\n"
                                        )
                                        startActivity(
                                            Intent.createChooser(
                                                intent,
                                                "Send Email using:"
                                            )
                                        )
                                    }
                                }.show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (response.code() == 404) {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.server_error_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        Log.d(
                            LOG_TAG,
                            "check update response unsuccessful code: ${response.code()}"
                        )
                    }
                }

                override fun onFailure(call: retrofit2.Call<Any>, t: Throwable) {
                    Log.d(LOG_TAG, "check update failed " + t.message)
                    runOnUiThread {
                        val msg = if (t.message.toString()
                                .startsWith(getString(R.string.error_prefix))
                        ) getString(R.string.internet_message) else getString(R.string.server_error_message)
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

            })
        }

    }

    private fun logout(view: View) {
        sharedPrefProfile = getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)
        val url = BASE_URL + getString(R.string.logout_api_path)
        val client = getOkHttpClient()
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
                    Log.d(LOG_TAG, "logout failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    val navController = Navigation.findNavController(
                        this@MainActivity, R.id.fragmentContainerView
                    )
                    if (response.isSuccessful) {
                        response.close()
                        Log.d(LOG_TAG, "logout successful")
                        deleteSharedPreferences(SHARED_PREFERENCES_PROFILE)
                        runOnUiThread {
                            Snackbar.make(view, "Logout Successful", Snackbar.LENGTH_SHORT).show()
                            navController.navigate(R.id.action_subjectListFragment_to_loginFragment)
                        }
                    } else {
                        Log.d(LOG_TAG, "logout response unsuccessful code: ${response.code}")
                        runOnUiThread {
                            if (response.code == 404) {
                                response.close()
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.server_error_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                response.close()
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.session_expired_message),
                                    Toast.LENGTH_SHORT
                                ).show()
                                deleteSharedPreferences(SHARED_PREFERENCES_PROFILE)
                                navController.navigate(R.id.action_subjectListFragment_to_loginFragment)
                            }
                        }
                    }
                }

            })
        }
    }

    fun fetchProfile() {
        sharedPrefProfile =
            getSharedPreferences(SHARED_PREFERENCES_PROFILE, Context.MODE_PRIVATE)
        val token = sharedPrefProfile?.getString(TOKEN_KEY, null)!!
        val id = sharedPrefProfile?.getInt(ID_KEY, 0)!!
        val url = BASE_URL + getString(R.string.faculty_profile_api_path, id)
        val client = getOkHttpClient()
        lifecycleScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().addHeader("Authorization", token).build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d(LOG_TAG, "profile load failed")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d(LOG_TAG, "profile loading successful")
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
                        Log.d(LOG_TAG, "profile loading unsuccessful code: ${response.code}")
                    }
                    response.close()
                }

            })
        }
    }

    override fun setDrawerLocked() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        Log.d(LOG_TAG, "drawer locked")
    }

    override fun setDrawerUnlocked() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        Log.d(LOG_TAG, "drawer unlocked")
    }

    override fun getOkHttpClient(): OkHttpClient {
        return okHttpClient
    }

}