package co.kaush.msusf.movies

import android.arch.lifecycle.*
import co.kaush.msusf.BaseViewModel
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieResult.ScreenLoadResult
import co.kaush.msusf.movies.MSMovieResult.SearchHistoryResult
import co.kaush.msusf.movies.MSMovieResult.SearchMovieResult
import co.kaush.msusf.movies.MSMovieViewEffect.AddedToHistoryToastEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * For this example, a simple ViewModel would have sufficed,
 * but in most real world examples we would use an AndroidViewModel
 *
 * Our Unit tests should still be able to run given this
 */
class MSMainVm(
        app: MSApp,
        private val movieRepo: MSMovieRepository
) : BaseViewModel(app) {

    private val viewStateLD = MutableLiveData<MSMovieViewState>()
    private val viewEffectLD = MutableLiveData<MSMovieViewEffect>()
    val viewState: LiveData<MSMovieViewState>
        get() = viewStateLD
    val viewEffects: LiveData<MSMovieViewEffect>
        get() = viewEffectLD

    private var currentViewState = MSMovieViewState()
        set(value) {
            field = value
            viewStateLD.value = value
        }

    private var searchMovieJob: Job? = null

    fun onEvent(event: MSMovieEvent) {
        Timber.d("----- event ${event.javaClass.simpleName}")

        when (event) {
            is MSMovieEvent.ScreenLoadEvent -> { onScreenLoad() }
            is MSMovieEvent.SearchMovieEvent -> { onSearchMovie(event.searchedMovieTitle) }
            is MSMovieEvent.AddToHistoryEvent -> { onAddToHistory() }
            is MSMovieEvent.RestoreFromHistoryEvent -> { onRestoreFromHistory(event.movieFromHistory) }
        }
    }

    // -----------------------------------------------------------------------------------
    // Internal helpers

    private fun resultToViewState(result: Lce<MSMovieResult>) {
        Timber.d("----- result $result")

        currentViewState = when (result) {
            is Lce.Content -> {
                when (result.packet) {
                    is ScreenLoadResult -> {
                        currentViewState.copy(searchBoxText = "")
                    }
                    is SearchMovieResult -> {
                        val movie: MSMovie = result.packet.movie
                        currentViewState.copy(
                                searchedMovieTitle = movie.title,
                                searchedMovieRating = movie.ratingSummary,
                                searchedMoviePoster = movie.posterUrl,
                                searchedMovieReference = movie
                        )
                    }

                    is SearchHistoryResult -> {
                        result.packet.movieHistory
                                ?.let {
                                    val adapterList: MutableList<MSMovie> =
                                            mutableListOf(*currentViewState.adapterList.toTypedArray())
                                    adapterList.add(it)
                                    currentViewState.copy(adapterList = adapterList)
                                } ?: currentViewState.copy()
                    }
                }
            }

            is Lce.Loading -> {
                currentViewState.copy(
                        searchBoxText = null,
                        searchedMovieTitle = "Searching Movie...",
                        searchedMovieRating = "",
                        searchedMoviePoster = "",
                        searchedMovieReference = null
                )
            }

            is Lce.Error -> {
                when (result.packet) {
                    is SearchMovieResult -> {
                        val movie: MSMovie = result.packet.movie
                        currentViewState.copy(searchedMovieTitle = movie.errorMessage!!)
                    }
                    is SearchHistoryResult -> {
                        val movie: MSMovie = result.packet.movieHistory!!
                        currentViewState.copy(searchedMovieTitle = movie.errorMessage!!)
                    }
                    else -> currentViewState.copy(searchedMovieTitle = ERROR_UNREACHABLE)
                }
            }
        }
    }

    private fun resultToViewEffect(result: Lce<MSMovieResult>) {
        if (result is Lce.Content && result.packet is SearchHistoryResult) {
            viewEffectLD.value = AddedToHistoryToastEffect
        }
    }

    // -----------------------------------------------------------------------------------
    // use cases

    private fun onScreenLoad() {
        resultToViewState(Lce.Content(ScreenLoadResult))
    }

    private fun onSearchMovie(searchedMovieTitle: String) {
        resultToViewState(Lce.Loading())
        if (searchMovieJob?.isActive == true) searchMovieJob?.cancel()

        searchMovieJob = launch {
            val msMovie = movieRepo.searchMovieAsync(searchedMovieTitle)
            val result: Lce<MSMovieResult> = if (msMovie.errorMessage?.isEmpty() == false) {
                Lce.Error(SearchMovieResult(msMovie))
            } else {
                Lce.Content(SearchMovieResult(msMovie))
            }
            resultToViewState(result)
        }
    }

    private fun onAddToHistory() {
        val movieResult: MSMovie? = currentViewState.searchedMovieReference
        if (movieResult == null) {
            Timber.w("couldn't find searched movie reference : $movieResult")
            return
        }

        val adapterList: List<MSMovie> = currentViewState.adapterList

        val result: Lce<MSMovieResult> = if (!adapterList.contains(movieResult)) {
            Lce.Content(SearchHistoryResult(movieResult))
        } else {
            Lce.Content(SearchHistoryResult(null))
        }

        resultToViewState(result)
        resultToViewEffect(result)
    }

    private fun onRestoreFromHistory(movieFromHistory: MSMovie) {
        resultToViewState(Lce.Content(SearchMovieResult(movieFromHistory)))
    }
}

// -----------------------------------------------------------------------------------
// LCE

sealed class Lce<T> {
    class Loading<T> : Lce<T>()
    data class Content<T>(val packet: T) : Lce<T>()
    data class Error<T>(val packet: T) : Lce<T>()
}

// -----------------------------------------------------------------------------------

class MSMainVmFactory(
        private val app: MSApp,
        private val movieRepo: MSMovieRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MSMainVm(app, movieRepo) as T
    }
}

// -----------------------------------------------------------------------------------
// CUSTOM ERROR MESSAGES

const val ERROR_UNREACHABLE = "Unknown Error. Please contact support."
