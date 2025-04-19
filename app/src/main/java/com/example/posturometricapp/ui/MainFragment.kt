package com.example.posturometricapp.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.posturometricapp.R
import com.example.posturometricapp.databinding.FragmentMainBinding
import com.google.android.material.tabs.TabLayoutMediator


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPagerAdapter: MainViewPagerAdapter
    private lateinit var tabMediator : TabLayoutMediator
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPagerAdapter = MainViewPagerAdapter(childFragmentManager,lifecycle)
        binding.viewPager.adapter = viewPagerAdapter
        tabMediator = TabLayoutMediator(binding.tabLayout,binding.viewPager){
            tab, position ->
            when (position){
                0 -> tab.text = "Сессия"
                1 -> tab.text = "История"
            }
        }
        tabMediator.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabMediator.detach()
    }
    companion object {

    }
}