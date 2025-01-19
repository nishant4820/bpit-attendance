package com.bpitindia.attendance

import okhttp3.OkHttpClient

interface MyActivityMethodProvider {

    fun getOkHttpClient(): OkHttpClient

}