package co.kaush.msusf.movies

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@OpenClassOnDebug
class MSMovieRepository @Inject constructor(
    val movieApi: MSMovieApi
) {
    private val searchError = "Error searching for movie"

    suspend fun searchMovieAsync(movieName: String): MSMovie {
        var msMovie = MSMovie(errorMessage = searchError)

        withContext(Dispatchers.IO) {
            try {
                val response = movieApi.searchMovieAsync(movieName).await()
                response.body()?.let { msMovie = it }

                response.errorBody()?.let { body ->
                    msMovie = Gson().fromJson(
                            body.string(),
                            MSMovie::class.java
                    )
                }
            } catch (ex: IOException) {
                Timber.w("search Movie fail", ex)
            }
        }
        return msMovie
    }
}
