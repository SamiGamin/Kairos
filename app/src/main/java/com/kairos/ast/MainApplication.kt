package com.kairos.ast

import android.app.Application
import com.kairos.ast.model.SupabaseClient

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val client = SupabaseClient.client
    }

}