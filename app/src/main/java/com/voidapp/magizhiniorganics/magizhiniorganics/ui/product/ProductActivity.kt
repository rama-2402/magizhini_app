package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.BestSellersAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.ProductViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LIMITED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NO_LIMIT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.OUT_OF_STOCK
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCT_NAME
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView
import kotlin.math.abs


class ProductActivity :
    BaseActivity(),
    KodeinAware,
    ReviewAdapter.ReviewItemClickListener,
        BestSellersAdapter.BestSellerItemClickListener
{
    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityProductBinding

    private val factory: ProductViewModelFactory by instance()
    private lateinit var viewModel: ProductViewModel

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var checkoutText: TextView
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var filterBtn: ImageView
    private lateinit var cartAdapter: CartAdapter

    private lateinit var dialogAddCouponBs: BottomSheetDialog

    companion object {
        const val ANIMATION_DURATION: Long = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_product)
        viewModel = ViewModelProvider(this, factory)[ProductViewModel::class.java]

        setSupportActionBar(binding.tbCollapsedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.tbCollapsedToolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.matteRed))
        title = ""
        binding.tvProductName.text = intent.getStringExtra(PRODUCT_NAME).toString()
        binding.tvProductName.isSelected = true

        viewModel.productID = intent.getStringExtra(PRODUCTS).toString()

        postponeEnterTransition()

        initData()
        cartBottomSheet()
        initLiveData()
        initViewPager()
        initClickListeners()

        binding.apply {
            clProductDetails.startAnimation(AnimationUtils.loadAnimation(this@ProductActivity, R.anim.slide_in_right_bounce))
            llViewPager.startAnimation(AnimationUtils.loadAnimation(this@ProductActivity, R.anim.slide_up))
        }
    }

    private fun initData() {
        viewModel.reviewAdapter = ReviewAdapter(
            this,
            arrayListOf(),
            this
        )
        viewModel.getProfileData()
    }

    override fun onStart() {
        super.onStart()
        viewModel.cartItemsCount = 0
        viewModel.getProductByID()
        viewModel.getAllCartItem()
    }

    private fun cartBottomSheet() {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
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

        cartBottomSheet.isHideable = false
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
                showToast(this, "No Coupon Available")
//                showAddCouponDialog()
            }
        }
        checkoutBtn.setOnClickListener {
            if (NetworkHelper.isOnline(this)) {
                updatePreferenceData()
                Intent(this, InvoiceActivity::class.java).also {
                    it.putExtra(NAVIGATION, PRODUCTS)
                    startActivity(it)
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
                        populateProductData(true)
                        refreshLimitedItemCount()
                    } ?: showErrorSnackBar(event.message!!, true)
                }
                is ProductViewModel.UiUpdate.PopulateSimilarProducts -> populateSimilarProducts()
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
//                is ProductViewModel.UiUpdate.OpenPreviewImage -> {
//                    val url = event.imageUri
//                    Intent(this, PreviewActivity::class.java).also { intent ->
//                        intent.putExtra("url", url!!.toString())
//                        intent.putExtra("contentType", "image")
//                        val options: ActivityOptionsCompat =
//                            ActivityOptionsCompat.makeSceneTransitionAnimation(this, event.thumbnail, ViewCompat.getTransitionName(event.thumbnail)!!)
//                        startActivity(intent, options.toBundle())
//                    }
////                    cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
////                    isPreviewVisible = true
////                    val url = event.imageUrl ?: event.imageUri
////                    with(binding) {
////                        GlideLoader().loadUserPictureWithoutCrop(this@ProductActivity, url!!, ivPreviewImage)
////                        ivPreviewImage.visible()
////                        ivPreviewImage.startAnimation(AnimationUtils.loadAnimation(this@ProductActivity, R.anim.scale_big))
////                    }
//                }
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
                is ProductViewModel.UiUpdate.HowToVideo -> {
                    hideProgressDialog()
                    if (event.url == "") {
                        showToast(this, "demo video will be available soon. sorry for the inconvenience.")
                    } else {
                        openInBrowser(event.url)
                    }
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
    }

    private fun populateSimilarProducts() {
        binding.apply {
            BestSellersAdapter(
                this@ProductActivity,
                viewModel.similarProducts,
                this@ProductActivity
            ).also {
                rvProducts.adapter = it
                rvProducts.layoutManager = LinearLayoutManager(this@ProductActivity, LinearLayoutManager.HORIZONTAL, false)
            }
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
        KeyboardVisibilityEvent.setEventListener(this
        ) { isOpen ->
            if (isOpen) {
                cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

//        binding.ivHowTo.setOnClickListener {
//            showProgressDialog(true)
//            viewModel.getHowToVideo("Product")
//        }

        binding.spProductQuantity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
               setPrice(viewModel.product!!.variants[binding.spProductVariant.selectedItemPosition])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                setPrice(viewModel.product!!.variants[binding.spProductVariant.selectedItemPosition])
            }
        }

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
                viewModel.product?.let {
                    updateNumberOfItemsFromCart(it.name, "${it.variants[variantposition].variantName} ${it.variants[variantposition].variantType}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setAddButtonContent(
                    viewModel.selectedVariantName
                )
                viewModel.product?.let {
                    updateNumberOfItemsFromCart(it.name, "${it.variants[0].variantName} ${it.variants[0].variantType}")
                }
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
            updatePreferenceData()
            if (!NetworkHelper.isOnline(this)) {
                showErrorSnackBar("Please check your Internet Connection", true)
                return@setOnClickListener
            }
            Intent(this, WalletActivity::class.java).also {
                it.putExtra(NAVIGATION, PRODUCTS)
                startActivity(it)
            }
        }

        binding.tbAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            title = if (abs(verticalOffset) -appBarLayout.totalScrollRange == 0) {
                viewModel.product?.name
            } else {
                ""
            }
        })
        binding.tlTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position) {
                    0 -> {
                        tab.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.matteRed))
                        binding.tlTabLayout.getTabAt(1)?.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.green_base))
                        binding.tlTabLayout.getTabAt(2)?.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.green_base))
                    }
                    1 -> {
                        tab.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.matteRed))
                        binding.tlTabLayout.getTabAt(0)?.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.green_base))
                        binding.tlTabLayout.getTabAt(2)?.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.green_base))
                    }
                    2 -> {
                        tab.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.matteRed))
                        binding.tlTabLayout.getTabAt(0)?.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.green_base))
                        binding.tlTabLayout.getTabAt(1)?.icon?.setTint(ContextCompat.getColor(this@ProductActivity, R.color.green_base))
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
    }

    private fun updateNumberOfItemsFromCart(productName: String, variantName: String) {
        binding.apply {
            viewModel.cartItems.isNotEmpty().let {
                for(cart in viewModel.cartItems) {
                    if (cart.productName == productName && variantName == cart.variant) {
                        spProductQuantity.setSelection(cart.quantity - 1)
                        return
                    }
                }
                spProductQuantity.setSelection(0)
            }
        }
    }

    private fun populateProductData(isFirstCall: Boolean = false) {
        viewModel.product?.let { product ->
            if (isFirstCall) {
                window.sharedElementEnterTransition = android.transition.TransitionSet()
                    .addTransition(android.transition.ChangeImageTransform())
                    .addTransition(android.transition.ChangeBounds())
                    .apply {
                        doOnEnd { binding.ivProductThumbnail.loadImg(product.thumbnailUrl) {
                            startPostponedEnterTransition()
                        } }
                    }
            }
            binding.ivProductThumbnail.loadImg(product.thumbnailUrl) {
                startPostponedEnterTransition()
            }
            setPrice(product.variants[viewModel.selectedVariantPosition])
            setFavorites(product.favorite)
            setVariantAdapter(product.variants)
            setAddButtonContent(
                viewModel.selectedVariantName
            )
//            startPostponedEnterTransition()
        }
    }

    private fun setFavorites(isFavorite: Boolean) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        //setting the favorties icon for the products
        if (isFavorite) {
            binding.ivFavourite.setImageResource(R.drawable.ic_favorite_filled)
            binding.ivFavourite.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.matteRed))
        } else {
            binding.ivFavourite.setImageResource(R.drawable.ic_favorite_outline)
            binding.ivFavourite.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_base))
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
                        "Rs: ${variant.variantPrice * spProductQuantity.selectedItem.toString().toInt()}",
                        ANIMATION_DURATION
                    )
                    tvOriginalPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    tvDiscount.fadInAnimation(ANIMATION_DURATION)
                    tvDiscountedPrice.setTextAnimation(
                        "Rs: ${viewModel.getSelectedItemPrice() * spProductQuantity.selectedItem.toString().toInt()}",
                        ANIMATION_DURATION
                    )
                    tvDiscountPercent.setTextAnimation(
                        "${viewModel.getDiscountPercent(variant.variantPrice.toFloat(), variant.discountPrice.toFloat()).toInt()}% Off",
                        ANIMATION_DURATION
                    )
                }
            } else {
                binding.apply {
                    tvOriginalPrice.fadOutAnimation(ANIMATION_DURATION)
                    tvOriginalPrice.remove()
                    tvDiscount.fadOutAnimation(ANIMATION_DURATION)
                    tvDiscountPercent.fadOutAnimation(ANIMATION_DURATION)
                    tvDiscountedPrice.setTextAnimation("Rs. ${viewModel.getSelectedItemPrice() * spProductQuantity.selectedItem.toString().toInt()}")
                    viewModel.currentCoupon?.let {
                        tvOriginalPrice.setTextAnimation(
                            "Rs: ${variant.variantPrice * spProductQuantity.selectedItem.toString().toInt()}",
                            ANIMATION_DURATION
                        )
                        tvOriginalPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    }

                }
            }
    }


    private fun addToCart() {
        if(viewModel.isProductAvailable()) {
            viewModel.upsertCartItem(binding.spProductQuantity.selectedItem.toString().toInt())
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
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(baseContext, R.color.matteRed))
        }
    }

    private fun setAddButton() {
        with(binding.btnAdd) {
            text = "Add"
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(baseContext, R.color.green_base))
        }
    }

    private fun initViewPager() {
        val adapter = ProductViewPager(supportFragmentManager, lifecycle)
        binding.vpFragmentContent.adapter = adapter
        TabLayoutMediator(binding.tlTabLayout, binding.vpFragmentContent) { tab, position ->
            when(position) {
                0 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_about_us)
                    tab.icon?.setTint(ContextCompat.getColor(this, R.color.matteRed))
                }
                1 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_reviews)
