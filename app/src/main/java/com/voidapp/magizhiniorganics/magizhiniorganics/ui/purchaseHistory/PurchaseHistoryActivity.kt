package com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.PurchaseHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityPurchaseHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.dialogs.ItemsBottomSheet
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class PurchaseHistoryActivity : BaseActivity(), KodeinAware {
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityPurchaseHistoryBinding
    private val factory: PurchaseHistoryViewModelFactory by instance()
    private lateinit var viewModel: PurchaseHistoryViewModel

    private lateinit var ordersAdapter: PurchaseHistoryAdapter
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    private var mOrderHistory: MutableList<OrderEntity> = mutableListOf()
    private lateinit var mCartBottomSheetDialog: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_purchase_history)
        viewModel = ViewModelProvider(this, factory).get(PurchaseHistoryViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)
        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }
        initRecyclerView()
        initLiveData()
    }

    private fun initLiveData() {
        viewModel.getFavorites()
        viewModel.getAllPurchaseHistory().observe(this, { orderEntities ->
            mOrderHistory.clear()
            mOrderHistory.addAll(orderEntities)
            mOrderHistory.sortByDescending {
                it.purchaseDate
            }
            ordersAdapter.orders = mOrderHistory
            ordersAdapter.notifyDataSetChanged()
        })

        viewModel.showCartBottomSheet.observe(this, {
            orderItemsAdapter.cartItems = it
            showCartBottomSheet()
        })

        //observing the favorites livedata
        viewModel.favorites.observe(this, {
            orderItemsAdapter.favorites = it
        })

        viewModel.moveToProductReview.observe(this, {
            moveToProductDetails(viewModel.productId, viewModel.productName)
        })

    }

    private fun moveToProductDetails(productId: String, productName: String) {
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(Constants.PRODUCTS, productId)
            it.putExtra(Constants.PRODUCT_NAME, productName)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun showCartBottomSheet() {
           ItemsBottomSheet(this, orderItemsAdapter).show()
    }

    private fun hideCartBottomSheet() {
        mCartBottomSheetDialog.dismiss()
    }

    private fun initRecyclerView() {
        ordersAdapter = PurchaseHistoryAdapter(
            this,
            listOf(),
            viewModel
        )
        binding.rvPurchaseHistory.layoutManager = LinearLayoutManager(this)
        binding.rvPurchaseHistory.adapter = ordersAdapter

        orderItemsAdapter = OrderItemsAdapter(
            this,
            listOf(),
            viewModel,
            arrayListOf(),
            ""
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}