package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentReviewsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class CWMReviewsFragment:
    Fragment(),
    KodeinAware,
    ReviewAdapter.ReviewItemClickListener
{

    override val kodein: Kodein by kodein()

    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private val factory: DishViewModelFactory by instance()
    private lateinit var dishViewModel: DishViewModel

    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        dishViewModel = ViewModelProvider(requireActivity(), factory).get(
            DishViewModel::class.java)

        initRecyclerView()
        initData()
        initLiveData()

        return binding.root
    }

    private fun initData() {
        dishViewModel.getProductReviews()
    }

    override fun previewImage(url: String, thumbnail: ShapeableImageView) {
        dishViewModel.openPreview(url, "preview")
    }

//    override fun previewImage(url: String) {
//        dishViewModel.openPreview(url, "preview")
//    }

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
        dishViewModel.reviews.observe(viewLifecycleOwner) {
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