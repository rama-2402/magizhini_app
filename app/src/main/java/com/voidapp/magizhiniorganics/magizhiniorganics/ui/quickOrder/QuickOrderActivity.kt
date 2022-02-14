package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.CaseMap
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListUpdateCallback
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AddressAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.QuickOrderListAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityQuickOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.ItemsBottomSheet
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOAD_DIALOG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.PermissionsUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toCartEntity
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

/*
* check if there is any order list already available
* disable the add button if order list is already in place
* enable place order only after adding atleast one image
*
* */

class QuickOrderActivity :
    BaseActivity(),
    KodeinAware,
    AddressAdapter.OnAddressClickListener,
    QuickOrderListAdapter.QuickOrderClickListener,
    AddressDialogClickListener
{
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityQuickOrderBinding

    private lateinit var viewModel: QuickOrderViewModel
    private val factory: QuickOrderViewModelFactory by instance()

    private lateinit var addressAdapter: AddressAdapter
    private lateinit var quickOrderListAdapter: QuickOrderListAdapter

    private var isPreviewOpened: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_order)
        viewModel = ViewModelProvider(this, factory).get(QuickOrderViewModel::class.java)

        initRecyclerView()
        initData()
        initObservers()
        initListeners()
    }

    private fun initData() {
        lifecycleScope.launchWhenCreated {
            viewModel.userProfile = UserProfileEntity()
            viewModel.addressContainer = Address()
        }
        showProgressDialog()
        viewModel.getAddress()
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivHelp.setOnClickListener {

            }
            ivPreviewImage.setOnClickListener {
                onBackPressed()
            }
            btnGetEstimate.setOnClickListener {
                if (viewModel.quickOrder == null && viewModel.orderListUri.isNullOrEmpty()) {
                    showErrorSnackBar("Please add your purchase list image to get Estimate", true)
                    return@setOnClickListener
                }
                viewModel.quickOrder?.let {
                    if (it.cart.isEmpty()) {
                        showErrorSnackBar("Estimate not yet available. Please wait", true)
                    } else {
                        cartItemsDialog(it.cart)
                    }
                } ?: showExitSheet(this@QuickOrderActivity, "To get Estimate price, Your List will be sent for validation and we will contact you with the price breakdown for each product and Total Order. Please click PROCEED below to start uploading order list.", "estimate")
            }
            btnPlaceOrder.setOnClickListener {
                if (viewModel.quickOrder == null && viewModel.orderListUri.isNullOrEmpty()) {
                    if (PermissionsUtil.hasStoragePermission(this@QuickOrderActivity)) {
                        getAction.launch(pickImageIntent)
                    } else {
                        showExitSheet(this@QuickOrderActivity, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                    }
                    return@setOnClickListener
                }
                viewModel.quickOrder?.let {
                    showListBottomSheet(this@QuickOrderActivity, arrayListOf<String>("Online", "Wallet (Rs: ${viewModel.wallet?.amount})", "Cash On Delivery"))
                } ?: viewModel.sendOrderPlaceRequest()
            }
        }
    }

    private fun initObservers() {
            viewModel.uiEvent.observe(this@QuickOrderActivity) { event ->
                when (event) {
                    is QuickOrderViewModel.UiEvent.WalletData -> {
                        viewModel.wallet = event.wallet
                    }
                    is QuickOrderViewModel.UiEvent.StartingTransaction -> {
                        showLoadStatusDialog("", "Processing payment from Wallet...", "transaction")
                    }
                    is QuickOrderViewModel.UiEvent.PlacingOrder -> {
                        updateLoadStatusDialogText("placingOrder")
                    }
                    is QuickOrderViewModel.UiEvent.OrderPlaced -> {
                        updateLoadStatusDialogText("orderPlaced")
                        lifecycleScope.launch {
                            delay(1800)
                            dismissLoadStatusDialog()
                        }
                    }
                    is QuickOrderViewModel.UiEvent.WalletTransactionFailed -> {
                        lifecycleScope.launch {
                            delay(1800)
                            dismissLoadStatusDialog()
                            showErrorSnackBar(
                                "Server Error! Wallet Transaction Failed. Try later",
                                true
                            )
                        }
                    }
                    is QuickOrderViewModel.UiEvent.OrderPlacementFailed -> {
                        updateLoadStatusDialogText("dismiss")
                        showErrorSnackBar("Server Error! Order Placement Failed. Try later", true)
                    }
                    is QuickOrderViewModel.UiEvent.BeginningUpload -> {
                        showLoadStatusDialog(
                            "",
                            "Starting to Upload your order List... Please wait",
                            "upload"
                        )
                    }
                    is QuickOrderViewModel.UiEvent.UploadingImage -> {
                        updateLoadStatusDialogText("Uploading Page ${event.pageNumber}...")
                    }
                    is QuickOrderViewModel.UiEvent.UploadComplete -> {
                        updateLoadStatusDialogText("Files Upload Complete!")
                        updateLoadStatusDialogText("success")
                        lifecycleScope.launch {
                            delay(1800)
                            dismissLoadStatusDialog()
                            showExitSheet(
                                this@QuickOrderActivity,
                                "We have received your order estimate request. This might take some time. After verification we will contact you with price breakdown.",
                                "close"
                            )
                        }
                    }
                    is QuickOrderViewModel.UiEvent.AddressUpdate -> {
                        if (event.isSuccess) {
                            if (event.message == "update") {
                                showToast(this@QuickOrderActivity, "Address Updated")
                            }
                            populateAddressDetails(event.data as List<Address>)
                        } else {
                            showErrorSnackBar(event.message, true)
                        }
                    }
                    is QuickOrderViewModel.UiEvent.EstimateData -> {
                        if (event.isSuccess) {
                            event.data?.let {
                                populateEstimateDetails(it)
                            }
                        } else {
                            showErrorSnackBar(event.message, true)
                        }
                        hideProgressDialog()
                    }
                    is QuickOrderViewModel.UiEvent.Empty -> return@observe
                    else -> Unit
                }
                viewModel.setEmptyStatus()
            }
    }

    private fun initRecyclerView() {
        addressAdapter = AddressAdapter(
            this,
            viewModel.mCheckedAddressPosition,
            arrayListOf(),
            this
            )
        binding.rvAddress.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAddress.adapter = addressAdapter

        quickOrderListAdapter = QuickOrderListAdapter(
            this,
            listOf(),
            listOf(),
            this
        )
        binding.rvOrderList.layoutManager =
            GridLayoutManager(this, 3)
        binding.rvOrderList.adapter = quickOrderListAdapter
    }

    private fun cartItemsDialog(cartItems: ArrayList<Cart>) {
        val entityMap = cartItems.map { it.toCartEntity() }
        val orderItemsAdapter = OrderItemsAdapter(
            this,
            entityMap,
            viewModel,
            arrayListOf(),
            "quickOrder"
        )
        ItemsBottomSheet(this, orderItemsAdapter).show()
    }

    private fun showLoadStatusDialog(title: String, body: String, content: String) {
        LoadStatusDialog.newInstance(title, body, content).show(supportFragmentManager, LOAD_DIALOG)
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    private fun updateLoadStatusDialogText(content: String) {
        LoadStatusDialog.statusText.value = content
    }

    private fun populateAddressDetails(addresses: List<Address>) {
        addressAdapter.setAddressData(addresses)
    }

    private fun populateEstimateDetails(quickOrder: QuickOrder) {
        viewModel.quickOrder = quickOrder
        binding.apply {
            quickOrderListAdapter.quickOrderListUrl = quickOrder.imageUrl
            quickOrderListAdapter.notifyDataSetChanged()
            if (quickOrder.note.isNotEmpty()) {
                ivNotification.badgeValue = 1
            }
            btnGetEstimate.text =
                "Rs:${viewModel.getTotalCartPrice()} (${quickOrder.cart.size} Items)"
            updatePlaceOrderButton()
            showToast(this@QuickOrderActivity, "click Total Price to get Individual Item Price", LONG)
        }

    }

    fun selectedPaymentMode(paymentMethod: String) {
        when(paymentMethod) {
            "Online" -> {

            }
            "Wallet" -> {
                val orderDetailsMap: HashMap<String, Any> = hashMapOf()
                orderDetailsMap["deliveryPreference"] = binding.spDeliveryPreference.selectedItem
                orderDetailsMap["deliveryNote"] = binding.etDeliveryNote.text.toString().trim()
                orderDetailsMap["appliedCoupon"] = if (binding.etCoupon.text.toString().isEmpty()) {
                    ""
                } else {
                    binding.etCoupon.text.toString().trim()
                }

                viewModel.proceedForWalletPayment(
                    orderDetailsMap
                )
            }
            else -> viewModel.sendOrderPlaceRequest()
        }
    }

    fun sendEstimateRequest() {
        val imageExtensionList = mutableListOf<String>()
        for (uri in viewModel.orderListUri) {
            imageExtensionList.add(GlideLoader().imageExtension(this@QuickOrderActivity, uri)!!)
        }

        viewModel.sendGetEstimateRequest (
            imageExtensionList
        )
    }

    private fun updatePlaceOrderButton() {
        if(
            viewModel.quickOrder == null &&
            viewModel.orderListUri.isNullOrEmpty()
        ) {
            binding.btnPlaceOrder.text = "Add List"
        } else {
            binding.btnPlaceOrder.text = "Place Order"
        }
    }

    private fun loadNewImage() {
        quickOrderListAdapter.quickOrderList = viewModel.orderListUri
        quickOrderListAdapter.notifyDataSetChanged()
        updatePlaceOrderButton()
    }

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val newOrderImage = result.data?.data
        newOrderImage?.let {
            viewModel.addNewImageUri(newOrderImage)
            loadNewImage()
        }
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    override fun onBackPressed() {
        when {
            isPreviewOpened -> {
//                binding.ivPreviewImage.startAnimation(Animations.scaleSmall)
                binding.ivPreviewImage.remove()
                isPreviewOpened = false
            }
            else -> {
                Intent(this, HomeActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.userProfile = null
        viewModel.quickOrder = null
        viewModel.addressContainer = null
        viewModel.wallet = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.STORAGE_PERMISSION_CODE) {
            if(
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showToast(this, "Storage Permission Granted")
                getAction.launch(pickImageIntent)
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }

//    private suspend fun onSuccessCallback(message: String, data: Any?) {
//        when (message) {
//            "address" -> {
//                populateAddressDetails(data as List<Address>)
//            }
//            "estimate" -> {
//                populateEstimateDetails(data as QuickOrder)
//                updatePlaceOrderButton()
//                hideProgressDialog()
////                orderDetailsVisibility(true)
//            }
//            "empty" -> {
//                updatePlaceOrderButton()
//                hideProgressDialog()
////                orderDetailsVisibility(false)
//            }
//            "addressUpdate" -> {
//                populateAddressDetails(data as List<Address>)
//                showToast(this, "Address Updated")
//            }
////            "starting" -> {
////                showLoadStatusDialog("", "Starting to Upload your order List... Please wait", "upload")
////            }
////            "uploading" -> {
////                updateLoadStatusDialogText("Uploading Page $data...")
////            }
////            "complete" -> {
////                updateLoadStatusDialogText("Files Upload Complete!")
////                updateLoadStatusDialogText("success")
////                delay(2000)
////                updateLoadStatusDialogText("dismiss")
////                showExitSheet(this, "We have received your order estimate request. This might take some time. After verification we will contact you with price breakdown.", "close")
////            }
//        }
//        viewModel.setEmptyStatus()
//    }
//
//    private fun onFailedCallback(message: String, data: Any?) {
//        when (message) {
//            "address" -> {
//                showErrorSnackBar(data as String, true)
//            }
//            "addressUpdate" -> {
//                showToast(this, data as String)
//            }
//            "complete" -> {
//                LoadStatusDialog.statusText.value = "dismiss"
////                showErrorSnackBar("Failed to upload List to generate Estimate. Try later", true)
//                showExitSheet(this, data.toString())
//            }
//            "estimate" -> {
//                hideProgressDialog()
//                showErrorSnackBar(data as String, true)
//            }
//
//        }
//        viewModel.setEmptyStatus()
//    }

    //from address adapter
    override fun selectedAddress(position: Int) {
        viewModel.mCheckedAddressPosition = position
        addressAdapter.checkedAddressPosition = position
        addressAdapter.notifyDataSetChanged()
    }

    override fun addAddress(position: Int) {
        AddressDialog().show(supportFragmentManager, "addressDialog")
    }

    override fun deleteAddress(position: Int) {
        viewModel.deleteAddress(position)
        addressAdapter.checkedAddressPosition = 0
        addressAdapter.notifyDataSetChanged()
    }

    override fun updateAddress(position: Int) {
        viewModel.addressPosition = position
        viewModel.userProfile?.let {
            val dialog = AddressDialog()
            val bundle = Bundle()
            bundle.putParcelable("address", it.address[position])
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "addressDialog")
        }

    }

    //from order list adapter
    override fun selectedListImage(position: Int, imageUri: Any) {
        binding.apply {
            isPreviewOpened = true
            GlideLoader().loadUserPictureWithoutCrop(this@QuickOrderActivity, imageUri, ivPreviewImage)
//            ivPreviewImage.startAnimation(Animations.scaleBig)
            ivPreviewImage.visible()
        }
    }

    override fun deleteListItem(position: Int, imageUri: Any) {
        viewModel.orderListUri.removeAt(position)
        loadNewImage()
    }

    override fun addImage() {
        if (PermissionsUtil.hasStoragePermission(this)) {
            getAction.launch(pickImageIntent)
        } else {
            showExitSheet(this, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
        }
    }

    //from address dialog
    override fun savedAddress(addressMap: HashMap<String, Any>, isNew: Boolean) {
        viewModel.addressContainer?.let { address ->
            address.userId = addressMap["userId"].toString()
            address.addressLineOne = addressMap["addressLineOne"].toString()
            address.addressLineTwo = addressMap["addressLineTwo"].toString()
            address.LocationCode = addressMap["LocationCode"].toString()
            address.LocationCodePosition = addressMap["LocationCodePosition"].toString().toInt()
            address.city = addressMap["city"].toString()
            if (isNew) {
                viewModel.addAddress(address)
            } else {
                viewModel.updateAddress(address)
            }
        }
    }
}




