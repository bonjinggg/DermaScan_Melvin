package com.example.dermascanai

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.example.dermascanai.databinding.FragmentDermaHomeBinding
import com.example.dermascanai.databinding.FragmentHomeUserBinding
import com.google.android.material.navigation.NavigationView


class DermaHomeFragment : Fragment() {
    private var _binding: FragmentDermaHomeBinding? = null
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDermaHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




    }


}