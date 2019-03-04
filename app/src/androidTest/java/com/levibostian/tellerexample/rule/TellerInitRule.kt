package com.levibostian.tellerexample.rule


import androidx.test.platform.app.InstrumentationRegistry
import com.levibostian.teller.Teller
import org.junit.rules.ExternalResource

class TellerInitRule: ExternalResource() {

    override fun before() {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext.let { application ->
            Teller.init(application)
            Teller.shared.clear()
        }
    }

}