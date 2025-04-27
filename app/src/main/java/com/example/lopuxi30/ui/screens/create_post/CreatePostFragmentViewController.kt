package com.example.lopuxi30.ui.screens.create_post

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.lopuxi30.databinding.FragmentCreatePostBinding
import com.example.lopuxi30.ui.recyclers.images.ImagesAdapter
import com.example.lopuxi30.ui.recyclers.images.RecyclerImagesAdapter
import com.example.lopuxi30.ui.stateholders.AuthRegStatus
import com.example.lopuxi30.ui.stateholders.CreatePostStatus
import com.example.lopuxi30.ui.stateholders.CreatePostViewModel
import kotlinx.coroutines.launch

class CreatePostFragmentViewController(
    binding: FragmentCreatePostBinding,
    private var fragment: CreatePostFragment?,
    private val viewModel: CreatePostViewModel
) {
    private val TAG = "CreatePostFragmentViewController"
    private val postImage = binding.postImage
    private val descriptionEt = binding.descriptionEt
    private val createPostButton = binding.postButton
    private val loadingImage = binding.statusImage

    private val requestPermissionLauncher = fragment!!.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            showMessage("Для загрузки изображения необходим доступ к галерее")
        }
    }

    fun setupViews() {
        bindPostImage()
        bindDescriptionEt()
        bindCreatePostButton()
        bindCreatePostStatus()
    }

    private fun bindPostImage() {
        postImage.setOnClickListener {
            checkStoragePermission()
        }
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                fragment!!.requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun bindCreatePostStatus() {
        fragment!!.viewLifecycleOwner.lifecycleScope.launch {
            fragment!!.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createPostStatus.collect { status ->
                    when (status) {
                        CreatePostStatus.LOADING -> {
                            createPostButton.visibility = View.INVISIBLE
                            loadingImage.visibility = View.VISIBLE
                        }
                        CreatePostStatus.DONE -> {
                            showMessage("Пост успешно создан")
                            createPostButton.visibility = View.VISIBLE
                            loadingImage.visibility = View.INVISIBLE
                        }
                        CreatePostStatus.ERROR -> {
                            showMessage("Ошибка при создании поста")
                            createPostButton.visibility = View.VISIBLE
                            loadingImage.visibility = View.INVISIBLE
                        }
                        CreatePostStatus.DEFAULT -> {
                            createPostButton.visibility = View.VISIBLE
                            loadingImage.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(fragment!!.requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun bindDescriptionEt() {
        descriptionEt.addTextChangedListener {
            viewModel.setDescription(it.toString())
        }
    }

    private fun bindCreatePostButton() {
        createPostButton.setOnClickListener {
            viewModel.createPost(fragment!!.requireContext())
        }

        fragment!!.viewLifecycleOwner.lifecycleScope.launch {
            fragment!!.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.buttonEnabled.collect { isEnabled ->
                    createPostButton.isEnabled = isEnabled
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private val pickImageLauncher = fragment!!.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null) {
                val uri = result.data!!.data!!
                Log.d(TAG, "Image selected: $uri")
                postImage.load(uri)
                viewModel.setImage(uri)
            }
        }
    }

    fun clear() {
        fragment = null
    }
}