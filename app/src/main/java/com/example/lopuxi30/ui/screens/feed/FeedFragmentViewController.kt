package com.example.lopuxi30.ui.screens.feed

import android.app.DatePickerDialog
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lopuxi30.data.models.Post
import com.example.lopuxi30.data.models.PostFilter
import com.example.lopuxi30.databinding.FragmentFeedBinding
import com.example.lopuxi30.ui.recyclers.feed.FeedAdapter
import com.example.lopuxi30.ui.stateholders.FeedViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FeedFragmentViewController(
    binding: FragmentFeedBinding,
    private var fragment: FeedFragment?,
    private val viewModel: FeedViewModel
) {
    private val usernameFilter = binding.usernameFilter
    private val startDate = binding.startDate
    private val endDate = binding.endDate
    private val applyFilterButton = binding.applyFilterButton
    private val postsRecyclerView = binding.postsRecyclerView
    private val swipeRefreshLayout = binding.swipeRefreshLayout

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var startDateMillis: Long? = null
    private var endDateMillis: Long? = null

    private val adapter = FeedAdapter(viewModel.token)

    fun setupViews() {
        setupRecyclerView()
        setupFilters()
        setupSwipeRefresh()
        observePosts()
        viewModel.getFeed()
    }

    private fun setupRecyclerView() {
        postsRecyclerView.layoutManager = LinearLayoutManager(fragment!!.requireContext())
        postsRecyclerView.adapter = adapter
    }

    private fun setupFilters() {
        startDate.setOnClickListener {
            showDatePicker(true)
        }

        endDate.setOnClickListener {
            showDatePicker(false)
        }

        applyFilterButton.setOnClickListener {
            applyFilter()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            fragment!!.requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val date = calendar.time
                if (isStartDate) {
                    startDate.setText(dateFormat.format(date))
                    startDateMillis = date.time
                } else {
                    endDate.setText(dateFormat.format(date))
                    endDateMillis = date.time
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyFilter() {
        val filter = PostFilter(
            username = usernameFilter.text?.toString()?.takeIf { it.isNotBlank() },
            startDate = startDateMillis,
            endDate = endDateMillis
        )
        viewModel.setFilter(filter)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observePosts() {
        fragment!!.viewLifecycleOwner.lifecycleScope.launch {
            fragment!!.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.posts.collect { posts ->
                    adapter.updatePosts(posts)
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    fun clear() {
        fragment = null
    }
}