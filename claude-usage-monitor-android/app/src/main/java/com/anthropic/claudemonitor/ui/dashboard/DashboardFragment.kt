package com.anthropic.claudemonitor.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.anthropic.claudemonitor.R
import com.anthropic.claudemonitor.data.models.UsageLimits
import com.anthropic.claudemonitor.databinding.FragmentDashboardBinding
import com.anthropic.claudemonitor.ui.login.LoginActivity
import com.anthropic.claudemonitor.util.formatDate
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

        viewModel.usage.observe(viewLifecycleOwner) { usage ->
            if (usage != null) renderUsage(usage)
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            binding.errorCard.isVisible = err != null
            binding.errorText.text = err
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }
        viewModel.sessionExpired.observe(viewLifecycleOwner) { expired ->
            if (expired) redirectToLogin()
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        viewModel.startPolling()
    }

    private fun renderUsage(u: UsageLimits) {
        val used  = u.used  ?: 0L
        val limit = u.limit
        val pct   = if (limit != null && limit > 0) used.toDouble() / limit * 100.0 else null

        binding.periodCard.isVisible = true
        binding.periodProgressBar.isVisible = pct != null
        binding.periodProgressBar.progress = pct?.roundToInt() ?: 0
        binding.periodPctText.text = if (pct != null) "%.1f%%".format(pct) else "—"
        binding.periodTokensText.text = if (limit != null) {
            "${fmtNum(used)} / ${fmtNum(limit)} messages"
        } else {
            "${fmtNum(used)} messages used"
        }
        binding.periodStartText.text = u.resetTimestamp?.let { "Resets: ${it.formatDate()}" } ?: ""
        binding.periodCostText.text  = ""
        binding.periodDaysText.text  = ""

        val color = when {
            pct == null -> requireContext().getColor(R.color.token_blue)
            pct >= 90   -> requireContext().getColor(R.color.token_red)
            pct >= 75   -> requireContext().getColor(R.color.token_orange)
            else        -> requireContext().getColor(R.color.token_green)
        }
        binding.periodProgressBar.setIndicatorColor(color)

        // Account section
        binding.todayCostText.text   = viewModel.userEmail ?: ""
        binding.todayTokensText.text = u.type ?: "Max"
        binding.todayInputText.text  = ""
        binding.todayOutputText.text = ""
        binding.todayCacheText.text  = ""

        // All-time section shows plan limit
        binding.totalTokensText.text     = if (limit != null) "${fmtNum(limit)} msg limit" else "—"
        binding.totalInputText.text      = ""
        binding.totalOutputText.text     = ""
        binding.totalCacheWriteText.text = ""
        binding.totalCacheReadText.text  = ""
        binding.totalCostText.text       = ""
        binding.totalSessionsText.text   = ""
        binding.modelsText.text          = ""
    }

    private fun fmtNum(n: Long): String = when {
        n >= 1_000_000L -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000L     -> "%.1fK".format(n / 1_000.0)
        else            -> n.toString()
    }

    private fun redirectToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
