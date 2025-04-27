package com.example.lopuxi30.ui.stateholders

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lopuxi30.data.models.CreatePostBody
import com.example.lopuxi30.data.repositories.CreatePostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

enum class CreatePostStatus { DEFAULT, LOADING, DONE, ERROR }

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostRepository: CreatePostRepository
) : ViewModel() {

    private val postBody = CreatePostBody("", "", arrayListOf(""))
    private val TAG = "CreatePostViewModel"

    private val _buttonEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val buttonEnabled: StateFlow<Boolean> = _buttonEnabled

    private val _createPostStatus: MutableStateFlow<CreatePostStatus> = MutableStateFlow(CreatePostStatus.DEFAULT)
    val createPostStatus: StateFlow<CreatePostStatus> = _createPostStatus

    var token = ""
    var uri: Uri = "".toUri()
    private var hasImage = false

    fun setUsername(username: String) {
        postBody.author = username
    }

    fun setDescription(description: String) {
        postBody.text = description
        checkButtonEnabled()
    }

    fun setImage(imageUri: Uri) {
        uri = imageUri
        hasImage = true
        Log.d(TAG, "Image set: $imageUri")
    }

    fun createPost(context: Context?) {
        // Обрезаем текст перед логированием и созданием поста
        postBody.text = postBody.text.trim()
        if (postBody.text == "") {
            _createPostStatus.value = CreatePostStatus.ERROR
            return
        }

        Log.d(TAG, "Creating post with image: $hasImage")
        _createPostStatus.value = CreatePostStatus.LOADING

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (hasImage) {
                    loadImage(uri, context)
                } else {
                    // Если изображения нет, создаем пост сразу
                    Log.d(TAG, "Creating post without image")
                    val response = createPostRepository.createPost(token, postBody) // Используем обрезанный текст
                    if (response.isSuccessful) {
                        _createPostStatus.value = CreatePostStatus.DONE
                        Log.d(TAG, "Post created successfully without image")
                    } else {
                        _createPostStatus.value = CreatePostStatus.ERROR
                        Log.e(TAG, "Failed to create post without image: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _createPostStatus.value = CreatePostStatus.ERROR
                Log.e(TAG, "Error creating post", e)
            }
        }
    }

    private fun loadImage(uri: Uri, context: Context?) {
        try {
            Log.d(TAG, "Loading image from URI: $uri")
            val path = getRealPathFromUri(uri, context)
            if (path.isEmpty()) {
                Log.e(TAG, "Failed to get real path from URI")
                _createPostStatus.value = CreatePostStatus.ERROR
                return
            }

            val file = File(path)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $path")
                _createPostStatus.value = CreatePostStatus.ERROR
                return
            }

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Uploading image")
                    val response = createPostRepository.uploadImage(token, body)
                    if (response.isSuccessful && response.body() != null) {
                        postBody.photosID[0] = response.body()!!.uri
                        Log.d(TAG, "Image uploaded successfully, creating post")
                        val postResponse = createPostRepository.createPost(token, postBody)
                        if (postResponse.isSuccessful) {
                            _createPostStatus.value = CreatePostStatus.DONE
                            Log.d(TAG, "Post created successfully with image")
                        } else {
                            _createPostStatus.value = CreatePostStatus.ERROR
                            Log.e(TAG, "Failed to create post with image: ${postResponse.code()}")
                        }
                    } else {
                        _createPostStatus.value = CreatePostStatus.ERROR
                        Log.e(TAG, "Failed to upload image: ${response.code()}")
                    }
                } catch (e: Exception) {
                    _createPostStatus.value = CreatePostStatus.ERROR
                    Log.e(TAG, "Error during image upload or post creation", e)
                }
            }
        } catch (e: Exception) {
            _createPostStatus.value = CreatePostStatus.ERROR
            Log.e(TAG, "Error loading image", e)
        }
    }

    private fun getRealPathFromUri(uri: Uri?, context: Context?): String {
        var filePath = ""
        if (context != null && uri != null) {
            var cursor: Cursor? = null
            try {
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting real path from URI", e)
            } finally {
                cursor?.close()
            }
        }
        return filePath
    }

    private fun checkButtonEnabled() {
        _buttonEnabled.value = postBody.text.isNotEmpty()
    }
}