//                    tab.icon?.setTint(ContextCompat.getColor(this, R.color.green_base))
                }
                2 -> {
                    tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_write_review)
//                    tab.icon?.setTint(ContextCompat.getColor(this, R.color.green_base))
                }
            }
        }.attach()
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

    private fun updatePreferenceData() {
        val productIDString = SharedPref(this).getData(PRODUCTS, Constants.STRING, "").toString()
        val productIDs: MutableList<String> = if (productIDString != "") {
            productIDString.split(":").map { it } as MutableList<String>
        } else {
            mutableListOf<String>()
        }
        if (viewModel.clearedIDsFromCart.isNotEmpty()) {
            viewModel.clearedIDsFromCart.forEach {
                if (!productIDs.contains(it)) {
                    productIDs.add(it)
                }
            }
//            productIDs.addAll(viewModel.clearedIDsFromCart.distinct())
            viewModel.clearedIDsFromCart.clear()
        }
        if (!productIDs.contains(viewModel.productID)) {
            productIDs.add(viewModel.productID)
        }
        SharedPref(this).putData(PRODUCTS, Constants.STRING, productIDs.joinToString(":")).toString()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isFinishing) {
            Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
        }
        super.onStop()
    }

    override fun onBackPressed() {
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            updatePreferenceData()
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        viewModel.apply {
            userProfile = null
            product = null
            currentCoupon = null
            reviewAdapter = null
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

    override fun previewImage(url: String, thumbnail: ShapeableImageView) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, thumbnail, ViewCompat.getTransitionName(thumbnail)!!)
            startActivity(intent, options.toBundle())
        }
    }

    override fun moveToProductDetails(
        productID: String,
        productName: String,
        thumbnail: ShapeableImageView
    ) {
        binding.tvProductName.text = productName
        binding.tvProductName.isSelected = true
        viewModel.productID = productID
        binding.spProductVariant.setSelection(0)
        binding.spProductQuantity.setSelection(0)
        viewModel.selectedVariantPosition = 0
        viewModel.getProductByID()
    }
}

