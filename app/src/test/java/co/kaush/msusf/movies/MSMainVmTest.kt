package co.kaush.msusf.movies

import android.arch.core.executor.testing.InstantTaskExecutorRule
import co.kaush.msusf.MSApp
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import co.kaush.msusf.movies.MSMovieViewEffect.*
import co.kaush.msusf.movies.livedatatesting.test
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.mock

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class MSMainVmTest {

    @get:Rule val testRule = InstantTaskExecutorRule()

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    private lateinit var viewModel: MSMainVm

    @Test
    fun onScreenLoad_searchBoxText_shouldBeCleared() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        viewModel.onEvent(ScreenLoadEvent)

        assertEquals("", viewModel.viewState.value?.searchBoxText)
    }

    @Test
    fun onSearchingMovie_shouldSeeSearchResults() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val testObserver = viewModel.viewState.test()

        viewModel.onEvent(SearchMovieEvent("blade runner 2049"))

        testObserver.assertValue { it.searchedMovieTitle == "Searching Movie..." }
                .awaitNextValue()
                .assertValue {
                    it.searchedMovieTitle == "Blade Runner 2049" &&
                            it.searchedMoviePoster == "https://m.media-amazon.com/images/M/MV5BNzA" +
                            "1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg" &&
                            it.searchedMovieRating == "\n8.1/10 (IMDB)\n87% (RT)"
                }

        testObserver.assertHistorySize(2)
    }

    @Test
    fun onClickingMovieSearchResult_shouldPopulateHistoryList() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val testObserverState = viewModel.viewState.test()
        val testObserverEffects = viewModel.viewEffects.test()

        viewModel.onEvent(SearchMovieEvent("blade runner 2049"))

        testObserverState.assertHasValue()
                .awaitNextValue()
                .assertHistorySize(2)

        viewModel.onEvent(AddToHistoryEvent)

        testObserverState.assertHistorySize(3)
                .value().apply {
                    assertThat(searchBoxText).isEqualTo(null) // prevents search box from reset
                    assertThat(adapterList).hasSize(1)
                    assertThat(adapterList[0]).isEqualTo(bladeRunner2049)
                }
        testObserverEffects.assertHistorySize(1)
                .assertValue { it is AddedToHistoryToastEffect }
    }

    @Test
    fun onClickingMovieSearchResultTwice_shouldShowToastEachTime() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val testObserverState = viewModel.viewState.test()
        val testObserverEffects = viewModel.viewEffects.test()
        viewModel.onEvent(SearchMovieEvent("blade runner 2049"))

        testObserverState.awaitNextValue()

        viewModel.onEvent(AddToHistoryEvent)
        viewModel.onEvent(AddToHistoryEvent)

        testObserverEffects.assertHistorySize(2)
                .valueHistory().apply {
                    assertThat(this[0] is AddedToHistoryToastEffect)
                    assertThat(this[1] is AddedToHistoryToastEffect)
                }
    }

    @Test
    fun onClickingMovieHistoryResult_ResultViewIsRepopulatedWithInfo() {
        viewModel = MSMainVm(mockApp, mockMovieRepo)

        val testObserverState = viewModel.viewState.test()

        // populate history
        viewModel.onEvent(SearchMovieEvent("blade runner 2049"))
        testObserverState.assertHasValue()
                .awaitNextValue()
        viewModel.onEvent(AddToHistoryEvent)
        viewModel.onEvent(SearchMovieEvent("blade"))
        testObserverState.assertHasValue()
                .awaitNextValue()
        viewModel.onEvent(AddToHistoryEvent)

        // check that the response is showing Blade
        testObserverState.assertValue { it.searchedMovieTitle == "Blade" }

        // click blade runner 2049 from history
        viewModel.onEvent(RestoreFromHistoryEvent(bladeRunner2049))
        testObserverState.assertValue {
            it.searchedMovieTitle == "Blade Runner 2049" &&
                    it.searchedMovieRating == bladeRunner2049.ratingSummary
        }

        // click blade again
        viewModel.onEvent(RestoreFromHistoryEvent(blade))
        testObserverState.assertValue {
            it.searchedMovieTitle == "Blade" &&
                    it.searchedMovieRating == blade.ratingSummary
        }
    }

    private val mockApp: MSApp by lazy { mock(MSApp::class.java) }

    private val mockMovieRepo: MSMovieRepository by lazy {
        mock(MSMovieRepository::class.java).apply {
            runBlocking<Unit> {
                launch(Dispatchers.Main) {
//                    println("------ thread: ${Thread.currentThread()}")
                    whenever(searchMovieAsync("blade runner 2049"))
                            .thenReturn(bladeRunner2049)
                    whenever(searchMovieAsync("blade"))
                            .thenReturn(blade)
                }
            }
        }
    }

    private val bladeRunner2049 by lazy {
        val ratingImdb = MSRating(
            source = "Internet Movie Database",
            rating = "8.1/10"
        )

        val ratingRottenTomatoes = MSRating(
            source = "Rotten Tomatoes",
            rating = "87%"
        )

        MSMovie(
            response = true,
            errorMessage = null,
            title = "Blade Runner 2049",
            ratings = listOf(ratingImdb, ratingRottenTomatoes),
            posterUrl = "https://m.media-amazon.com/images/M/MV5BNzA1Njg4NzYxOV5BMl5BanBnXkFtZTgwODk5NjU3MzI@._V1_SX300.jpg"
        )
    }

    private val blade by lazy {
        val ratingImdb = MSRating(
            source = "Internet Movie Database",
            rating = "7.1/10"
        )

        val ratingRottenTomatoes = MSRating(
            source = "Rotten Tomatoes",
            rating = "54%"
        )

        MSMovie(
            response = true,
            errorMessage = null,
            title = "Blade",
            ratings = listOf(ratingImdb, ratingRottenTomatoes),
            posterUrl = "https://m.media-amazon.com/images/M/MV5BMTQ4MzkzNjcxNV5BMl5BanBnXkFtZTcwNzk4NTU0Mg@@._V1_SX300.jpg"
        )
    }
}