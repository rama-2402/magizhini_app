package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hsalf.smileyrating.helper.SmileyActiveIndicator
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentReviewsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Animations
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class ReviewsFragment : Fragment(), KodeinAware, ReviewAdapter.ReviewItemClickListener {

    override val kodein: Kodein by kodein()
    private val factory: ProductViewModelFactory by instance()
    private lateinit var productViewModel: ProductViewModel
    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private var mProductId: String = ""
    private var mReviews: ArrayList<Review> = arrayListOf()

    private lateinit var adapter: ReviewAdapter

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
        initLiveData()

        return binding.root
    }

    override fun previewImage(url: String) {
//        productViewModel.reviewImage.value = url
//        GlideLoader().loadUserPicture(binding.iv.context, url, binding.ivReviewImage)
//        binding.ivPreviewImage.startAnimation(Animations.scaleBig)
//        binding.ivPreviewImage.visible()
//        isPreviewVisible = true
    }

    private fun initRecyclerView() {
        val smileyActiveIndicator = SmileyActiveIndicator()
//        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())

        adapter = ReviewAdapter(
            requireContext(),
            arrayListOf(),
            this
        )
        binding.rvReviews.adapter = adapter
    }

    private fun initLiveData() {
        mProductId = productViewModel.mProducts
        productViewModel.getProductById(mProductId).observe(viewLifecycleOwner, {
            mReviews.clear()
            mReviews.addAll(it.reviews)
            mReviews.sortBy { review ->
                review.timeStamp
            }
            adapter.reviews = mReviews
            adapter.notifyDataSetChanged()
        })
    }

}