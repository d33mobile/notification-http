package de.jl.notificationlog.livedata

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import java.util.concurrent.atomic.AtomicBoolean

fun <T> LiveData<T>.ignoreUnchanged(): LiveData<T> {
    val result = MediatorLiveData<T>()
    val isFirstChange = AtomicBoolean(false)

    result.addSource(this) {
        newValue ->

        if (isFirstChange.compareAndSet(false, true) || result.value != newValue) {
            result.value = newValue
        }
    }

    return result
}