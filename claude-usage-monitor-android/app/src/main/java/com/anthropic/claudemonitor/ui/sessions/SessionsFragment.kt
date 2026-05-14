package com.anthropic.claudemonitor.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.anthropic.claudemonitor.databinding.FragmentSessionsBinding

class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionsViewModel by viewModels()
    private val adapter = SessionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            adapter.submitList(sessions)
            binding.emptyText.isVisible = sessions.isEmpty()
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            binding.errorText.isVisible = err != null
            binding.errorText.text = err
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
