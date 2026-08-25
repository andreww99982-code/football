package com.rmatch.football

import android.app.Application
import com.rmatch.football.core.di.ServiceLocator

class RMatchApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
