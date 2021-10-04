package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class SubscriptionProductActivity : BaseActivity(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var viewModel: SubscriptionProductViewModel
    private lateinit var binding: ActivitySubscriptionProductBinding

    private var mProduct = ProductEntity()
    private var mProductId: String = ""
    private var mProductName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscription_product)
        viewModel = ViewModelProvider(this, factory).get(SubscriptionProductViewModel::class.java)
        binding.viewmodel = viewModel

        mProductId = intent.getStringExtra(Constants.PRODUCTS).toString()
        mProductName = intent.getStringExtra(Constants.PRODUCT_NAME).toString()
        viewModel.mProductID = mProductId

        setSupportActionBar(binding.tbCollapsedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.tvProductName.text = mProductName
        binding.tvProductName.isSelected = true

        initData()
    }

    private fun initData() {

    }
}