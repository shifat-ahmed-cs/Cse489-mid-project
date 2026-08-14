package com.cse489.landmarks.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.cse489.landmarks.databinding.FragmentActivityBinding
import com.cse489.landmarks.ui.viewmodel.LandmarkViewModel

class ActivityFragment : Fragment() {

    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LandmarkViewModel by activityViewModels()
    private lateinit var adapter: ActivityAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActivityAdapter()
        binding.recyclerViewActivity.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewActivity.adapter = adapter

        viewModel.visitHistory.observe(viewLifecycleOwner) { history ->
            if (history.isNullOrEmpty()) {
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.recyclerViewActivity.visibility = View.GONE
            } else {
                binding.txtEmptyState.visibility = View.GONE
                binding.recyclerViewActivity.visibility = View.VISIBLE
                adapter.submitList(history)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
