package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentNewReviewBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.compressImageToNewFile
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.imageExtension
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class CWMNewReviewsFragment: Fragment(), KodeinAware {

    override val kodein: Kodein by kodein()

    private var _binding: FragmentNewReviewBinding? = null
    private val binding get() = _binding!!

    private val factory: DishViewModelFactory by instance()
    private lateinit var dishViewModel: DishViewModel

    private var mRatingImageUri: Uri? = null
    private var imageExtension: String = ""

    private val picker = registerForActivityResult((ActivityResultContracts.GetContent())) {
        it?.let{ uri ->
            mRatingImageUri = uri
            binding.ivReviewImage.visibility = View.VISIBLE
            binding.ivReviewImage.loadImg(uri) {}
            binding.ivReviewImage.scaleType = ImageView.ScaleType.CENTER_CROP
            imageExtension = imageExtension(requireActivity(),  mRatingImageUri)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNewReviewBinding.inflate(inflater, container, false)
        dishViewModel = ViewModelProvider(requireActivity(), factory).get(
            DishViewModel::class.java)

        clickListeners()
        initLiveData()

        return binding.root
    }

    private fun initLiveData() {
        dishViewModel.previewImage.observe(viewLifecycleOwner) {
            when (it) {
                "granted" -> picker.launch("image/*")
                "added" -> {
                    binding.ivReviewImage.visibility = View.GONE
                    binding.edtDescription.setText("")
                    mRatingImageUri = null
                }
            }
        }
    }

    private fun clickListeners() {
        with(binding) {
            btnAddImage.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                lifecycleScope.launch {
                    delay(150)
                    dishViewModel.setStoragePermission(true)
                }
            }

            ivReviewImage.setOnClickListener {
                picker.launch("image/*")
            }

            btnSaveReview.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                if (!NetworkHelper.isOnline(requireContext())) {
                    Toast.makeText(requireContext(), "Please check your Internet Connection", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dishViewModel.userProfile?.let { profile ->
                    Review(
                        "",
                        profile.name,
                        profile.profilePicUrl,
                        System.currentTimeMillis(),
                        srSmileyRating.count,
                        getReviewContent()
                    ).also { review ->
                        mRatingImageUri?.let { uri ->
                            compressImageToNewFile(requireContext(), uri)?.let { file ->
                                dishViewModel.tempFile = file
                                dishViewModel.upsertProductReview(
                                    review,
                                    file.toUri(),
                                    ".jpg"
                                )
                            }
                        } ?: let {
                            dishViewModel.upsertProductReview(
                                review,
                                null,
                                ""
                            )
                        }
                    }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}