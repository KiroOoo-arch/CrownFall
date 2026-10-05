package com.crownfall.realm

import android.app.Application
import com.crownfall.realm.di.AppContainer

class CrownfallApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.bootstrap()
    }

    override fun onTerminate() {
        container.shutdown()
        super.onTerminate()
    }
}
