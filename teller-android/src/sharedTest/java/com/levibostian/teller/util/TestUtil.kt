package com.levibostian.teller.util

import android.os.Looper


object TestUtil {

    fun isOnMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

}