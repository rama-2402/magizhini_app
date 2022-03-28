package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentReviewsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class ReviewsFragment : Fragment(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val factory: ProductViewModelFactory by instance()
    private lateinit var productViewModel: ProductViewModel
    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

//    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        productViewModel =
            ViewModelProvider(requireActivity(), factory).get(ProductViewModel::class.java)
        binding.viewmodel = productViewModel

        initRecyclerView()
        initData()
        initLiveData()

        return binding.root
    }

    private fun initData() {
        if (!NetworkHelper.isOnline(requireContext())) {
            Toast.makeText(requireContext(), "Please check your Internet Connection", Toast.LENGTH_SHORT).show()
            return
        }
        productViewModel.getProductReviews()
    }

    private fun initRecyclerView() {
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.adapter = productViewModel.reviewAdapter
    }

    private fun initLiveData() {
        productViewModel.reviews.observe(viewLifecycleOwner) {
//            adapter.reviews = it
//            adapter.notifyDataSetChanged()
            it?.let {
                binding.apply {
                    llEmptyLayout.visibility = View.GONE
                    productViewModel.reviewAdapter?.let{ adapter ->
                        adapter.reviews = it
                        adapter.notifyDataSetChanged()
                    }
                }
            } ?: binding.apply {
                llEmptyLayout.fadInAnimation(200)
            }
        }
    }

}