package com.levibostian.teller.extensions

import android.content.Context
import android.content.SharedPreferences
import com.levibostian.teller.util.ConstantsUtil

internal fun Context.getTellerSharedPreferences(): SharedPreferences {
    return getSharedPreferences(ConstantsUtil.NAMESPACE, Context.MODE_PRIVATE)
}