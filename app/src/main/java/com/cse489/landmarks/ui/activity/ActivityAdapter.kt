package com.cse489.landmarks.ui.activity

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cse489.landmarks.R
import com.cse489.landmarks.data.local.VisitHistoryEntity
import com.cse489.landmarks.databinding.ItemVisitHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class ActivityAdapter : ListAdapter<VisitHistoryEntity, ActivityAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    class ViewHolder(val binding: ItemVisitHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVisitHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.txtLandmarkName.text = item.landmarkTitle
        binding.txtVisitTime.text = "Visited: ${dateFormat.format(Date(item.visitTime))}"

        when (item.status.lowercase()) {
            "done" -> {
                binding.txtStatusBadge.text = "DONE"
                binding.txtStatusBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_done))
                binding.txtStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_done_text))
                val dist = item.distance ?: 0.0
                val distStr = if (dist >= 1000) {
                    String.format(Locale.getDefault(), "Distance: %.2f km", dist / 1000.0)
                } else {
                    String.format(Locale.getDefault(), "Distance: %.2f meters", dist)
                }
                binding.txtDistance.text = distStr
                binding.txtDistance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.primary))
            }
            "pending" -> {
                binding.txtStatusBadge.text = "PENDING"
                binding.txtStatusBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_pending))
                binding.txtStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_pending_text))
                binding.txtDistance.text = "Distance: Polling background worker..."
                binding.txtDistance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_pending_text))
            }
            "queued_offline" -> {
                binding.txtStatusBadge.text = "QUEUED (OFFLINE)"
                binding.txtStatusBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_offline))
                binding.txtStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_offline_text))
                binding.txtDistance.text = "Queued offline: Will sync when connected"
                binding.txtDistance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_offline_text))
            }
            else -> {
                binding.txtStatusBadge.text = "FAILED"
                binding.txtStatusBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_failed))
                binding.txtStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_failed_text))
                binding.txtDistance.text = "Distance calculation failed"
                binding.txtDistance.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_failed_text))
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<VisitHistoryEntity>() {
        override fun areItemsTheSame(oldItem: VisitHistoryEntity, newItem: VisitHistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VisitHistoryEntity, newItem: VisitHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
