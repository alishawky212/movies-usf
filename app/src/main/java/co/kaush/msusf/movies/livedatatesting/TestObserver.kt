package co.kaush.msusf.movies.livedatatesting

import android.arch.lifecycle.LiveData

/**
 * Credit - jraska on github
 * Copied from [https://github.com/jraska/livedata-testing]
 * Reason - The library on maven only supports AndroidX
 */
fun <T> LiveData<T>.test(): TestObserver<T> {
  return TestObserver.test(this)
}
