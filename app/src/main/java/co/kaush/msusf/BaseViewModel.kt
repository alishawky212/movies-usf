package co.kaush.msusf

import android.arch.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

open class BaseViewModel(app: MSApp) : AndroidViewModel(app), CoroutineScope {

    private val compositeJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + compositeJob

    override fun onCleared() {
        compositeJob.cancel()
        super.onCleared()
    }
}