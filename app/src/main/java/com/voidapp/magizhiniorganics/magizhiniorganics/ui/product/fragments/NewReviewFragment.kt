package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.hsalf.smileyrating.SmileyRating
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentNewReviewBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class NewReviewFragment : Fragment(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val factory: ProductViewModelFactory by instance()
    private lateinit var productViewModel: ProductViewModel
    private var _binding: FragmentNewReviewBinding? = null
    private val binding get() = _binding!!

    private var mProduct: ProductEntity = ProductEntity()
    private val mPurchasedProductIds = arrayListOf<String>()
    private var mProductId: String = ""
    private var mRating: Int = 5

    private var mUserName: String = ""
    private var mProfilePicUrl: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNewReviewBinding.inflate(inflater, container, false)
        productViewModel =
            ViewModelProvider(requireActivity(), factory).get(ProductViewModel::class.java)
        binding.viewmodel = productViewModel

        initLiveData()
        clickListeners()

        return binding.root
    }

    private fun initLiveData() {
        mProductId = productViewModel.mProducts

        productViewModel.name.observe(viewLifecycleOwner, {
            mUserName = it
        })
        productViewModel.profilePicUrl.observe(viewLifecycleOwner, {
            mProfilePicUrl = it
        })
        productViewModel.getProductById(mProductId).observe(viewLifecycleOwner, {
            mProduct = it
        })
        productViewModel.orderHistory.observe(viewLifecycleOwner, { orders ->
            mPurchasedProductIds.clear()
            mPurchasedProductIds.addAll(orders)
            initItems()
            Log.e("qqqq", orders.toString())
        })
    }

    private fun initItems() {
        if (mPurchasedProductIds.contains(mProductId)) {
            giveAccessToReview()
        } else {
            initViews()
        }
    }

    private fun clickListeners() {
        with(binding) {

            srSmileyRating.setSmileySelectedListener { type ->
                mRating = when (type) {
                    SmileyRating.Type.TERRIBLE -> 1
                    SmileyRating.Type.BAD -> 2
                    SmileyRating.Type.OKAY -> 3
                    SmileyRating.Type.GOOD -> 4
                    SmileyRating.Type.GREAT -> 5
                    else -> 5
                }
            }

            btnSaveReview.setOnClickListener {
                val reviewContent: String = getReviewContent()
                val review = Review(
                    mUserName,
                    mProfilePicUrl,
                    System.currentTimeMillis(),
                    mRating,
                    reviewContent
                )
                mProduct.reviews.add(review)
                productViewModel.upsertProductReview(mProductId, review, mProduct)
                Toast.makeText(requireContext(), "Thanks for the review :)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getReviewContent(): String {
        return if (binding.edtDescription.text.isNullOrEmpty()) {
            ""
        } else {
            binding.edtDescription.text.toString().trim()
        }
    }

    private fun giveAccessToReview() {
        with(binding) {
            tvAddCommentWarning.visibility = View.GONE
            tvAddCommentContent.visibility = View.GONE
            srSmileyRating.visibility = View.VISIBLE
            etlDescription.visibility = View.VISIBLE
            btnSaveReview.visibility = View.VISIBLE
        }
    }

    private fun initViews() {
        with(binding) {
            tvAddCommentWarning.visibility = View.VISIBLE
            tvAddCommentContent.visibility = View.VISIBLE
            srSmileyRating.visibility = View.GONE
            etlDescription.visibility = View.GONE
            btnSaveReview.visibility = View.GONE
        }
    }

}