package co.kaush.msusf.movies

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CircularProgressDrawable
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutCompat.HORIZONTAL
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import co.kaush.msusf.MSActivity
import co.kaush.msusf.R
import co.kaush.msusf.movies.MSMovieEvent.AddToHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.RestoreFromHistoryEvent
import co.kaush.msusf.movies.MSMovieEvent.ScreenLoadEvent
import co.kaush.msusf.movies.MSMovieEvent.SearchMovieEvent
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import javax.inject.Inject

class MSMovieActivity : MSActivity() {

    @Inject
    lateinit var movieRepo: MSMovieRepository

    private lateinit var viewModel: MSMainVm
    private lateinit var listAdapter: MSMovieSearchHistoryAdapter

    private val spinner: CircularProgressDrawable by lazy {
        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()
        circularProgressDrawable
    }

    override fun inject(activity: MSActivity) {
        app.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(
            this,
            MSMainVmFactory(app, movieRepo)
        ).get(MSMainVm::class.java)

        viewModel.viewState.observe(this, Observer { render(it) })
        viewModel.viewEffects.observe(this, Observer { takeActionOn(it) })

        setupListView()
        setupViewEvents()
    }

    private fun setupViewEvents() {
        viewModel.onEvent(ScreenLoadEvent)

        ms_mainScreen_searchBtn.setOnClickListener {
            viewModel.onEvent(SearchMovieEvent(ms_mainScreen_searchText.text.toString()))
        }

        ms_mainScreen_poster.setOnClickListener {
            ms_mainScreen_poster.growShrink()
            viewModel.onEvent(AddToHistoryEvent)
        }
    }

    private fun render(viewState: MSMovieViewState?) {
        if (viewState == null) return // Ignore null values

        Timber.d("----- viewState $viewState")
        viewState.searchBoxText?.let {
            ms_mainScreen_searchText.setText(it)
        }
        ms_mainScreen_title.text = viewState.searchedMovieTitle
        ms_mainScreen_rating.text = viewState.searchedMovieRating

        viewState.searchedMoviePoster
                .takeIf { it.isNotBlank() }
                ?.let {
                    Glide.with(ctx)
                            .load(viewState.searchedMoviePoster)
                            .placeholder(spinner)
                            .into(ms_mainScreen_poster)
                } ?: run {
            ms_mainScreen_poster.setImageResource(0)
        }

        listAdapter.submitList(viewState.adapterList)
    }

    private fun takeActionOn(viewEffect: MSMovieViewEffect?) {
        if (viewEffect == null) return

        Timber.d("----- viewEffect $viewEffect")
        when (viewEffect) {
            is MSMovieViewEffect.AddedToHistoryToastEffect -> {
                Toast.makeText(this, "added to history", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListView() {
        val layoutManager = LinearLayoutManager(this, HORIZONTAL, false)
        ms_mainScreen_searchHistory.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(this, HORIZONTAL)
        dividerItemDecoration.setDrawable(
            ContextCompat.getDrawable(this, R.drawable.ms_list_divider_space)!!
        )
        ms_mainScreen_searchHistory.addItemDecoration(dividerItemDecoration)

        listAdapter = MSMovieSearchHistoryAdapter { viewModel.onEvent(RestoreFromHistoryEvent(it)) }
        ms_mainScreen_searchHistory.adapter = listAdapter
    }
}
