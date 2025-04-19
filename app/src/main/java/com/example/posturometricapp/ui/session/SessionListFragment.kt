package com.example.posturometricapp.ui.session

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.posturometricapp.databinding.FragmentSessionListBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


class SessionListFragment : Fragment() {

    private val viewModel by viewModel<SessionListViewModel>()
    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!
    private val adapter by lazy {SessionAdapter{ sesion ->
        Toast.makeText(requireContext(),"$sesion",Toast.LENGTH_LONG).show()
    }  }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvSessions.adapter = adapter

        viewModel.sessions.observe(viewLifecycleOwner){ sessions ->
            adapter.submitList(sessions)
        }
    }

    companion object {
        fun newInstance() = SessionListFragment()
    }
}