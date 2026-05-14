package com.anthropic.claudemonitor.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.anthropic.claudemonitor.R
import com.anthropic.claudemonitor.data.models.Stats
import com.anthropic.claudemonitor.databinding.FragmentDashboardBinding
import com.anthropic.claudemonitor.util.formatCostShort
import com.anthropic.claudemonitor.util.formatDate
import com.anthropic.claudemonitor.util.formatTokens
import kotlin.math.roundToInt

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) renderStats(stats)
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            binding.errorCard.isVisible = err != null
            binding.errorText.text = err
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        viewModel.startPolling()
    }

    private fun renderStats(s: Stats) {
        // Billing period card
        val hasPeriod = s.billingStart != null
        binding.periodCard.isVisible = hasPeriod
        if (hasPeriod) {
            val pct = s.periodUsagePct
            val limit = s.planTokenLimit

            binding.periodProgressBar.progress = pct?.roundToInt() ?: 0
            binding.periodProgressBar.isVisible = pct != null
            binding.periodPctText.text = if (pct != null) "%.1f%%".format(pct) else "No limit set"
            binding.periodTokensText.text = if (limit != null) {
                "${s.periodTokens.formatTokens()} / ${limit.formatTokens()} tokens"
            } else {
                "${s.periodTokens.formatTokens()} tokens used"
            }
            binding.periodCostText.text = s.periodEstimatedCostUsd.formatCostShort()
            binding.periodDaysText.text = s.daysRemainingInPeriod?.let {
                "%.1f days left".format(it)
            } ?: "—"
            binding.periodStartText.text = "Period: ${s.billingStart.formatDate()} – ${s.billingEnd.formatDate()}"

            // Color progress bar based on usage
            val color = when {
                pct == null -> requireContext().getColor(R.color.token_blue)
                pct >= 90   -> requireContext().getColor(R.color.token_red)
                pct >= 75   -> requireContext().getColor(R.color.token_orange)
                else        -> requireContext().getColor(R.color.token_green)
            }
            binding.periodProgressBar.setIndicatorColor(color)
        }

        // Today card
        binding.todayTokensText.text  = s.todayTokens.formatTokens()
        binding.todayInputText.text   = "In: ${s.todayInputTokens.formatTokens()}"
        binding.todayOutputText.text  = "Out: ${s.todayOutputTokens.formatTokens()}"
        binding.todayCacheText.text   = "Cache: ${s.todayCacheReadTokens.formatTokens()}"
        binding.todayCostText.text    = s.todayEstimatedCostUsd.formatCostShort()

        // All-time card
        binding.totalTokensText.text  = s.totalTokens.formatTokens()
        binding.totalInputText.text   = "In: ${s.totalInputTokens.formatTokens()}"
        binding.totalOutputText.text  = "Out: ${s.totalOutputTokens.formatTokens()}"
        binding.totalCacheWriteText.text = "Cache wr: ${s.totalCacheWriteTokens.formatTokens()}"
        binding.totalCacheReadText.text  = "Cache rd: ${s.totalCacheReadTokens.formatTokens()}"
        binding.totalCostText.text    = s.totalEstimatedCostUsd.formatCostShort()
        binding.totalSessionsText.text = "${s.totalSessions} sessions · ${s.totalProjects} projects"
        binding.modelsText.text = s.modelsUsed.joinToString(", ")
            .ifEmpty { "—" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
