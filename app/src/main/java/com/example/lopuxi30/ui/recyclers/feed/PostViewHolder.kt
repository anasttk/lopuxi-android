package com.example.lopuxi30.ui.recyclers.feed

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lopuxi30.R
import com.example.lopuxi30.data.data_sources.network.BASE_URL
import com.example.lopuxi30.data.models.Post
import com.example.lopuxi30.databinding.FeedItemBinding
import java.text.SimpleDateFormat

class PostViewHolder(private val binding: FeedItemBinding): RecyclerView.ViewHolder(binding.root) {

    private val TAG = "PostViewHolder"

    fun bind(post: Post, token: String) {
        Log.d(TAG, "Binding post: id=${post.id}, author=${post.author}, text=${post.text}, photos=${post.photos}")
        
        binding.usernameTv.text = post.author
        binding.timeTv.text = SimpleDateFormat("HH:mm dd MMM").format(post.time)
        binding.text.text = post.text

        if(post.photos.isNotEmpty()) {
            Log.d(TAG, "Post has photos: ${post.photos}")
            binding.postItemImage.visibility = View.VISIBLE
            binding.postItemImage.load("${BASE_URL}file/${post.photos[0]}") {
                addHeader("Authorization", token)
                // error(R.drawable.baseline_image) // зачем мы тут рисуем иконку, если у поста просто нет катинок
                placeholder(R.drawable.loading_animation)
            }
        } else {
            Log.d(TAG, "Post has no photos")
            binding.postItemImage.visibility = View.GONE
        }
    }

}