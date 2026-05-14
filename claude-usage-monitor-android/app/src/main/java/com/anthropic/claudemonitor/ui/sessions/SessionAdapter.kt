package com.anthropic.claudemonitor.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anthropic.claudemonitor.data.models.Session
import com.anthropic.claudemonitor.databinding.ItemSessionBinding
import com.anthropic.claudemonitor.util.formatCostShort
import com.anthropic.claudemonitor.util.formatDateTime
import com.anthropic.claudemonitor.util.formatTokens
import com.anthropic.claudemonitor.util.shortModel

class SessionAdapter : ListAdapter<Session, SessionAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemSessionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: Session) {
            b.projectText.text  = s.project ?: "unknown"
            b.branchText.text   = s.gitBranch ?: "—"
            b.modelText.text    = s.model.shortModel()
            b.timeText.text     = s.lastActivity.formatDateTime()
            b.tokensText.text   = s.totalTokens.formatTokens()
            b.inOutText.text    = "↑${s.inputTokens.formatTokens()} ↓${s.outputTokens.formatTokens()}"
            b.cacheText.text    = "cache: ${(s.cacheWriteTokens + s.cacheReadTokens).formatTokens()}"
            b.costText.text     = s.estimatedCostUsd.formatCostShort()
            b.msgsText.text     = "${s.userMessages + s.assistantMessages} msgs"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(a: Session, b: Session) = a.sessionId == b.sessionId
            override fun areContentsTheSame(a: Session, b: Session) = a == b
        }
    }
}
