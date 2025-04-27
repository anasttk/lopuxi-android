package com.example.lopuxi30.ui.stateholders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lopuxi30.data.data_sources.network.BASE_URL
import com.example.lopuxi30.data.data_sources.network.Network
import com.example.lopuxi30.data.models.Post
import com.example.lopuxi30.data.models.PostFilter
import com.example.lopuxi30.data.repositories.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepositoryImpl: FeedRepository
) : ViewModel() {

    private val TAG = "FeedViewModel"
    var token = ""
    private var requestNumber = 0
    private var currentFilter: PostFilter = PostFilter()

    private val _posts: MutableStateFlow<ArrayList<Post>> = MutableStateFlow(arrayListOf())
    val posts: StateFlow<ArrayList<Post>> = _posts

    fun getFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = feedRepositoryImpl.getFeed(token, "${BASE_URL}feed/$requestNumber")
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Received posts from backend: ${response.body()}")
                val list = arrayListOf<Post>()
                list.addAll(_posts.value)
                list.addAll(response.body()!!)
                _posts.value = applyFilter(list)
                requestNumber++

                if(response.body()!!.size == 5) getFeed()
            } else {
                Log.e(TAG, "Failed to get feed: ${response.code()}")
            }
        }
    }

    fun setFilter(filter: PostFilter) {
        currentFilter = filter
        refresh()
    }

    private fun applyFilter(list: ArrayList<Post>): ArrayList<Post> {
        val filteredList = arrayListOf<Post>()
        for (post in list) {
            if (currentFilter.username != null && post.author != currentFilter.username) continue
            if (currentFilter.startDate != null && post.time.time < currentFilter.startDate!!) continue
            if (currentFilter.endDate != null && post.time.time > currentFilter.endDate!!) continue
            filteredList.add(post)
        }
        return filteredList
    }

    fun refresh() {
        requestNumber = 0
        _posts.value = arrayListOf()
        getFeed()
    }
}