package com.matungos.sshing.analitycs

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.matungos.sshing.BuildConfig
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes

object TrackerManager {

    fun init(application: Application) {

        FirebaseAnalytics.getInstance(application.applicationContext).setAnalyticsCollectionEnabled(BuildConfig.FIREBASE_ENABLED)

        if (BuildConfig.APPCENTER_ENABLED) {
            AppCenter.start(application, "5c04fb53-ce0f-41f7-989c-456e6cd7e965",
                    Analytics::class.java, Crashes::class.java)
        }
    }

}
