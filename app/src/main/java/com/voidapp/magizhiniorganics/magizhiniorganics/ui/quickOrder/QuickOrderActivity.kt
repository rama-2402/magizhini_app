package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.QuickOrderClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.QuickOrderListAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.QuickOrderTextAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrderTextItem
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityQuickOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOAD_DIALOG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView
import java.io.File


class QuickOrderActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    QuickOrderClickListener,
    AddressDialogClickListener {
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityQuickOrderBinding

    private lateinit var viewModel: QuickOrderViewModel
    private val factory: QuickOrderViewModelFactory by instance()

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var checkoutText: TextView
    private lateinit var filterBtn: ImageView

    private lateinit var cartAdapter: CartAdapter
    private lateinit var quickOrderListAdapter: QuickOrderListAdapter
    private var quickOrderTextAdapter: QuickOrderTextAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_order)
        viewModel = ViewModelProvider(this, factory).get(QuickOrderViewModel::class.java)

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            onBackPressed()
        }

        Checkout.preload(applicationContext)

        initRecyclerView()
        initData()
        initObservers()
        initListeners()
    }

    private fun initData() {
        binding.ivImage.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.matteRed))
        lifecycleScope.launchWhenCreated {
            viewModel.userProfile = UserProfileEntity()
            viewModel.addressContainer = Address()
        }
        viewModel.getAddress()
    }

    private fun initListeners() {
        binding.apply {
            /*
            * This is a keyboard listener that changes visibility of buttons when typing for coupon or description
            * */
            KeyboardVisibilityEvent.setEventListener(
                this@QuickOrderActivity
            ) { isOpen ->
                if (!isOpen) {
                    viewModel.quickOrder ?: let {
                        binding.btnGetEstimate.visible()
                        binding.btnPlaceOrder.visible()
                    }
                } else {
                    binding.btnPlaceOrder.remove()
                    binding.btnGetEstimate.remove()
                }
            }
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            btnApplyCoupon.setOnClickListener {
                if (etCoupon.text.isNullOrEmpty()) {
                    showToast(this@QuickOrderActivity, "Enter a coupon code")
                    return@setOnClickListener
                }
                /*
                * checking if cart is empty so that we can determine if estimate is received
                * since estimate data is posted with cart items filled
                * */
                if (viewModel.quickOrder?.cart?.isEmpty() != false) {
                    etCoupon.setText("")
                    if (KeyboardVisibilityEvent.isKeyboardVisible(this@QuickOrderActivity)) {
                        this@QuickOrderActivity.hideKeyboard()
                    }
                    showErrorSnackBar(
                        "Coupon can be applied only after receiving Estimate Data",
                        true
                    )
                } else {
                    /*
                    * if couponAppliedPrice is not null that mean coupon is already applied. So it has to be removed
                    * */
                    viewModel.couponAppliedPrice?.let {
                        applyUiChangesWithCoupon(false)
                    } ?: viewModel.verifyCoupon(etCoupon.text.toString().trim())
                }
            }
            ivHelp.setOnClickListener {
                showDescriptionBs(getString(R.string.quick_order_description))
//                showProgressDialog(true)
//                viewModel.getHowToVideo("QuickOrder")
            }
            ivText.setOnClickListener {
                if (viewModel.currentQuickOrderMode == "text") {
                    showToast(this@QuickOrderActivity, "Already in Text Mode")
                    return@setOnClickListener
                } else {
                    showExitSheet(
                        this@QuickOrderActivity,
                        "Quick Order Text Mode will be activated and all current changes will be discarded. Click PROCEED to continue",
                        "text"
                    )
                }
//                setCurrentQuickOrderTypeSelection("text")
            }
            ivImage.setOnClickListener {
                if (viewModel.currentQuickOrderMode == "image") {
                    showToast(this@QuickOrderActivity, "Already in Image Mode")
                    return@setOnClickListener
                } else {
                    showExitSheet(
                        this@QuickOrderActivity,
                        "Quick Order Image Mode will be activated and all current changes will be discarded. Click PROCEED to continue",
                        "image"
                    )
                }
//                setCurrentQuickOrderTypeSelection("image")
            }
            ivVoice.setOnClickListener {
                if (viewModel.currentQuickOrderMode == "voice") {
                    showToast(this@QuickOrderActivity, "Already in Voice Mode")
                    return@setOnClickListener
                } else {
                    showExitSheet(
                        this@QuickOrderActivity,
                        "Quick Order Voice Mode will be activated and all current changes will be discarded. Click PROCEED to continue",
                        "voice"
                    )
                }
//                setCurrentQuickOrderTypeSelection("audio")
            }
            btnAddProduct.setOnClickListener {
                when {
                    etproductName.text.toString().isNullOrEmpty() -> showToast(
                        this@QuickOrderActivity,
                        "Please provide a brief description of product name"
                    )
                    etVariantName.text.toString().isNullOrEmpty() -> showToast(
                        this@QuickOrderActivity,
                        "Please provide the product type (Eg: 1Kg)"
                    )
                    etQuantity.text.toString().isNullOrEmpty() -> showToast(
                        this@QuickOrderActivity,
                        "Please provide the quantity"
                    )
                    else -> addProductToQuickOrderTextList()
                }
            }
            ivRecord.setOnClickListener {
                if (viewModel.isPlaying) {
                    stopRecording()
                } else {
                    PermissionsUtil.checkAudioPermission(this@QuickOrderActivity)
                }
            }
            ivDeleteRecording.setOnClickListener {
                viewModel.apply {
                    player?.stop()
                    player?.reset()
                    player?.release()
                    recorder = null
                    player = null
                    isPlaying = false
                    lastProgress = 0
                    pausedTime = 0
                    tvRecordTime.base = SystemClock.elapsedRealtime()
                    tvRecordTime.stop()
                    fileName?.let { path ->
                        File(path).let { file ->
                            if (file.exists()) {
                                file.delete()
                                fileName = null
                            }
                        }
                    }
                    if (currentQuickOrderMode == "voice") {
                        updateAudioLayout(true)
                    }
                }
            }
            ivPlayPause.setOnClickListener {
                when {
                    (!viewModel.isPlaying && viewModel.pausedTime == 0L) -> {
                        viewModel.quickOrder  ?.let {
                            ivPlayPause.fadOutAnimation()
                            ivPlayPause.hide()
                            progressCircular.fadInAnimation()
                            progressCircular.visible()
                            showToast(this@QuickOrderActivity, "Loading Audio...")
                            MediaPlayer().let { player ->
                                viewModel.player = player
                                viewModel.player?.setOnBufferingUpdateListener { mediaPlayer, buffer ->
                                    val ratio = buffer / 100.0
                                    val bufferingLevel = (mediaPlayer.duration * ratio).toInt()
                                    seekBar.secondaryProgress = bufferingLevel
                                }
                                viewModel.quickOrder?.let {
                                    viewModel.player?.setDataSource(it.audioFileUrl)
                                }
                                viewModel.player?.prepareAsync()
                                viewModel.player?.setOnPreparedListener {
                                    progressCircular.fadOutAnimation()
                                    progressCircular.remove()
                                    progressCircular.clearAnimation()
                                    ivPlayPause.fadInAnimation()
                                    ivPlayPause.visible()
                                    showToast(this@QuickOrderActivity, "Ready to play.!")
                                    viewModel.player?.start()
                                    updatePlayerLayout("play")
                                }
                            }
                        } ?: let {
                            MediaPlayer().let { player ->
                                viewModel.player = player
                                viewModel.player?.setOnBufferingUpdateListener { mediaPlayer, buffer ->
                                    val ratio = buffer / 100.0
                                    val bufferingLevel = (mediaPlayer.duration * ratio).toInt()
                                    seekBar.secondaryProgress = bufferingLevel
                                }
                                viewModel.player?.setDataSource(viewModel.fileName)
                                viewModel.player?.prepare()
                                viewModel.player?.start()
                                updatePlayerLayout("play")
                            }
                        }
                    }
                    (viewModel.isPlaying) -> {
                        updatePlayerLayout("pause")
                    }
                    (!viewModel.isPlaying && viewModel.pausedTime != 0L) -> {
                        updatePlayerLayout("resume")
                    }
                }
            }
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    viewModel.player?.let {
                        if (fromUser) {
                            it.seekTo(progress)
                            tvRecordTime.base = SystemClock.elapsedRealtime() - it.currentPosition
                            viewModel.lastProgress = progress
                        }
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
//            ivNotification.setOnClickListener {
//                viewModel.quickOrder?.let {
//                    if (it.note.isNotEmpty()) {
//                        showDescriptionBs(it.note)
//                        binding.ivNotification.badgeValue = 0
//                    } else {
//                        showToast(this@QuickOrderActivity, "No New Notification")
//                    }
//                }
//            }
            nsvScrollBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                viewModel.quickOrder?.let {
                    when {
                        scrollY < oldScrollY -> cartBottomSheet.state =
                            BottomSheetBehavior.STATE_COLLAPSED
                        scrollY > oldScrollY -> cartBottomSheet.state =
                            BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            })
            clAddress.setOnClickListener {
                updateAddress()
            }
            btnGetEstimate.setOnClickListener {
                if (!NetworkHelper.isOnline(this@QuickOrderActivity)) {
                    showErrorSnackBar("Please check your Internet Connection", true)
                    return@setOnClickListener
                }
                /*
                * If there is no quick order and the uri is empty then nothing is selected
                * */
                if (validateEntry()) {
                    showExitSheet(
                        this@QuickOrderActivity,
                        "To get Estimate price, Your List will be sent for validation and we will contact you with the price breakdown for each product and Total Order. Please click PROCEED below to start uploading order list.",
                        "estimate"
                    )
                }
            }
            btnPlaceOrder.setOnClickListener {
                /*
                * So if quick order is null and there is no uri means no pic is added yet. Hence for first pic to be uploaded
                * we click from place order button
                * */
//                if (viewModel.quickOrder == null && viewModel.orderListUri.isNullOrEmpty()) {
//                    if (PermissionsUtil.hasStoragePermission(this@QuickOrderActivity)) {
//                        getAction.launch(pickImageIntent)
//                    } else {
//                        showExitSheet(this@QuickOrderActivity, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
//                    }
//                    return@setOnClickListener
//                }
                if (validateEntry()) {
                    showListBottomSheet(
                        this@QuickOrderActivity,
                        arrayListOf<String>(
                            "Online",
                            "Wallet (Rs: ${viewModel.wallet?.amount})",
                            "Cash On Delivery"
                        )
                    )
                }
            }
        }
    }

    private fun initObservers() {
//        viewModel.deliveryNotAvailableDialog.observe(this) {
//            CustomAlertDialog(this).show()
//        }
        viewModel.uiEvent.observe(this) { event ->
            when (event) {
                is UIEvent.Toast -> showToast(this, event.message, event.duration)
                is UIEvent.SnackBar -> showErrorSnackBar(event.message, event.isError)
                is UIEvent.ProgressBar -> {
                    if (event.visibility) {
                        showProgressDialog(true)
                    } else {
                        hideProgressDialog()
                    }
                }
                is UIEvent.EmptyUIEvent -> return@observe
                else -> Unit
            }
            viewModel.setEmptyUiEvent()
        }

        viewModel.uiUpdate.observe(this@QuickOrderActivity) { event ->
            when (event) {
                is QuickOrderViewModel.UiUpdate.WalletData -> {
                    viewModel.wallet = event.wallet
                }
                is QuickOrderViewModel.UiUpdate.StartingTransaction -> {
                    showLoadStatusDialog("", "Processing payment from Wallet...", "transaction")
                }
                is QuickOrderViewModel.UiUpdate.PlacingOrder -> {
                    updateLoadStatusDialogText("placingOrder", event.message)

                }
                is QuickOrderViewModel.UiUpdate.OrderPlaced -> {
                    updateLoadStatusDialogText("success", event.message)
                    lifecycleScope.launch {
                        delay(1800)
                        dismissLoadStatusDialog()
                        viewModel.quickOrder?.let { startTotalOrderWorker(it.cart as MutableList<CartEntity>) }
                        if (viewModel.placeOrderByCOD) {
                            showExitSheet(
                                this@QuickOrderActivity,
                                "We have received your order. Once we verify all the product in the list, We will add all the products to your cart. You can track the progress from Purchase History",
                                "close"
                            )
                        }
                    }
                }
                is QuickOrderViewModel.UiUpdate.WalletTransactionFailed -> {
                    lifecycleScope.launch {
                        delay(1800)
                        dismissLoadStatusDialog()
                        showExitSheet(
                            this@QuickOrderActivity,
                            "Server Error! Transaction Failed. If you have already paid or money is deducted from wallet, Please contact customer support we will verify the transaction and refund the amount. Click CUSTOMER SUPPORT to open customer support",
                            "cs"
                        )
                    }
                }
                is QuickOrderViewModel.UiUpdate.OrderPlacementFailed -> {
                    viewModel.placeOrderByCOD = false
                    dismissLoadStatusDialog()
                    showExitSheet(
                        this,
                        "Server Error! Failed to place order. If you have already paid or money is deducted from wallet, Please contact customer support we will verify the transaction and refund the amount. Click CUSTOMER SUPPORT to open customer support",
                        "cs"
                    )
                }
                is QuickOrderViewModel.UiUpdate.BeginningUpload -> {
                    showLoadStatusDialog(
                        "",
                        "Starting to Upload your order List... Please wait",
                        "upload"
                    )
                }
                is QuickOrderViewModel.UiUpdate.UploadingImage -> {
                    updateLoadStatusDialogText("upload", event.message)
                }
                is QuickOrderViewModel.UiUpdate.UploadComplete -> {
                    /*
                    * This comes after the images are uploaded. So if the user chose COD then we move
                    * to COD method to place order with COD option. If COD is false that means the user has
                    * requested only for get estimate request
                    * */
                    if (viewModel.placeOrderByCOD) {
                        viewModel.placeCashOnDeliveryOrder(generateOrderDetailsMap())
                    } else {
                        updateLoadStatusDialogText("success", event.message)
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
                }
                is QuickOrderViewModel.UiUpdate.UploadFailed -> {
                    viewModel.placeOrderByCOD = false
                    dismissLoadStatusDialog()
                    showErrorSnackBar(event.message, true)
                }
                is QuickOrderViewModel.UiUpdate.DeletingImages -> {
                    showLoadStatusDialog(
                        "",
                        event.message,
                        "upload"
                    )
                }
                is QuickOrderViewModel.UiUpdate.DeletingQuickOrder -> {
                    /*
                    * This is the callback to delete quick order request from customer
                    * by using the delete icon in the expanded cart bottomsheet
                    * */
                    event.data?.let {
                        when (event.data) {
                            "order" -> updateLoadStatusDialogText("placingOrder", event.message)
                            "success" -> lifecycleScope.launch {
                                updateLoadStatusDialogText(
                                    "success",
                                    event.message
                                )
                                resetQuickOrderUI()
                                delay(1800)
                                dismissLoadStatusDialog()
                            }
                            else -> lifecycleScope.launch {
                                updateLoadStatusDialogText(
                                    "fail",
                                    event.message
                                )
                                delay(1800)
                                dismissLoadStatusDialog()
                            }
                        }
                    }
                }
                is QuickOrderViewModel.UiUpdate.ValidatingPurchase -> {
                    showLoadStatusDialog(
                        "",
                        "Validating Transaction...",
                        "purchaseValidation"
                    )
                }
                is QuickOrderViewModel.UiUpdate.AddressUpdate -> {
                    if (event.isSuccess) {
                        if (event.message == "update") {
                            showToast(this@QuickOrderActivity, "Address Updated")
                        }
                        event.address?.let { populateAddressDetails(it) }
                    } else {
                        showErrorSnackBar(event.message, true)
                    }
                }
                is QuickOrderViewModel.UiUpdate.EstimateData -> {
//                    isSuccess means there is estimate data available
                    if (event.isSuccess) {
                        event.data?.let {
                            //if data is not null there is estimate request available to be displayed
                            populateEstimateDetails(it)
                        } ?: let {
                            /*
                            * since null no previous request is available and we do the empty page animation
                            * we hide the cart bottom sheet
                            */
                            binding.clBody.visible()
                            binding.clBody.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@QuickOrderActivity,
                                    R.anim.slide_up
                                )
                            )
                            binding.clAddress.visible()
                            binding.clAddress.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@QuickOrderActivity,
                                    R.anim.slide_in_right_bounce
                                )
                            )
                            setCartBottom(null)
                            cartBottomSheet.isHideable = true
                            cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    } else {
                        showErrorSnackBar(event.message, true)
                    }
                }
                is QuickOrderViewModel.UiUpdate.PopulateOrderDetails -> {
                    populateOrderDetails(event.order)
                }
                is QuickOrderViewModel.UiUpdate.CouponApplied -> {
                    updateCheckoutText()
                    event.message?.let {
                        this.hideKeyboard()
                        showErrorSnackBar(it, false)
                        applyUiChangesWithCoupon(true)
                    } ?: applyUiChangesWithCoupon(false)
                }
                is QuickOrderViewModel.UiUpdate.UpdateCartData -> {
                    updateCheckoutText()
                    updateCartBadge()
                    event.count?.let {
                        cartAdapter.updateItemsCount(event.position, it)
                    } ?: cartAdapter.deleteCartItem(event.position)
                }
                is QuickOrderViewModel.UiUpdate.HowToVideo -> {
                    hideProgressDialog()
                    if (event.url == "") {
                        showToast(this, "demo video will be available soon. sorry for the inconvenience.")
                    } else {
                        openInBrowser(event.url)
                    }
                }
                is QuickOrderViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(url)
            startActivity(Intent.createChooser(intent, "Open link with"))
        } catch (e: Exception) {
            println("The current phone does not have a browser installed")
        }
    }

    private fun initRecyclerView() {
        quickOrderListAdapter = QuickOrderListAdapter(
            this,
            listOf(Uri.EMPTY),
            listOf(),
            true,
            this
        )
        binding.rvOrderList.layoutManager =
            GridLayoutManager(this, 3)
        binding.rvOrderList.adapter = quickOrderListAdapter

        quickOrderTextAdapter = QuickOrderTextAdapter(
            true,
            mutableListOf<QuickOrderTextItem>(),
            this
        )

        cartAdapter = CartAdapter(
            this,
            mutableListOf(),
            viewModel
        )
    }

    private fun setCartBottom(cartEntity: ArrayList<Cart>?) {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
        filterBtn = findViewById<ImageView>(R.id.ivFilter)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        cartBtn = findViewById(R.id.ivCart)

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartAdapter.cartItems = if (cartEntity == null) {
            mutableListOf<CartEntity>()
        } else {
            cartEntity.map { it.toCartEntity() } as MutableList<CartEntity>
        }
        cartAdapter.notifyDataSetChanged()
        cartRecycler.adapter = cartAdapter

        filterBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_wallet))

        updateCheckoutText()
        cartBottomSheet.isDraggable = true
        cartBottomSheet.isHideable = false

        cartEntity?.let { cart ->
            cartBottomSheet.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    updateCheckoutText()
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })

            cartBtn.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
            }

            filterBtn.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    if (cart.isNotEmpty() && checkoutText.text != "PURCHASE HISTORY") {
                        showExitSheet(
                            this,
                            "Do you wish to delete and Create a New Order",
                            "delete"
                        )
                    }
                } else {
                    if (!NetworkHelper.isOnline(this@QuickOrderActivity)) {
                        showErrorSnackBar("Please check your Internet Connection", true)
                        return@setOnClickListener
                    }
                    Intent(this, WalletActivity::class.java).also { intent ->
                        intent.putExtra(Constants.NAVIGATION, Constants.QUICK_ORDER)
                        startActivity(intent)
                    }
                }
            }

            checkoutBtn.setOnClickListener {
                if (NetworkHelper.isOnline(this)) {
                    when {
                        viewModel.quickOrder?.orderPlaced == true -> {
                            Intent(this, PurchaseHistoryActivity::class.java).also { intent ->
                                startActivity(intent)
                                finish()
                            }
                        }
                        cart.isEmpty() -> {
                            showErrorSnackBar("Estimate not yet available. Please wait", true)
                            return@setOnClickListener
                        }
                        else -> showListBottomSheet(
                            this@QuickOrderActivity,
                            arrayListOf<String>(
                                "Online",
                                "Wallet (Rs: ${viewModel.wallet?.amount})",
                                "Cash On Delivery"
                            )
                        )

                    }
                } else {
                    showErrorSnackBar("Please check network connection", true)
                }
            }
        }
    }

    private fun setBottomSheetIcon(content: String) {
        val icon = when (content) {
            "coupon" -> R.drawable.ic_coupon
            "delete" -> R.drawable.ic_delete
            "wallet" -> R.drawable.ic_wallet
            else -> R.drawable.ic_filter
        }
        filterBtn.fadOutAnimation(300)
        filterBtn.setImageDrawable(ContextCompat.getDrawable(this, icon))
        filterBtn.fadInAnimation(300)
        filterBtn.imageTintList =
            if (content == "delete") {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.matteRed))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_base))
            }
    }

    private fun addProductToQuickOrderTextList() {
        viewModel.selectedTextItemPosition?.let {
            binding.apply {
                when {
                    etproductName.text.toString().isNullOrEmpty() -> showToast(
                        this@QuickOrderActivity,
                        "Please provide a brief description of product name"
                    )
                    etVariantName.text.toString().isNullOrEmpty() -> showToast(
                        this@QuickOrderActivity,
                        "Please provide the product type (Eg: 1Kg)"
                    )
                    etQuantity.text.toString().isNullOrEmpty() -> showToast(
                        this@QuickOrderActivity,
                        "Please provide the quantity"
                    )
                    else -> {
                        viewModel.textOrderItemList[it].let { textItem ->
                            textItem.productName = etproductName.text.toString().trim()
                            textItem.variantName = etVariantName.text.toString().trim()
                            textItem.quantity = etQuantity.text.toString().trim().toInt()

                            quickOrderTextAdapter?.updateTextItem(it, textItem)

                            viewModel.selectedTextItemPosition = null
                            btnAddProduct.text = "Add Product"
                        }
                    }
                }
            }
        } ?: let {
            binding.apply {
                QuickOrderTextItem(
                    etproductName.text.toString().trim(),
                    etVariantName.text.toString().trim(),
                    etQuantity.text.toString().trim().toInt()
                ).let { textItem ->
                    viewModel.textOrderItemList.add(textItem)
                    quickOrderTextAdapter?.addTextItem(textItem)
                }
            }
        }

        binding.apply {
            etproductName.setText("")
            etVariantName.setText("")
            etQuantity.setText("")
            etproductName.requestFocus()
        }
    }

    fun setCurrentQuickOrderTypeSelection(selection: String) {
        binding.apply {
            when (selection) {
                "text" -> {
                    ivText.setColor(R.color.matteRed)
                    ivImage.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    ivVoice.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    rvOrderList.fadInAnimation()
                    rvOrderList.visible()
                    clTextTemplate.fadInAnimation()
                    clTextTemplate.visible()
                    clAudioTemplate.fadOutAnimation()
                    clAudioTemplate.remove()
                    viewModel.currentQuickOrderMode = "text"
                    viewModel.orderListUri.clear()
                    viewModel.orderListUri.add(Uri.EMPTY)
                    QuickOrderTextAdapter(
                        true,
                        mutableListOf(),
                        this@QuickOrderActivity
                    ).let { adapter ->
                        quickOrderTextAdapter = adapter
                        rvOrderList.adapter = adapter
                        rvOrderList.layoutManager = LinearLayoutManager(this@QuickOrderActivity)
                    }
                    ivDeleteRecording.performClick()
                }
                "image" -> {
                    ivImage.setColor(R.color.matteRed)
                    ivText.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    ivVoice.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    viewModel.currentQuickOrderMode = "image"
                    viewModel.textOrderItemList.clear()
                    rvOrderList.fadInAnimation()
                    rvOrderList.visible()
                    clTextTemplate.fadOutAnimation()
                    clTextTemplate.remove()
                    clAudioTemplate.fadOutAnimation()
                    clAudioTemplate.remove()
                    QuickOrderListAdapter(
                        this@QuickOrderActivity,
                        viewModel.orderListUri,
                        listOf(),
                        true,
                        this@QuickOrderActivity
                    ).let {
                        quickOrderListAdapter = it
                        rvOrderList.layoutManager =
                            GridLayoutManager(this@QuickOrderActivity, 3)
                        rvOrderList.adapter = quickOrderListAdapter
                    }
                    ivDeleteRecording.performClick()
                }
                else -> {
//                    ivVoice.imageTintList = ColorStateList.valueOf(
//                        ContextCompat.getColor(
//                            this@QuickOrderActivity,
//                            R.color.matteRed
//                        )
//                    )
                    ivVoice.setColor(R.color.matteRed)
                    ivImage.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    ivText.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    viewModel.currentQuickOrderMode = "voice"
                    viewModel.textOrderItemList.clear()
                    viewModel.orderListUri.clear()
                    viewModel.orderListUri.add(Uri.EMPTY)
                    clTextTemplate.fadOutAnimation()
                    clTextTemplate.remove()
                    rvOrderList.fadOutAnimation()
                    rvOrderList.remove()
                    clAudioTemplate.fadInAnimation()
                    clAudioTemplate.visible()
                    updateAudioLayout(true)
                }
            }
        }
    }

    //updating UI changes
    private fun validateEntry(): Boolean {
        return when (viewModel.currentQuickOrderMode) {
            "image" -> {
                if (viewModel.orderListUri.isNotEmpty() && viewModel.orderListUri[0] == Uri.EMPTY) {
                    showErrorSnackBar(
                        "Add your purchase list image to proceed",
                        true
                    )
                    false
                } else {
                    true
                }
            }
            "text" -> {
                if (viewModel.textOrderItemList.isEmpty()) {
                    showErrorSnackBar(
                        "Enter your purchase list data to proceed",
                        true
                    )
                    false
                } else {
                    true
                }
            }
            else -> {
                if (viewModel.fileName == null) {
                    showErrorSnackBar(
                        "Please record an audio containing your purchase list to proceed",
                        true
                    )
                    false
                } else {
                    true
                }
            }
        }
    }

    private fun resetQuickOrderUI() {
        binding.apply {
            applyUiChangesWithCoupon(false)
            etDeliveryNote.setText("")
            btnGetEstimate.visible()
            btnPlaceOrder.visible()
            ivImage.performClick()
            quickOrderListAdapter.quickOrderList = listOf()
            quickOrderListAdapter.quickOrderListUrl = listOf()
            quickOrderListAdapter.notifyDataSetChanged()
            cartAdapter.emptyCart()
            cartBtn.badgeValue = 0
        }
        viewModel.apply {
            orderListUri.clear()
            quickOrder = null
            orderID = null
            placeOrderByCOD = false
            couponAppliedPrice = null
            appliedCoupon = null
        }
//        updatePlaceOrderButton()
        cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun updateCheckoutText() {
        when (cartBottomSheet.state) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                if (viewModel.quickOrder?.orderPlaced == true) {
                    checkoutText.text = "PURCHASE HISTORY"
                } else {
                    checkoutText.setTextAnimation("CHECKOUT", 200)
                }
                setBottomSheetIcon("wallet")
            }
            BottomSheetBehavior.STATE_EXPANDED -> {
                lifecycleScope.launch {
                    if (viewModel.quickOrder?.orderPlaced == true) {
                        checkoutText.text = "PURCHASE HISTORY"
                    } else {
                        checkoutText.setTextAnimation(
                            "Rs: ${viewModel.couponAppliedPrice ?: viewModel.getTotalCartPrice()} + ${viewModel.getDeliveryCharge()}",
                            200
                        )
                        if (viewModel.quickOrder?.cart?.isNotEmpty() == true) {
                            setBottomSheetIcon("delete")
                        }
                    }
                }
            }
        }
    }

    private fun updateCartBadge() {
        viewModel.quickOrder?.let {
            cartBtn.badgeValue = viewModel.getCartItemsQuantity(it.cart)
        }
    }

    private fun applyUiChangesWithCoupon(isCouponApplied: Boolean) {
        binding.apply {
            if (isCouponApplied) {
                etCoupon.disable()
                btnApplyCoupon.text = "Remove"
                btnApplyCoupon.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        baseContext,
                        R.color.matteRed
                    )
                )
            } else {
                viewModel.couponAppliedPrice = null
                viewModel.appliedCoupon = null
                etCoupon.enable()
                etCoupon.setText("")
                btnApplyCoupon.text = "Apply"
                btnApplyCoupon.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        baseContext,
                        R.color.green_base
                    )
                )
            }
        }
    }

    private fun updateAudioLayout(newRecord: Boolean) {
        binding.apply {
            if (newRecord) {
                ivRecord.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@QuickOrderActivity,
                        R.drawable.ic_voice
                    )
                )
                ivPlayPause.fadOutAnimation(200)
                ivPlayPause.remove()
                seekBar.fadOutAnimation(200)
                seekBar.remove()
                ivRecord.fadInAnimation(300)
                ivRecord.visible()
                ivDeleteRecording.fadOutAnimation(300)
                ivDeleteRecording.remove()
            } else {
                ivPlayPause.fadInAnimation(300)
                ivPlayPause.setImage(R.drawable.ic_play)
                ivPlayPause.visible()
                seekBar.fadInAnimation(300)
                seekBar.visible()
                ivRecord.fadOutAnimation(200)
                ivRecord.remove()
                ivDeleteRecording.fadInAnimation(300)
                ivDeleteRecording.visible()
            }
        }
    }

    private fun updatePlayerLayout(playerState: String) {
        binding.apply {
            when (playerState) {
                "play" -> {
                    viewModel.player?.let {
                        it.seekTo(viewModel.lastProgress)
                        ivPlayPause.setImage(R.drawable.ic_pause)
                        seekBar.progress = viewModel.lastProgress
                        seekBar.max = it.duration
                        tvRecordTime.base = SystemClock.elapsedRealtime()
                        tvRecordTime.start()
                        viewModel.isPlaying = true
                        it.setOnCompletionListener {
                            updatePlayerLayout("stop")
                        }
                        seekBarUpdate()
                    }
                }
                "resume" -> {
                    viewModel.player?.let {
                        it.start()
                        it.seekTo(viewModel.lastProgress)
                        ivPlayPause.setImageDrawable(
                            ContextCompat.getDrawable(
                                this@QuickOrderActivity,
                                R.drawable.ic_pause
                            )
                        )
                        seekBar.progress = viewModel.lastProgress
                        tvRecordTime.base = SystemClock.elapsedRealtime() + viewModel.pausedTime
                        tvRecordTime.start()
                        viewModel.pausedTime = 0
                        viewModel.isPlaying = true
                        seekBarUpdate().start()
                    }
                }
                "pause" -> {
                    viewModel.player?.pause()
                    ivPlayPause.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@QuickOrderActivity,
                            R.drawable.ic_play
                        )
                    )
                    viewModel.isPlaying = false
                    viewModel.pausedTime = tvRecordTime.base - SystemClock.elapsedRealtime()
                    tvRecordTime.stop()
                    seekBarUpdate().cancel()
                }
                "quickOrder" -> {
                    ivPlayPause.setImage(R.drawable.ic_play)
//                    ivPlayPause.disable()
                    viewModel.isPlaying = false
                    tvRecordTime.stop()
                    ivDeleteRecording.remove()
                    tvRecordTime.base = SystemClock.elapsedRealtime()
                    viewModel.lastProgress = 0
                    seekBar.progress = viewModel.lastProgress
                    viewModel.player?.seekTo(viewModel.lastProgress)
                }
            }
        }
    }

    private fun seekBarUpdate() = lifecycleScope.launch {
        while (true) {
            viewModel.player?.let {
                val currentPosition = it.currentPosition
                viewModel.lastProgress = currentPosition
                binding.seekBar.progress = currentPosition
            }
            delay(1000)
        }
    }

    private fun loadNewImage() {
        if (viewModel.orderListUri.isEmpty()) {
            viewModel.orderListUri.add(Uri.EMPTY)
        }
        quickOrderListAdapter.quickOrderList = viewModel.orderListUri
        quickOrderListAdapter.notifyDataSetChanged()
//        updatePlaceOrderButton()
    }

    fun deleteQuickOrder() {
        viewModel.deleteQuickOrder()
    }

    private fun startRecording() {
        viewModel.fileName = "${getExternalFilesDir("/")?.absolutePath}/voice.m4a"
        viewModel.recorder = MediaRecorder()
        viewModel.recorder?.let {
            it.setAudioSource(MediaRecorder.AudioSource.MIC)
            it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            it.setAudioEncodingBitRate(16 * 44100)
            it.setAudioSamplingRate(44100)
            it.setOutputFile(viewModel.fileName)
            it.setAudioChannels(1)
            it.prepare()
            it.start()
        }
        showToast(this, "started")
        viewModel.lastProgress = 0
        binding.seekBar.progress = 0
        viewModel.isPlaying = true
        // making the imageView a stop button starting the chronometer
        binding.tvRecordTime.base = SystemClock.elapsedRealtime()
        binding.tvRecordTime.start()
        binding.ivRecord.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_stop))
    }

    private fun stopRecording() {
        try {
            viewModel.recorder!!.stop()
            viewModel.recorder!!.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        showToast(this, "stopped")
        viewModel.recorder = null
        viewModel.isPlaying = false
        //showing the play button
//        imgViewPlay.setImageResource(R.drawable.ic_play_circle)
        binding.tvRecordTime.base = SystemClock.elapsedRealtime()
        binding.tvRecordTime.stop()
        updateAudioLayout(false)
    }

    fun updateAddress() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        if (viewModel.quickOrder?.orderPlaced == true) {
            showErrorSnackBar("Can't edit Address. Order placed already", true)
            return
        }
        viewModel.userProfile?.let {
            val dialog = AddressDialog()
            val bundle = Bundle()
            bundle.putParcelable("address", it.address[0])
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "addressDialog")
        }
    }

    //load status dialog
    private fun showLoadStatusDialog(title: String, body: String, content: String) {
        LoadStatusDialog.newInstance(title, body, content).show(supportFragmentManager, LOAD_DIALOG)
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    private fun updateLoadStatusDialogText(filter: String, content: String) {
        LoadStatusDialog.statusContent = content
        LoadStatusDialog.statusText.value = filter
    }

    //populating data to views
    private fun populateAddressDetails(address: Address) {
        binding.apply {
            tvUserName.setTextAnimation(address.userId)
            tvAddressOne.setTextAnimation(address.addressLineOne)
            tvAddressTwo.setTextAnimation(address.addressLineTwo)
            tvAddressCity.setTextAnimation("${address.city} - ${address.LocationCode}")
        }
    }

    private fun populateEstimateDetails(quickOrder: QuickOrder) {
        viewModel.quickOrder = quickOrder
        binding.apply {
            when (quickOrder.orderType) {
                "text" -> {
                    if (quickOrder.orderPlaced || quickOrder.cart.isNullOrEmpty()) {
                        quickOrderTextAdapter?.isEditable = false
                    }
                    quickOrderTextAdapter?.let {
                        it.isEditable = false
                        it.setQuickOrderData(quickOrder.textItemsList)
                    }
                    rvOrderList.adapter = quickOrderTextAdapter
                    rvOrderList.layoutManager = LinearLayoutManager(this@QuickOrderActivity)
                    ivImage.remove()
                    ivVoice.remove()
                    ivText.setColor(R.color.matteRed)
                    rvOrderList.fadInAnimation()
                    rvOrderList.visible()
                    clTextTemplate.fadOutAnimation()
                    clTextTemplate.remove()
                    clAudioTemplate.fadOutAnimation()
                    clAudioTemplate.remove()
                    viewModel.currentQuickOrderMode = "text"
                }
                "image" -> {
                    quickOrderListAdapter.addImage = false
                    quickOrderListAdapter.quickOrderListUrl = quickOrder.imageUrl
                    quickOrderListAdapter.notifyDataSetChanged()
                    ivText.remove()
                    ivVoice.remove()
                    ivImage.setColor(R.color.matteRed)
                    viewModel.currentQuickOrderMode = "image"
                    viewModel.textOrderItemList.clear()
                    rvOrderList.fadInAnimation()
                    rvOrderList.visible()
                    clTextTemplate.fadOutAnimation()
                    clTextTemplate.remove()
                    clAudioTemplate.fadOutAnimation()
                    clAudioTemplate.remove()
                }
                else -> {
                    //todo logic to get audio file and play in the player
                    ivText.remove()
                    ivImage.remove()
                    ivVoice.setColor(R.color.matteRed)
                    viewModel.currentQuickOrderMode = "voice"
                    clTextTemplate.fadOutAnimation()
                    clTextTemplate.remove()
                    rvOrderList.fadOutAnimation()
                    rvOrderList.remove()
                    clAudioTemplate.fadInAnimation()
                    clAudioTemplate.visible()
                    updateAudioLayout(false)
                    updatePlayerLayout("quickOrder")
                }
            }
            setCartBottom(quickOrder.cart)
            updateCartBadge()
            clBody.visible()
            clBody.startAnimation(
                AnimationUtils.loadAnimation(
                    this@QuickOrderActivity,
                    R.anim.slide_up
                )
            )
            clAddress.visible()
            clAddress.startAnimation(
                AnimationUtils.loadAnimation(
                    this@QuickOrderActivity,
                    R.anim.slide_in_right_bounce
                )
            )
            btnGetEstimate.remove()
            btnPlaceOrder.remove()
        }
    }

    private fun populateOrderDetails(order: OrderEntity) {
        binding.apply {
            populateAddressDetails(order.address)
            etDeliveryNote.setText(order.deliveryNote)
            etCoupon.setText(order.appliedCoupon)
            if (order.appliedCoupon.isNotEmpty()) {
                applyUiChangesWithCoupon(true)
            }
            btnApplyCoupon.disable()
            hideProgressDialog()
            val preferences = resources.getStringArray(R.array.delivery_preference_array)
            for (i in preferences.indices) {
                if (preferences[i] == order.deliveryPreference) {
                    spDeliveryPreference.setSelection(i)
                    return
                }
            }
        }
    }

//    private fun updatePlaceOrderButton() {
//        if(
//            viewModel.quickOrder == null &&
//            viewModel.orderListUri.isNullOrEmpty()
//        ) {
//            binding.btnPlaceOrder.text = "Add List"
//        } else {
//            binding.btnPlaceOrder.text = "Place Order"
//        }
//    }

    //order and estimate placement
    fun selectedPaymentMode(paymentMethod: String) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        lifecycleScope.launch {
            when (paymentMethod) {
                "Online" -> {
                    if (viewModel.quickOrder == null) {
                        showExitSheet(
                            this@QuickOrderActivity,
                            "Generate Estimate to get the Total Price for the Items in the list to pay online or choose Cash on Delivery to place order immediately",
                            "okay"
                        )
                        return@launch
                    }
                    val mrp = (viewModel.couponAppliedPrice
                        ?: viewModel.getTotalCartPrice()) + viewModel.getDeliveryCharge()
                    viewModel.userProfile?.let {
                        startPayment(
                            this@QuickOrderActivity,
                            mailID = it.mailId,
                            mrp * 100f,
                            name = it.name,
                            userID = it.id,
                            phoneNumber = it.phNumber
                        ).also { status ->
                            if (!status) {
                                showToast(this@QuickOrderActivity, "Error in processing payment")
                            }
                        }
                    }
                }
                "Cash On Delivery" -> {
                    viewModel.placeOrderByCOD = true
                    viewModel.quickOrder?.let {
                        showLoadStatusDialog(
                            "",
                            "Validating your purchase...",
                            "purchaseValidation"
                        )
                        viewModel.placeCashOnDeliveryOrder(generateOrderDetailsMap())
                    } ?: sendEstimateRequest()
                }
                else -> {
                    if (viewModel.quickOrder == null) {
                        showExitSheet(
                            this@QuickOrderActivity,
                            "Generate Estimate to get the Total Price for the Items in the list to pay online or choose Cash on Delivery to place order immediately",
                            "okay"
                        )
                        return@launch
                    }
                    val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
                    viewModel.proceedForWalletPayment(
                        orderDetailsMap
                    )
                }
            }
        }
    }

    override fun onPaymentSuccess(response: String?) {
        val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
        orderDetailsMap["transactionID"] = response!!
        viewModel.placeOrderWithOnlinePayment(orderDetailsMap)
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun generateOrderDetailsMap(): HashMap<String, Any> {
        val orderDetailsMap = hashMapOf<String, Any>()
        orderDetailsMap["deliveryPreference"] = binding.spDeliveryPreference.selectedItem
        orderDetailsMap["deliveryNote"] = binding.etDeliveryNote.text.toString().trim()
        orderDetailsMap["appliedCoupon"] = if (viewModel.couponAppliedPrice != null) {
            binding.etCoupon.text.toString().trim()
        } else {
            ""
        }
        viewModel.userProfile?.let { profile ->
            orderDetailsMap["userID"] = profile.id
            orderDetailsMap["name"] = profile.name
            orderDetailsMap["phoneNumber"] = profile.phNumber
            orderDetailsMap["address"] = profile.address[0]
        }

        orderDetailsMap["orderID"] = viewModel.orderID ?: viewModel.quickOrder!!.orderID

        return orderDetailsMap
    }

    private fun startTotalOrderWorker(cartItems: MutableList<CartEntity>) {
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
                .setInputData(
                    workDataOf(
                        "cart" to cartToStringConverter(cartItems),
                        STATUS to true
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun cartToStringConverter(value: MutableList<CartEntity>): String {
        return Gson().toJson(value)
    }

    fun sendEstimateRequest() {
        val tempFileUriList = mutableListOf<Uri>()
        when (viewModel.currentQuickOrderMode) {
            "image" -> {
                for (uri in viewModel.orderListUri) {
                    compressImageToNewFile(this, uri)?.let { file ->
                        viewModel.tempFilesList.add(file)
                        tempFileUriList.add(file.toUri())
                    }
                }
                viewModel.sendGetEstimateRequest(
                    tempFileUriList
                )
            }
            else -> {
                viewModel.sendGetEstimateRequest(tempFileUriList)
            }
        }
    }

    fun moveToCustomerSupport() {
        Intent(this, QuickOrderActivity::class.java).also {
            startActivity(it)
        }
    }

    //permission
    private val getAction =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val newOrderImage = result.data?.data
            newOrderImage?.let {
                viewModel.addNewImageUri(newOrderImage)
                loadNewImage()
            }
        }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.STORAGE_PERMISSION_CODE -> {
                if (
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    showToast(this, "Storage Permission Granted")
                    if (viewModel.currentQuickOrderMode == "image") {
                        getAction.launch(pickImageIntent)
                    }
                } else {
                    showToast(this, "Storage Permission Denied")
                    showExitSheet(
                        this,
                        "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]",
                        "setting"
                    )
                }
            }
            Constants.AUDIO_PERMISSION_CODE -> {
                if (
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (PermissionsUtil.hasStoragePermission(this)) {
                        startRecording()
                    } else {
                        showExitSheet(
                            this,
                            "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions",
                            "permission"
                        )
                    }
                } else {
                    showToast(this, "Storage Permission Denied")
                    showExitSheet(
                        this,
                        "Permission Denied to record audio. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Audio]",
                        "setting"
                    )
                }
            }
        }
    }

    //from order list adapter
    override fun selectedListImage(position: Int, imageUri: Any, thumbnail: ShapeableImageView) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", imageUri.toString())
// }?:let {
//                intent.setData(imageUri.toString().toUri())
//            }
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    thumbnail,
                    ViewCompat.getTransitionName(thumbnail)!!
                )
            startActivity(intent, options.toBundle())
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
            showExitSheet(
                this,
                "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions",
                "permission"
            )
        }
    }

    override fun removeTextItem(position: Int) {
        viewModel.textOrderItemList.removeAt(position)
        quickOrderTextAdapter?.deleteTextItem(position)
    }

    override fun updateTextItem(position: Int) {
        viewModel.selectedTextItemPosition = position
        binding.apply {
            etproductName.setText(viewModel.textOrderItemList[position].productName)
            etVariantName.setText(viewModel.textOrderItemList[position].variantName)
            etQuantity.setText(viewModel.textOrderItemList[position].quantity.toString())
            btnAddProduct.text = "Update Product"
        }
    }

    //from address dialog
    override fun savedAddress(addressMap: HashMap<String, Any>) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.addressContainer?.let { address ->
            address.userId = addressMap["userId"].toString()
            address.addressLineOne = addressMap["addressLineOne"].toString()
            address.addressLineTwo = addressMap["addressLineTwo"].toString()
            address.LocationCode = addressMap["LocationCode"].toString()
            address.LocationCodePosition = addressMap["LocationCodePosition"].toString().toInt()
            address.city = addressMap["city"].toString()

            viewModel.updateAddress(address)
        }
    }

    override fun onBackPressed() {
        when {
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED ->
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        viewModel.userProfile = null
        viewModel.quickOrder = null
        viewModel.addressContainer = null
        viewModel.wallet = null
        viewModel.recorder?.stop()
        viewModel.player?.stop()
        viewModel.recorder = null
        viewModel.player = null
        viewModel.fileName = null
        viewModel.textOrderItemList.clear()
        viewModel.orderListUri.clear()
        viewModel.tempFilesList.clear()
        super.onDestroy()
    }

}





