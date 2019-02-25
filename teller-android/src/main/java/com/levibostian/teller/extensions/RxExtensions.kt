package com.levibostian.teller.extensions

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

// Allows you to do: compositeDisposable += disposableInstance
internal operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}