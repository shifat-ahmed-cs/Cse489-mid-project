package com.cse489.landmarks.ui.list

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cse489.landmarks.databinding.FragmentLandmarksBinding
import com.cse489.landmarks.ui.viewmodel.LandmarkViewModel
import com.cse489.landmarks.ui.viewmodel.SortOption
import com.cse489.landmarks.utils.LocationUtils
import kotlinx.coroutines.launch
import java.util.Locale

class LandmarksFragment : Fragment() {

    private var _binding: FragmentLandmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LandmarkViewModel by activityViewModels()
    private lateinit var adapter: LandmarksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilters()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = LandmarksAdapter(
            onVisitClick = { landmark ->
                lifecycleScope.launch {
                    val loc = LocationUtils.getCurrentLocation(requireContext())
                    viewModel.visitLandmark(landmark.id, landmark.title, loc.latitude, loc.longitude)
                }
            },
            onDeleteClick = { landmark ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Landmark")
                    .setMessage("Are you sure you want to soft-delete '${landmark.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteLandmark(landmark.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        binding.recyclerViewLandmarks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLandmarks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshLandmarks()
        }
    }

    private fun setupSearchAndFilters() {
        // Search text watcher
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchQuery.value = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sort spinner
        val sortLabels = arrayOf("Score (Highest First)", "Score (Lowest First)", "Title (A-Z)")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = spinnerAdapter

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.sortOption.value = SortOption.SCORE_DESC
                    1 -> viewModel.sortOption.value = SortOption.SCORE_ASC
                    2 -> viewModel.sortOption.value = SortOption.TITLE_ASC
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Min Score Slider
        binding.sliderMinScore.addOnChangeListener { _, value, _ ->
            if (value <= -999900) {
                viewModel.minScoreFilter.value = null
                binding.txtMinScoreLabel.text = "Min Score: All"
            } else {
                viewModel.minScoreFilter.value = value.toDouble()
                binding.txtMinScoreLabel.text = String.format(Locale.getDefault(), "Min Score: %.0f", value)
            }
        }

        binding.btnResetFilter.setOnClickListener {
            binding.etSearch.setText("")
            binding.spinnerSort.setSelection(0)
            binding.sliderMinScore.value = -1000000f
            viewModel.minScoreFilter.value = null
            binding.txtMinScoreLabel.text = "Min Score: All"
        }
    }

    private fun observeViewModel() {
        viewModel.filteredLandmarks.observe(viewLifecycleOwner) { landmarks ->
            adapter.submitList(landmarks)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
