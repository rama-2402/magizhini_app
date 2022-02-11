package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AddressAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.QuickOrderListAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityQuickOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Animations
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.PermissionsUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

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
        viewModel.getAddress()
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivHelp.setOnClickListener {
                if (PermissionsUtil.hasStoragePermission(this@QuickOrderActivity)) {
                    getAction.launch(pickImageIntent)
                } else {
                    showExitSheet(this@QuickOrderActivity, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                }
            }
            ivPreviewImage.setOnClickListener {
                onBackPressed()
            }
            btnGetEstimate.setOnClickListener {
                viewModel.sendGetEstimateRequest()
            }
            btnPlaceOrder.setOnClickListener {
                viewModel.sendOrderPlaceRequest()
            }

        }
    }

    private fun initObservers() {
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
            mutableListOf(),
            this
        )
        binding.rvOrderList.layoutManager =
            GridLayoutManager(this, 3)
        binding.rvOrderList.adapter = quickOrderListAdapter
    }

    private fun populateAddressDetails(addresses: List<Address>) {
        addressAdapter.setAddressData(addresses)
    }

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val newOrderImage = result.data?.data
        newOrderImage?.let {
            viewModel.addNewImageUri(newOrderImage)
            loadNewImage()
        }
    }

    private fun loadNewImage() {
        quickOrderListAdapter.quickOrderList = viewModel.orderListUri
        quickOrderListAdapter.notifyDataSetChanged()
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    override fun onBackPressed() {
        when {
            isPreviewOpened -> {
                binding.ivPreviewImage.startAnimation(Animations.scaleSmall)
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

    private fun onSuccessCallback(message: String, data: Any?) {
        when (message) {
            "address" -> {
                populateAddressDetails(data as List<Address>)
            }
            "addressUpdate" -> {
                viewModel.userProfile?.let {
                    populateAddressDetails(it.address)
                }
                showToast(this, data as String)
            }
        }
        viewModel.setEmptyStatus()
    }

    private fun onFailedCallback(message: String, data: Any?) {
        when (message) {
            "address" -> {
                showErrorSnackBar(data as String, true)
            }
            "addressUpdate" -> {
                showToast(this, data as String)
            }
        }
        viewModel.setEmptyStatus()
    }

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
    override fun selectedListImage(position: Int, imageUri: Uri) {
        binding.apply {
            isPreviewOpened = true
            GlideLoader().loadUserPictureWithoutCrop(this@QuickOrderActivity, imageUri, ivPreviewImage)
            ivPreviewImage.startAnimation(Animations.scaleBig)
            ivPreviewImage.visible()
        }
    }

    override fun deleteListItem(position: Int, imageUri: Uri) {
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




