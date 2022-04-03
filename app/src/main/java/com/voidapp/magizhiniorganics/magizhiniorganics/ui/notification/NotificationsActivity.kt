package com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.NotificationsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.SwipeGesture
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserNotificationEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityNotificationsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BOOLEAN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CATEGORY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_HISTORY_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_NOTIFICATIONS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class NotificationsActivity :
    BaseActivity(),
    KodeinAware,
    NotificationsAdapter.NotificationItemClickListener
{
    override val kodein: Kodein by kodein()

    private val factory: NotificationsViewModelFactory by instance()
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var viewModel: NotificationsViewModel

    private lateinit var notificationsAdapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notifications)
        viewModel = ViewModelProvider(this, factory).get(NotificationsViewModel::class.java)

        initData()
        initRecyclerView()
        initObservers()
        initListener()
    }

    private fun initListener() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                finish()
            }
            tvClearAll.setOnClickListener {
                showProgressDialog()
                viewModel.clearAllNotifications()
            }
        }
    }

    private fun initRecyclerView() {
        notificationsAdapter = NotificationsAdapter(
            this,
            mutableListOf(),
            this
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = notificationsAdapter

        val swipeGesture = object : SwipeGesture(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when(direction) {
                    ItemTouchHelper.LEFT -> {
                        viewModel.clickedNotificationPosition = viewHolder.absoluteAdapterPosition
                        deleteNotification()
                    }
                    ItemTouchHelper.RIGHT -> {
                        viewModel.clickedNotificationPosition = viewHolder.absoluteAdapterPosition
                        deleteNotification()
                    }
                }
            }
        }

        val touchHelper = ItemTouchHelper(swipeGesture)
        touchHelper.attachToRecyclerView(binding.rvNotifications)
    }

    private fun deleteNotification() {
        showProgressDialog()
        viewModel.deleteNotification()
    }

    private fun initData() {
        viewModel.getAllNotifications()
    }

    private fun initObservers() {
        viewModel.notifications.observe(this) {
            hideProgressDialog()
            if (it.isEmpty()) {
                showEmptyScreen()
            } else {
                hideEmptyScreen()
                viewModel.allNotifications.clear()
                viewModel.allNotifications.addAll(it)
                notificationsAdapter.notifications = viewModel.allNotifications
                binding.tvToolbarTitle.text = "Notifications (${viewModel.allNotifications.size})"
                notificationsAdapter.notifyDataSetChanged()

                if (!SharedPref(this).getData(USER_NOTIFICATIONS, BOOLEAN, false).toString()
                        .toBoolean()
                ) {
                    showToast(this, "Swipe Left or Right to clear Notification")
                    SharedPref(this).putData(USER_NOTIFICATIONS, BOOLEAN, true)
                }

            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when(result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> {
                        if (result.message == "") {
                            showProgressDialog()
                        } else {
                            showSuccessDialog("", result.message, result.data)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "delete" -> {
                hideProgressDialog()
                showToast(this, data as String)
                removeNotificationFromAdapter()
            }
            "deleteAll" -> {
                hideProgressDialog()
                showToast(this, data as String)
                viewModel.allNotifications.clear()
                notificationsAdapter.notifyDataSetChanged()
            }
        }
        viewModel.setEmptyStatus()
    }

    private fun removeNotificationFromAdapter() {
        viewModel.allNotifications.removeAt(viewModel.clickedNotificationPosition)
        notificationsAdapter.notifyItemRemoved(viewModel.clickedNotificationPosition)
        binding.tvToolbarTitle.text = "Notifications (${viewModel.allNotifications.size})"
    }

    private fun showEmptyScreen() {
        binding.apply {
            llEmptyLayout.visible()
            tvClearAll.hide()
            tvToolbarTitle.text = "Notifications (0)"
            tvEmptyMessage.text = "Looks like there is no New Notifications!"
        }
    }
    private fun hideEmptyScreen() {
        binding.apply {
            llEmptyLayout.remove()
            tvClearAll.visible()
        }
    }

    private fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "delete" -> {
                hideProgressDialog()
                showToast(this, data as String)
            }
            "deleteAll" -> {
                hideProgressDialog()
                showToast(this, data as String)
            }

        }
        viewModel.setEmptyStatus()
    }

    override fun clickedNotification(notification: UserNotificationEntity, position: Int) {
        lifecycleScope.launch {
            viewModel.clickedNotificationPosition = position
            deleteNotification()
            when(notification.clickType) {
                PRODUCTS -> {
                    val entity = viewModel.getProductByID(notification.clickContent)
                    entity?.let { product ->
                        Intent(this@NotificationsActivity, ProductActivity::class.java).also {
                            it.putExtra(PRODUCTS, product.id)
                            it.putExtra(Constants.PRODUCT_NAME, product.name)
                            startActivity(it)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    } ?: showErrorSnackBar("Product does not Exist", true)
                }
                CATEGORY -> {
                    viewModel.getCategoryName(notification.clickContent)?.let { category ->
                        Intent(this@NotificationsActivity, ShoppingMainActivity::class.java).also {
                            it.putExtra(CATEGORY, category)
                            startActivity(it)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    }
                }
                ORDER_HISTORY_PAGE -> {
                    Intent(this@NotificationsActivity, PurchaseHistoryActivity::class.java).also {
                        startActivity(it)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
                SUBSCRIPTION -> {
                    Intent(this@NotificationsActivity, SubscriptionHistoryActivity::class.java).also {
                        startActivity(it)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
                WALLET -> {
                    Intent(this@NotificationsActivity, WalletActivity::class.java).also {
                        startActivity(it)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
                else -> Unit
            }
        }
    }
}