package com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.PurchaseHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityPurchaseHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetOrderHistoryService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.dialogs.ItemsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.StringBufferInputStream
import java.text.SimpleDateFormat

class PurchaseHistoryActivity : BaseActivity(), KodeinAware {
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityPurchaseHistoryBinding
    private val factory: PurchaseHistoryViewModelFactory by instance()
    private lateinit var viewModel: PurchaseHistoryViewModel

    private lateinit var ordersAdapter: PurchaseHistoryAdapter
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    private var mOrderHistory: MutableList<OrderEntity> = mutableListOf()

    private var mFilterMonth = "January"
    private var mFilterYear = "2021"
    private var mCancelOrder = OrderEntity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_purchase_history)
        viewModel = ViewModelProvider(this, factory).get(PurchaseHistoryViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)
        mFilterMonth = Time().getMonth()
        mFilterYear = Time().getYear()
        binding.fabMonthFilter.text = " $mFilterMonth"

        showShimmer()

        initRecyclerView()
        initLiveData()
        initListeners()
    }

    private fun initListeners() {
        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.tvYearFilter.setOnClickListener {
            val years = resources.getStringArray(R.array.years).toList() as ArrayList<String>
            showListBottomSheet(this, years, data = "years")
        }

        //scroll change listener to hide the fab when scrolling down
        binding.rvPurchaseHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                super.onScrolled(recyclerView, up, down)
                if (down > 0 && binding.fabMonthFilter.isVisible) {
                    binding.fabMonthFilter.startAnimation(AnimationUtils.loadAnimation(binding.fabMonthFilter.context, R.anim.slide_out_right))
                    binding.fabMonthFilter.hide()
                } else if (down < 0 && binding.fabMonthFilter.isGone) {
                    binding.fabMonthFilter.show()
                    binding.fabMonthFilter.startAnimation(AnimationUtils.loadAnimation(this@PurchaseHistoryActivity, R.anim.slide_in_right))
                }
            }
        })

        binding.fabMonthFilter.setOnClickListener {
            val months = resources.getStringArray(R.array.months_name).toList() as ArrayList<String>
            showListBottomSheet(this, months, data = "months")
        }
    }

    fun setYearFilter(year: String) {
        mFilterYear = year
        binding.tvYearFilter.text = year
        fetchData()
    }

    fun setMonthFilter(month: String) {
        mFilterMonth = month
        binding.fabMonthFilter.text = " $month"
        fetchData()
//        val monthFormat = SimpleDateFormat("MM")
//        val currentMonth = monthFormat.format(System.currentTimeMillis())
//        if (getMonthNumber(month) <= currentMonth.toInt()) {
//            mFilterMonth = month
//            fetchData()
//        } else {
//            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
//        }
    }

    private fun fetchData() {
//        if (mFilterYear.toInt() <= Time().getYear().toInt() && getMonthNumber(month) <= currentMonth.toInt()) {
            showShimmer()
            viewModel.getAllPurchaseHistory("${mFilterMonth}${mFilterYear}")
//        }
    }

//    private fun getMonthNumber(month: String): Int {
//       return when(month) {
//           "January" -> 1
//           "February" -> 2
//           "March" -> 3
//           "April" -> 4
//           "May" -> 5
//           "June" -> 6
//           "July" -> 7
//           "August" -> 8
//           "September" -> 9
//           "October" -> 10
//           "November" -> 11
//           else -> 12
//       }
//    }

    private fun initLiveData() {
        viewModel.getAllPurchaseHistory("${mFilterMonth}${mFilterYear}")
        viewModel.getFavorites()

        viewModel.purchaseHistory.observe(this, { orderEntities ->
            lifecycleScope.launch {
                mOrderHistory.clear()
                mOrderHistory.addAll(orderEntities)
                mOrderHistory.sortByDescending {
                    it.purchaseDate
                }
                ordersAdapter.orders = mOrderHistory
                ordersAdapter.notifyDataSetChanged()
                delay(1500)
                hideShimmer()
            }
        })

        viewModel.showCartBottomSheet.observe(this, {
            orderItemsAdapter.cartItems = it
            showCartBottomSheet()
        })

        //observing the favorites livedata
        viewModel.favorites.observe(this, {
            orderItemsAdapter.favorites = it
//            orderItemsAdapter.notifyDataSetChanged()
        })

        viewModel.moveToProductReview.observe(this, {
            moveToProductDetails(viewModel.productId, viewModel.productName)
        })

        viewModel.cancelOrder.observe(this, {
            mCancelOrder = it
            showExitSheet(this, "Confirm Cancellation")
        })

        viewModel.cancellationStatus.observe(this, {
            if (it) {
                startWorkerThread(mCancelOrder)
                viewModel.orderCancelled(mCancelOrder)
                for (i in mOrderHistory.indices) {
                    if (mOrderHistory[i].orderId == mCancelOrder.orderId) {
                        mOrderHistory[i].orderStatus = Constants.CANCELLED
                        ordersAdapter.orders = mOrderHistory
                        ordersAdapter.notifyItemChanged(i)
                        hideProgressDialog()
                        showErrorSnackBar("Order Cancelled", false)
                    }
                }
            } else {
                hideProgressDialog()
                showErrorSnackBar("Cancellation failed! Please try again later", true)
            }
        })

        viewModel.startWork.observe(this, {
            val workRequest: WorkRequest =
                OneTimeWorkRequestBuilder<GetOrderHistoryService>()
                    .setInputData(workDataOf(
                        "filter" to "${mFilterMonth}${mFilterYear}"
                    ))
                    .build()

            WorkManager.getInstance(this).enqueue(workRequest)
            WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(workRequest.id)
                .observe(this, {
                    when (it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            viewModel.getAllPurchaseHistory("${mFilterMonth}${mFilterYear}")
                        }
                        WorkInfo.State.CANCELLED -> {
                            hideShimmer()
                            showErrorSnackBar("Failed to get data! Please try again later", true)
                        }
                        WorkInfo.State.FAILED -> {
                            hideShimmer()
                            showErrorSnackBar("Failed to get data! Please try again later", true)
                        }
                    }
                })
        })

        viewModel.errorMessage.observe(this, {
            hideShimmer()
            Toast.makeText(this, "No data available in the selected period", Toast.LENGTH_SHORT).show()
        })
    }

    private fun startWorkerThread(order: OrderEntity) {
        val stringConvertedOrder = order.toStringConverter(order)
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
                .setInputData(
                    workDataOf(
                    "order" to stringConvertedOrder,
                        Constants.STATUS to false
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun OrderEntity.toStringConverter(order: OrderEntity): String {
        return Gson().toJson(order)
    }

    fun cancellationConfirmed() {
        hideExitSheet()
        showProgressDialog()
        viewModel.confirmCancellation(mCancelOrder)
    }

    private fun moveToProductDetails(productId: String, productName: String) {
        hideCartBottomSheet()
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
        ItemsBottomSheet(this, orderItemsAdapter).dismiss()
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

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.show()
            rvPurchaseHistory.gone()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.gone()
            rvPurchaseHistory.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}