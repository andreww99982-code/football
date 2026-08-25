package com.rogermichin.rmatch

import android.app.Application

class RMatchApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
