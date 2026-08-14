package com.cse489.landmarks.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cse489.landmarks.R
import com.cse489.landmarks.data.api.LandmarkApiService
import com.cse489.landmarks.data.local.LandmarkEntity
import com.cse489.landmarks.databinding.ItemLandmarkBinding
import java.util.Locale

class LandmarksAdapter(
    private val onVisitClick: (LandmarkEntity) -> Unit,
    private val onDeleteClick: (LandmarkEntity) -> Unit
) : ListAdapter<LandmarkEntity, LandmarksAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemLandmarkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLandmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val landmark = getItem(position)
        val binding = holder.binding

        binding.txtTitle.text = landmark.title
        binding.txtScore.text = String.format(Locale.getDefault(), "Score: %.2f", landmark.score)

        val distText = if (landmark.avgDistance >= 1000) {
            String.format(Locale.getDefault(), "%.2f km", landmark.avgDistance / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.2f m", landmark.avgDistance)
        }
        binding.txtStats.text = String.format(Locale.getDefault(), "Visits: %d | Avg Dist: %s", landmark.visitCount, distText)

        if (!landmark.image.isNullOrEmpty()) {
            val fullImageUrl = if (landmark.image.startsWith("http")) {
                landmark.image
            } else {
                LandmarkApiService.BASE_URL + landmark.image
            }
            Glide.with(binding.imgLandmark.context)
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_landmarks)
                .error(R.drawable.ic_landmarks)
                .into(binding.imgLandmark)
        } else {
            binding.imgLandmark.setImageResource(R.drawable.ic_landmarks)
        }

        binding.btnVisit.setOnClickListener { onVisitClick(landmark) }
        binding.btnDelete.setOnClickListener { onDeleteClick(landmark) }
    }

    object DiffCallback : DiffUtil.ItemCallback<LandmarkEntity>() {
        override fun areItemsTheSame(oldItem: LandmarkEntity, newItem: LandmarkEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LandmarkEntity, newItem: LandmarkEntity): Boolean {
            return oldItem == newItem
        }
    }
}
