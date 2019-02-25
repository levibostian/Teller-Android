package com.levibostian.teller.rule

import org.junit.rules.ExternalResource
import org.mockito.MockitoAnnotations

class MockitoInitRule(private val testClass: Any): ExternalResource() {

    override fun before() {
        MockitoAnnotations.initMocks(testClass)
    }

}