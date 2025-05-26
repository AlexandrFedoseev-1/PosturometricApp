package com.example.posturometricapp.ui.session

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.posturometricapp.R
import com.example.posturometricapp.databinding.FragmentSessionListBinding
import com.example.posturometricapp.domain.model.Session
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel


class SessionListFragment : Fragment() {

    private val viewModel by viewModel<SessionListViewModel>()
    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!
    private val adapter by lazy {
        SessionAdapter(
            onClick = { session ->
                val action = SessionListFragmentDirections
                    .actionGlobalSessionDetailsFragment(session)
                findNavController().navigate(action)
            },
            onLongTrackClick = { session ->
                showDeleteSessionDialog(session)
            }
        )
    }

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

        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            adapter.submitList(sessions)
        }

    }

    private fun showDeleteSessionDialog(session: Session) {
        val title = "Удалить сессию?"
        val message = "Удалить выбранную сеесию?"
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Нет") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Да") { _, _ ->
                viewModel.deleteSession(session)
            }
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
    }


    companion object {
        fun newInstance() = SessionListFragment()
    }
}