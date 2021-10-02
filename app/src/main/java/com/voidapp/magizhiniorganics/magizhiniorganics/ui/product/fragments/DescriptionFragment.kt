package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentDescriptionBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class DescriptionFragment : Fragment(), KodeinAware {

    override val kodein: Kodein by kodein()

    private var _binding: FragmentDescriptionBinding? = null
    private val binding get() = _binding!!

    private val factory: ProductViewModelFactory by instance()
    private lateinit var productViewModel: ProductViewModel
    private var mProductId = ""
    private var mProduct = ProductEntity()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDescriptionBinding.inflate(inflater, container, false)
        productViewModel = ViewModelProvider(requireActivity(), factory).get(ProductViewModel::class.java)
        binding.viewmodel = productViewModel

        mProductId = productViewModel.mProducts

        productViewModel.getProductById(mProductId).observe(viewLifecycleOwner, { product ->
            mProduct = product
              if (mProduct.description.isEmpty()) {
                  binding.tvDescription.text = "No Description provided"
              } else {
                  binding.tvDescription.text = mProduct.description
              }
        })

        binding.tvDescription.setOnClickListener {
            when(mProduct.descType) {
                Constants.DO_NOTHING -> {}
                Constants.OPEN_LINK -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        intent.data = Uri.parse(mProduct.description)
                        startActivity(Intent.createChooser(intent, "Open link with"))
                    } catch (e: Exception) {
                        println("The current phone does not have a browser installed")
                    }
                }
                Constants.SHOW_DETAILS -> {
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}