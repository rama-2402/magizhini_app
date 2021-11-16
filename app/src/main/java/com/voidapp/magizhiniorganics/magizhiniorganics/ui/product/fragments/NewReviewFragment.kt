package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hsalf.smileyrating.SmileyRating
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentNewReviewBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.PermissionsUtil
import kotlinx.coroutines.*
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

    private var mRating: Int = 5
    private var mRatingImageUri: Uri? = null
    private var imageExtension: String = ""

    private val picker = registerForActivityResult((ActivityResultContracts.GetContent())) {
        it?.let{ uri ->
            mRatingImageUri = uri
            binding.ivReviewImage.visibility = View.VISIBLE
            GlideLoader().loadUserPicture(
                requireContext(),
                uri,
                binding.ivReviewImage
            )
            binding.ivReviewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            imageExtension = GlideLoader().imageExtension(requireActivity(),  mRatingImageUri)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNewReviewBinding.inflate(inflater, container, false)
        productViewModel =
            ViewModelProvider(requireActivity(), factory).get(ProductViewModel::class.java)
        binding.viewmodel = productViewModel

        clickListeners()
        initLiveData()

        return binding.root
    }

    private fun initLiveData() {
        productViewModel.previewImage.observe(viewLifecycleOwner, {
            when(it) {
                "granted" -> picker.launch("image/*")
                "added" -> {
                    binding.ivReviewImage.visibility = View.GONE
                    binding.edtDescription.setText("")
                }
            }
        })
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

            btnAddImage.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                lifecycleScope.launch {
                    delay(150)
                    productViewModel.checkStoragePermission()
                }
            }

            ivReviewImage.setOnClickListener {
                picker.launch("image/*")
            }

            btnSaveReview.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                Review(
                    "",
                    productViewModel.userProfile.name,
                    productViewModel.userProfile.profilePicUrl,
                    System.currentTimeMillis(),
                    mRating,
                    getReviewContent()
                ).also { review ->
                    productViewModel.upsertProductReview(
                        review,
                        productViewModel.product.id,
                        mRatingImageUri,
                        mRatingImageUri?.let { GlideLoader().imageExtension(requireActivity(),  mRatingImageUri)!! } ?: ""
                    )
//                    lifecycleScope.launch {
//                        if (productViewModel.upsertProductReview(review, productViewModel.product, mRatingImageUri, imageExtension)) {
//                            clearData()
//                        }
//                    }
                }
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
}