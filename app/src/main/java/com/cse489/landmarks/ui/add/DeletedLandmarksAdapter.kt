package com.cse489.landmarks.ui.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cse489.landmarks.data.local.LandmarkEntity
import com.cse489.landmarks.databinding.ItemDeletedLandmarkBinding
import java.util.Locale

class DeletedLandmarksAdapter(
    private val onRestoreClick: (LandmarkEntity) -> Unit
) : ListAdapter<LandmarkEntity, DeletedLandmarksAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemDeletedLandmarkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeletedLandmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.txtTitle.text = item.title
        binding.txtCoords.text = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f | Score: %.2f", item.lat, item.lon, item.score)

        binding.btnRestore.setOnClickListener { onRestoreClick(item) }
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
