package com.matungos.sshing

import androidx.multidex.MultiDexApplication
import com.matungos.sshing.analitycs.TrackerManager
import com.matungos.sshing.data.DataStore
import com.matungos.sshing.utils.WidgetUtils
import java.security.Security

class SSHingApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        TrackerManager.init(this)

        DataStore.init(applicationContext)
        DataStore.commandListLiveData.observeForever {
            WidgetUtils.updateWidgets(this)
        }
        DataStore.hostListLiveData.observeForever {
            WidgetUtils.updateWidgets(this)
        }

        Security.removeProvider("BC")
        Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 0)
    }

}
