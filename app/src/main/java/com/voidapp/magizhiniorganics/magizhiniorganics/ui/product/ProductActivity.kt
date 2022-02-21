package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.ProductViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.math.abs
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.HOME_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LIMITED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NO_LIMIT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_HISTORY_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.OUT_OF_STOCK
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCT_NAME
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import ru.nikartm.support.ImageBadgeView

/*
* todo
*  set up coupon code as a dialog with more details
* */

class ProductActivity :
    BaseActivity(),
    KodeinAware
{
    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityProductBinding

    private val factory: ProductViewModelFactory by instance()
    private lateinit var viewModel: ProductViewModel

    private var cartBottomSheet: BottomSheetBehavior<LinearLayout> = BottomSheetBehavior()
    private lateinit var checkoutText: TextView
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var filterBtn: ImageView
    private lateinit var cartAdapter: CartAdapter

    private lateinit var dialogAddCouponBs: BottomSheetDialog

    private var isPreviewVisible: Boolean = false

    companion object {
        const val ANIMATION_DURATION: Long = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_product)
        viewModel = ViewModelProvider(this, factory).get(ProductViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.navigateToPage = intent.getStringExtra(NAVIGATION).toString()

        setSupportActionBar(binding.tbCollapsedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        binding.tvProductName.text = intent.getStringExtra(PRODUCT_NAME).toString()
        binding.tvProductName.isSelected = true

        initData(intent.getStringExtra(PRODUCTS).toString())
        cartBottomSheet()
        initLiveData()
        initViewPager()
        initClickListeners()
    }

    private fun initData(productID: String) {
        viewModel.getProfileData()
        viewModel.getProductByID(productID)
        viewModel.getAllCartItem()
    }

    private fun cartBottomSheet() {
        val bottomSheet = findViewById<LinearLayout>(R.id.clBottomCart)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        filterBtn = findViewById(R.id.ivFilter)
        cartBtn = findViewById(R.id.ivCart)
        checkoutText = findViewById(R.id.tvCheckOut)

        setBottomSheetIcon("coupon")

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartAdapter = CartAdapter(
            this,
            arrayListOf(),
            viewModel
        )

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartRecycler.adapter = cartAdapter

        cartBottomSheet.isDraggable = true

        cartBottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        checkoutText.setTextAnimation(
                            "Rs: ${viewModel.getCartPrice(viewModel.cartItems)}",
                            ANIMATION_DURATION
                        )
                        setBottomSheetIcon("delete")
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        checkoutText.setTextAnimation("CHECKOUT", ANIMATION_DURATION)
                        setBottomSheetIcon("coupon")
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        cartBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        }
        filterBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                viewModel.clearCart()
            } else {
                showAddCouponDialog()
            }
        }
        checkoutBtn.setOnClickListener {
            if (NetworkHelper.isOnline(this)) {
                Intent(this, InvoiceActivity::class.java).also {
                    it.putExtra(NAVIGATION, PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
            } else {
                showErrorSnackBar("Please check network connection", true)
            }
        }
    }

    private fun setBottomSheetIcon(content: String) {
        val icon =  when(content) {
            "coupon" -> R.drawable.ic_coupon
            "delete" -> R.drawable.ic_delete
            else -> R.drawable.ic_filter
        }
        filterBtn.setImageDrawable(ContextCompat.getDrawable(this, icon))
        filterBtn.imageTintList =
            if (content == "delete") {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.matteRed))
        } else {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_base))
        }
    }

    private fun showAddCouponDialog() {
        //BS to add referral number
        dialogAddCouponBs = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogBottomAddReferralBinding = DataBindingUtil.inflate(LayoutInflater.from(applicationContext),R.layout.dialog_bottom_add_referral,null,false)
        dialogAddCouponBs.setCancelable(true)
        dialogAddCouponBs.setContentView(view.root)
        dialogAddCouponBs.dismissWithAnimation = true

        //if coupon is applied then the edt filed and the button view will be chaged accordingly
        viewModel.currentCoupon?.let { coupon ->
            with(view) {
                etReferralNumber.setText(coupon.code)
                etReferralNumber.disable()
                btnApply.setBackgroundColor(ContextCompat.getColor(this@ProductActivity, R.color.matteRed))
                btnApply.text = "Remove"
            }
        }

        view.etReferralNumber.inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
        view.etlReferralNumber.hint = "Coupon Code"

        //verifying if the referral number is empty and assigning it to the userProfile object
        view.btnApply.setOnClickListener {
            val code = view.etReferralNumber.text.toString().trim()
            if (code.isEmpty()) {
                view.etlReferralNumber.error = "* Enter a valid code"
                return@setOnClickListener
            }
            if (viewModel.currentCoupon == null) {
                viewModel.verifyCoupon(code)
                dialogAddCouponBs.dismiss()
            } else {
                applyUiChangesWithCoupon(false, view)
                dialogAddCouponBs.dismiss()
                Toast.makeText(this, "Coupon Removed", Toast.LENGTH_SHORT).show()
            }
        }
        dialogAddCouponBs.show()
    }

    private fun applyUiChangesWithCoupon(isCouponApplied: Boolean, view: DialogBottomAddReferralBinding?) {
        binding.apply {
            if (isCouponApplied) {
                view?.apply {
                    etReferralNumber.disable()
                    etReferralNumber.setText(viewModel.currentCoupon?.code ?: "")
                    btnApply.text = "Remove"
                    btnApply.setBackgroundColor(
                        ContextCompat.getColor(
                            baseContext,
                            R.color.matteRed
                        )
                    )
                }
                populateProductData()
            } else {
                viewModel.couponPrice = null
                viewModel.currentCoupon = null
                setPrice(viewModel.product!!.variants[viewModel.selectedVariantPosition])
                view?.apply {
                    etReferralNumber.enable()
                    etReferralNumber.setText("")
                    btnApply.text = "Apply"
                    btnApply.setBackgroundColor(
                        ContextCompat.getColor(
                            baseContext,
                            R.color.green_base
                        )
                    )
                }
            }
        }
    }

    private fun initLiveData() {
        viewModel.uiUpdate.observe(this) { event ->
            when(event) {
                is ProductViewModel.UiUpdate.PopulateProductData -> {
                    event.product?.let {
                        binding.spProductVariant.setSelection(viewModel.selectedVariantPosition)
                        populateProductData()
                        refreshLimitedItemCount()
                    } ?: showErrorSnackBar(event.message!!, true)
                }
                is ProductViewModel.UiUpdate.UpdateLimitedItemCount -> {
                    refreshLimitedItemCount()
                }
                is ProductViewModel.UiUpdate.UpdateFavorites -> {
                    setFavorites(event.isFavorite)
                }
                is ProductViewModel.UiUpdate.CheckStoragePermission -> {
                    if (PermissionsUtil.hasStoragePermission(this)) {
                        showToast(this, "Storage Permission Granted")
                        viewModel.previewImage("granted")
                    } else {
                        showExitSheet(this, "The App Needs Storage Permission to access Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                    }
                }
                is ProductViewModel.UiUpdate.OpenPreviewImage -> {
                    isPreviewVisible = true
                    val url = event.imageUrl ?: event.imageUri
                    with(binding) {
                        GlideLoader().loadUserPictureWithoutCrop(this@ProductActivity, url!!, ivPreviewImage)
                        ivPreviewImage.visible()
                        ivPreviewImage.startAnimation(AnimationUtils.loadAnimation(this@ProductActivity, R.anim.scale_big))
                    }
                }
                is ProductViewModel.UiUpdate.CouponApplied -> {
                    event.message?.let {
                        applyUiChangesWithCoupon(true, null)
                        viewModel.setNullCoupon()
                    }
                }
                is ProductViewModel.UiUpdate.PopulateCartData -> {
                    event.cartItems?.let {
                        updateBadgeValue()
                        cartAdapter.setCartData(it as MutableList<CartEntity>)
                    } ?: showToast(this, "Looks like your cart is Empty")
                }
                is ProductViewModel.UiUpdate.UpdateCartData -> {
                    updateBadgeValue()
                    event.count?.let {
                        cartAdapter.updateItemsCount(event.position, event.count)
                    } ?: let {
                        cartAdapter.deleteCartItem(event.position)
                        setAddButton()
                    }
                }
                is ProductViewModel.UiUpdate.AddCartItem -> {
                    updateBadgeValue()
                    event.cartEntity?.let {
                        cartAdapter.addCartItem(it)
                    } ?: showErrorSnackBar("Failed to add in Cart", true)
                }
                is ProductViewModel.UiUpdate.CartCleared -> {
                    cartAdapter.emptyCart()
                    updateBadgeValue()
                    populateProductData()
                    refreshLimitedItemCount()
                }
                is ProductViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }

        viewModel.uiEvent.observe(this) { event ->
            when(event) {
                is UIEvent.Toast -> showToast(this, event.message, event.duration)
                is UIEvent.SnackBar -> showErrorSnackBar(event.message, event.isError)
                is UIEvent.ProgressBar -> {
                    if (event.visibility) {
                        showProgressDialog()
                    } else {
                        hideProgressDialog()
                    }
                }
                is UIEvent.EmptyUIEvent -> return@observe
                else -> Unit
            }
            viewModel.setEmptyUiEvent()
        }
    }

    private fun updateBadgeValue() {
        cartBtn.badgeValue = viewModel.cartItemsCount
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            checkoutText.setTextAnimation(
                "Rs: ${viewModel.getCartPrice(viewModel.cartItems)}",
                ANIMATION_DURATION
            )
        } else {
            checkoutText.setTextAnimation(
                "CHECKOUT",
                ANIMATION_DURATION
            )
        }
    }

    private fun initClickListeners() {
        binding.spProductVariant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                variantposition: Int,
                id: Long
            ) {
                viewModel.selectedVariantPosition = variantposition
                viewModel.updateVariantName(variantposition)
                setPrice(viewModel.product!!.variants[variantposition])
                refreshLimitedItemCount()
                setAddButtonContent(
                    viewModel.selectedVariantName
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setAddButtonContent(
                    viewModel.selectedVariantName
                )
            }
        }

        binding.ivFavourite.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            viewModel.updateFavorites()
        }

        binding.btnAdd.setOnClickListener {
            if (binding.btnAdd.text == "Add") {
                addToCart()
            } else if (binding.btnAdd.text == "Remove") {
                removeFromCart()
            }
        }

        binding.ivWallet.setOnClickListener {
            Intent(this, WalletActivity::class.java).also {
                it.putExtra(NAVIGATION, PRODUCTS)
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        binding.tbAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            title = if (abs(verticalOffset) -appBarLayout.totalScrollRange == 0) {
                viewModel.product?.name
            } else {
                ""
            }
        })

        binding.ivPreviewImage.setOnClickListener {
            onBackPressed()
        }
    }

    private fun populateProductData() {
        viewModel.product?.let { product ->
            GlideLoader().loadUserPicture(this, product.thumbnailUrl, binding.ivProductThumbnail)
            setPrice(product.variants[viewModel.selectedVariantPosition])
            setFavorites(product.favorite)
            setVariantAdapter(product.variants)
            setAddButtonContent(
                viewModel.selectedVariantName
            )
        }
    }

    private fun setFavorites(isFavorite: Boolean) {
        //setting the favorties icon for the products
        if (isFavorite) {
            binding.ivFavourite.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            binding.ivFavourite.setImageResource(R.drawable.ic_favorite_outline)
        }
    }

    private fun setVariantAdapter(productVariants: ArrayList<ProductVariant>) {
        val variants = arrayListOf<String>()
        productVariants.indices.forEach { i ->
            variants.add(viewModel.getVariantName(i))
        }
        val spinnerAdapter = ArrayAdapter(
            binding.spProductVariant.context,
            R.layout.support_simple_spinner_dropdown_item,
            variants
        )
        binding.spProductVariant.adapter = spinnerAdapter
    }

    private fun setAddButtonContent(variantName: String) {
        viewModel.product?.let {
            if (it.variantInCart.contains(variantName)) {
                setRemoveButton()
            } else {
                setAddButton()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLimitedItemCount() {
        val variant = viewModel.product!!.variants[viewModel.selectedVariantPosition]
        when(variant.status) {
            NO_LIMIT -> {
                binding.tvLimited.hide()
            }
            OUT_OF_STOCK -> {
                binding.tvLimited.visible()
                binding.tvLimited.text = "Out of Stock "
            }
            LIMITED -> {
                binding.tvLimited.visible()
                binding.tvLimited.text = "(Only ${variant.inventory} in Stock) "
            }
            else -> {
                binding.tvLimited.hide()
            }
        }
    }

    private fun setPrice(variant: ProductVariant) {
            if (
                variant.discountPrice != 0.0
            ) {
                binding.apply {
                    tvOriginalPrice.setTextAnimation(
                        "Rs: ${variant.variantPrice}",
                        ANIMATION_DURATION
                    )
                    tvOriginalPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    tvDiscount.fadInAnimation(ANIMATION_DURATION)
                    tvDiscountedPrice.setTextAnimation(
                        "Rs: ${viewModel.getSelectedItemPrice()}",
                        ANIMATION_DURATION
                    )
                    tvDiscountPercent.setTextAnimation(
                        "${viewModel.getDiscountPercent(variant.variantPrice.toFloat(), variant.discountPrice.toFloat())}% Off",
                        ANIMATION_DURATION
                    )
                }
            } else {
                binding.apply {
                    tvOriginalPrice.fadOutAnimation(ANIMATION_DURATION)
                    tvOriginalPrice.remove()
                    tvDiscount.fadOutAnimation(ANIMATION_DURATION)
                    tvDiscountPercent.fadOutAnimation(ANIMATION_DURATION)
                    tvDiscountedPrice.setTextAnimation("Rs. ${viewModel.getSelectedItemPrice()}")
                    viewModel.currentCoupon?.let {
                        tvOriginalPrice.setTextAnimation(
                            "Rs: ${variant.variantPrice}",
                            ANIMATION_DURATION
                        )
                        tvOriginalPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    }

                }
            }
    }


    private fun addToCart() {
        if(viewModel.isProductAvailable()) {
            viewModel.upsertCartItem()
            setRemoveButton()
        } else {
            showErrorSnackBar("Product Out of Stock", true)
        }
    }

    private fun removeFromCart() {
        viewModel.deleteProductFromCart()
        setAddButton()
    }

    private fun setRemoveButton() {
        with(binding.btnAdd) {
            text = "Remove"
            setIconResource(R.drawable.ic_delete)
            setBackgroundColor(ContextCompat.getColor(baseContext, R.color.matteRed))
        }
    }

    private fun setAddButton() {
        with(binding.btnAdd) {
            text = "Add"
            setIconResource(R.drawable.ic_add)
            setBackgroundColor(ContextCompat.getColor(baseContext, R.color.green_base))
        }
    }

    private fun initViewPager() {
        val adapter = ProductViewPager(supportFragmentManager, lifecycle)
        binding.vpFragmentContent.adapter = adapter
        TabLayoutMediator(binding.tlTabLayout, binding.vpFragmentContent) { tab, position ->
            when(position) {
                0 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_about_us)
                1 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_reviews)
                2 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_write_review)
            }
        }.attach()
    }

    private fun navigateToPreviousPage() {
        when(viewModel.navigateToPage) {
            HOME_PAGE -> {
                Intent(this, HomeActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    finishAffinity()
                }
            }
            ORDER_HISTORY_PAGE -> finish()
            else -> {
                Intent(this, ShoppingMainActivity::class.java).also {
                    it.putExtra(Constants.CATEGORY, viewModel.navigateToPage)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    finishAffinity()
                }
            }
        }
    }

    //Title bar back button press function
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        when {
            isPreviewVisible -> {
                binding.ivPreviewImage.startAnimation(AnimationUtils.loadAnimation(this@ProductActivity, R.anim.scale_small))
                binding.ivPreviewImage.visibility = View.GONE
                isPreviewVisible = false
            }
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED -> cartBottomSheet.state =
                BottomSheetBehavior.STATE_COLLAPSED
            else -> {
                navigateToPreviousPage()
            }
        }
    }

    override fun onDestroy() {
        viewModel.apply {
            userProfile = null
            product = null
            currentCoupon = null
        }
        super.onDestroy()
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

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
                viewModel.previewImage("granted")
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }
}

