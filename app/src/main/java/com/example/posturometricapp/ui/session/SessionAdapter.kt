package com.example.posturometricapp.ui.session

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.posturometricapp.R
import com.example.posturometricapp.databinding.SessionListItemBinding
import com.example.posturometricapp.domain.model.Session
import com.example.posturometricapp.formatTimestampDate
import com.example.posturometricapp.formatTimestampTime

class SessionAdapter(
    private val onClick: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(old: Session, new: Session) = old.id == new.id
            override fun areContentsTheSame(old: Session, new: Session) = old == new
        }
    }

    inner class ViewHolder(private val binding: SessionListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Session) {
            binding.date.text = "Дата:" + formatTimestampDate(item.startTime)
            binding.start.text = "Начало:" + formatTimestampTime(item.startTime)
            binding.end.text = "Конец:" + (item.endTime?.let { formatTimestampTime(it) } ?: "–")
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SessionListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
