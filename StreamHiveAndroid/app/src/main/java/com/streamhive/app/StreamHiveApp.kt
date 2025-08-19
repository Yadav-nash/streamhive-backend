package com.streamhive.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class StreamHiveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        MobileAds.initialize(this)
        FirebaseAuth.getInstance().signInAnonymously()
        AppInit.init(this)
    }
}

