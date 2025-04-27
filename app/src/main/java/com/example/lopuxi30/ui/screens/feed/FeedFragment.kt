package com.example.lopuxi30.ui.screens.feed

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import com.example.lopuxi30.R
import com.example.lopuxi30.databinding.FragmentFeedBinding
import com.example.lopuxi30.ui.stateholders.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private lateinit var binding: FragmentFeedBinding

    private val viewModel: FeedViewModel by viewModels()

    private var viewController: FeedFragmentViewController? = null

    // Флаг для отслеживания состояния видимости фильтров
    private var isFilterVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)

        viewModel.token = getToken()

        viewController = FeedFragmentViewController(
            binding,
            this,
            viewModel
        ).apply {
            setupViews()
        }

        // Настройка кнопки переключения видимости фильтров
        setupFilterToggle()

        return binding.root
    }

    override fun onDestroyView() {
        viewController?.clear()
        viewController = null
        super.onDestroyView()
    }

    private fun getToken(): String {
        val sharedPreferences: SharedPreferences by lazy {
            requireContext().getSharedPreferences(
                requireContext().getString(R.string.SharedPrefs),
                Context.MODE_PRIVATE
            )
        }
        return sharedPreferences.getString(
            requireContext().getString(R.string.SharedPrefs_Token),
            ""
        )!!
    }

    /**
     * Настройка кнопки переключения видимости фильтров.
     */
    private fun setupFilterToggle() {
        binding.toggleFilterButton.setOnClickListener {
            isFilterVisible = !isFilterVisible

            // Добавляем анимацию
            TransitionManager.beginDelayedTransition(binding.filterCard)

            val visibility = if (isFilterVisible) View.VISIBLE else View.GONE
            listOf(
                binding.usernameFilterLayout,
                binding.startDateLayout,
                binding.endDateLayout,
                binding.applyFilterButton
            ).forEach { it.visibility = visibility }

            binding.toggleFilterButton.text = if (isFilterVisible) "Скрыть фильтры" else "Раскрыть фильтры"
        }
    }
}