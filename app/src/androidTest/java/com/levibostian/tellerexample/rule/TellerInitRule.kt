package com.levibostian.tellerexample.rule


import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.levibostian.teller.Teller
import org.junit.rules.ExternalResource

class TellerInitRule: ExternalResource() {

    override fun before() {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext.let { application ->
            val sharedPrefs = application.getSharedPreferences("teller-testing", Context.MODE_PRIVATE)
            Teller.initTesting(sharedPrefs)
            Teller.shared.clear()
        }
    }

}