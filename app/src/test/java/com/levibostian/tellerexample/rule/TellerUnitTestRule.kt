package com.levibostian.tellerexample.rule

import com.levibostian.teller.Teller
import org.junit.rules.ExternalResource

class TellerUnitTestRule: ExternalResource() {

    override fun before() {
        Teller.initUnitTesting()
    }

}