package com.cse489.landmarks.ui.add

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cse489.landmarks.databinding.FragmentAddViewBinding
import com.cse489.landmarks.ui.viewmodel.LandmarkViewModel
import com.cse489.landmarks.utils.LocationUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AddViewFragment : Fragment() {

    private var _binding: FragmentAddViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LandmarkViewModel by activityViewModels()
    private lateinit var deletedAdapter: DeletedLandmarksAdapter

    private var selectedImageFile: File? = null

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val file = uriToFile(it)
                if (file != null) {
                    selectedImageFile = file
                    binding.imgPreview.setImageURI(it)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAddForm()
        setupDeletedList()
    }

    private fun setupAddForm() {
        binding.btnFetchLocation.setOnClickListener {
            lifecycleScope.launch {
                val location = LocationUtils.getCurrentLocation(requireContext())
                binding.etLat.setText(location.latitude.toString())
                binding.etLon.setText(location.longitude.toString())
                Toast.makeText(requireContext(), "GPS location updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnCreateLandmark.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim() ?: ""
            val latStr = binding.etLat.text?.toString()?.trim() ?: ""
            val lonStr = binding.etLon.text?.toString()?.trim() ?: ""

            if (title.isEmpty()) {
                binding.etTitle.error = "Title is required"
                return@setOnClickListener
            }

            val lat = latStr.toDoubleOrNull()
            if (lat == null) {
                binding.etLat.error = "Valid latitude required"
                return@setOnClickListener
            }

            val lon = lonStr.toDoubleOrNull()
            if (lon == null) {
                binding.etLon.error = "Valid longitude required"
                return@setOnClickListener
            }

            viewModel.createLandmark(title, lat, lon, selectedImageFile)

            // Reset Form
            binding.etTitle.setText("")
            binding.etLat.setText("")
            binding.etLon.setText("")
            selectedImageFile = null
            binding.imgPreview.setImageResource(com.cse489.landmarks.R.drawable.ic_add_view)
        }
    }

    private fun setupDeletedList() {
        deletedAdapter = DeletedLandmarksAdapter { landmark ->
            viewModel.restoreLandmark(landmark.id)
        }

        binding.recyclerViewDeleted.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDeleted.adapter = deletedAdapter

        viewModel.deletedLandmarks.observe(viewLifecycleOwner) { deletedList ->
            if (deletedList.isNullOrEmpty()) {
                binding.txtNoDeleted.visibility = View.VISIBLE
                binding.recyclerViewDeleted.visibility = View.GONE
            } else {
                binding.txtNoDeleted.visibility = View.GONE
                binding.recyclerViewDeleted.visibility = View.VISIBLE
                deletedAdapter.submitList(deletedList)
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
