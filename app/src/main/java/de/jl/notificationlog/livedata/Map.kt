package de.jl.notificationlog.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations

fun <X, Y> LiveData<X>.map(func: (X) -> Y) = Transformations.map(this, func)
fun <X, Y> LiveData<X>.switchMap(func: (X) -> LiveData<Y>) = Transformations.switchMap(this, func)
