package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentDescriptionBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class SubDescriptionFragment: Fragment(), KodeinAware {

    override val kodein: Kodein by kodein()

    private var _binding: FragmentDescriptionBinding? = null
    private val binding get() = _binding!!

    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var productViewModel: SubscriptionProductViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDescriptionBinding.inflate(inflater, container, false)
        productViewModel = ViewModelProvider(requireActivity(), factory).get(
            SubscriptionProductViewModel::class.java)

        productViewModel.description.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.tvDescription.text = "No Description provided"
            } else {
                binding.tvDescription.text = it
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}