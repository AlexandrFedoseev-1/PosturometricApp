package com.example.posturometricapp.ui.sessionDetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.posturometricapp.convertToStUnit
import com.example.posturometricapp.databinding.ItemTopSensorBinding

class TopSensorsAdapter(
    private val onClick: (Int) -> Unit
) : ListAdapter<SessionDetailsViewModel.TopSensor, TopSensorsAdapter.VH>(DIFF) {


    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SessionDetailsViewModel.TopSensor>() {
            override fun areItemsTheSame(
                old: SessionDetailsViewModel.TopSensor,
                new: SessionDetailsViewModel.TopSensor
            ) = old.id == new.id

            override fun areContentsTheSame(
                old: SessionDetailsViewModel.TopSensor,
                new: SessionDetailsViewModel.TopSensor
            ) = old == new
        }
    }

    inner class VH(private val binding: ItemTopSensorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SessionDetailsViewModel.TopSensor) {
            binding.tvSensorId.text = "№${item.id + 1}"
            binding.tvSensorMax.text = "Max: ${convertToStUnit(item.maxValue)}"
            binding.root.setOnClickListener { onClick(item.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTopSensorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))
}