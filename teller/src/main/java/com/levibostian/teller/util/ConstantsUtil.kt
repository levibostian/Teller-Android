package com.levibostian.teller.util


internal object ConstantsUtil {
    /**
     * When creating a constant string in Teller, use this prefix. Why? It's bad to have your constant clash with the host app. So having a prefix lessons the chance of that happening.
     */
    const val PREFIX: String = "DRIVER"

}