package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentDescriptionBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentReviewsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class SubReviewsFragment: Fragment(), KodeinAware, ReviewAdapter.ReviewItemClickListener {

    override val kodein: Kodein by kodein()

    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var productViewModel: SubscriptionProductViewModel

    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        productViewModel = ViewModelProvider(requireActivity(), factory).get(
            SubscriptionProductViewModel::class.java)

        initRecyclerView()
        initData()
        initLiveData()

        return binding.root
    }

    private fun initData() {
        productViewModel.getProductReviews()
    }

    override fun previewImage(url: String) {
        productViewModel.openPreview(url, "preview")
    }

    private fun initRecyclerView() {
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())

        adapter = ReviewAdapter(
            requireContext(),
            arrayListOf(),
            this
        )
        binding.rvReviews.adapter = adapter
    }

    private fun initLiveData() {
        productViewModel.reviews.observe(viewLifecycleOwner) {
//            adapter.reviews = it
//            adapter.notifyDataSetChanged()
            it?.let {
                binding.apply {
                    llEmptyLayout.visibility = View.GONE
                    adapter.reviews = it
                    adapter.notifyDataSetChanged()
                }
            } ?: binding.apply {
                llEmptyLayout.fadInAnimation(200)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}