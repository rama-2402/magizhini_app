package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.ProductViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.math.abs
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.flow.collect


//TODO CHECK LIMITED ITEMS COUNT BEFORE ORDERING MULTIPLE

class ProductActivity :
    BaseActivity(),
    KodeinAware
{

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityProductBinding

    private val factory: ProductViewModelFactory by instance()
    private lateinit var viewModel: ProductViewModel

    private var userId: String = ""

    private var mCartItems: List<CartEntity> = listOf()
    private var mProduct: ProductEntity = ProductEntity()
    private var mProductId: String = ""
    private var mProductName: String = ""
    private var mVariants: ArrayList<String> = arrayListOf()
    private var mSelectedVariant: Int = 0
    private var mFinalPrice: Float = 0f
    private var mCartPrice: Float? = 0f

    private var cartBottomSheet: BottomSheetBehavior<LinearLayout> = BottomSheetBehavior()
    private lateinit var cartAdapter: CartAdapter
    private lateinit var checkoutText: TextView

    private lateinit var dialogAddCouponBs: BottomSheetDialog
    private var mCoupon: CouponEntity = CouponEntity()
    private var isCouponApplied: Boolean = false

    private var isPreviewVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_product)
        viewModel = ViewModelProvider(this, factory).get(ProductViewModel::class.java)
        binding.viewmodel = viewModel

        mProductId = intent.getStringExtra(Constants.PRODUCTS).toString()
        mProductName = intent.getStringExtra(Constants.PRODUCT_NAME).toString()
        viewModel.productID = mProductId

        setSupportActionBar(binding.tbCollapsedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        binding.tvProductName.text = mProductName
        binding.tvProductName.isSelected = true

        cartBottomSheet()
        initLiveData()
        initViewPager()
        initClickListeners()

        //getting the current user id so that favorites can be added to firestore data
        userId = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()
    }

    @SuppressLint("SetTextI18n")
    private fun cartBottomSheet() {
        val bottomSheet = findViewById<LinearLayout>(R.id.clBottomCart)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartBtn = findViewById<ImageView>(R.id.ivCart)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        val filterBtn: ImageView = findViewById(R.id.ivFilter)
        checkoutText = findViewById(R.id.tvCheckOut)

        //TODO NEED TO CHANGE THE ICON OF THE FILTERBTN TO COUPON

        checkoutText.text = "Rs: $mCartPrice"
        filterBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_coupon))

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
                        checkoutText.text = "Rs: $mCartPrice"
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        checkoutText.text = "CHECKOUT"
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
            showAddCouponDialog()
        }
        checkoutBtn.setOnClickListener {
            if (NetworkHelper.isOnline(this)) {
                Intent(this, InvoiceActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                }
            } else {
                showErrorSnackBar("Please check network connection", true)
            }
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
        if (isCouponApplied) {
            with(view) {
                etReferralNumber.setText(mCoupon.code)
                etReferralNumber.isEnabled = false
                btnApply.setBackgroundColor(ContextCompat.getColor(this@ProductActivity, R.color.matteRed))
                btnApply.text = "Remove"
            }
        }

        view.etReferralNumber.inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
        view.etlReferralNumber.hint = "Coupon Code"

        //verifying if the referral number is empty and assigning it to the userProfile object
        view.btnApply.setOnClickListener {
            val code = view.etReferralNumber.text.toString().trim()

            when {
                code.isEmpty() -> {
                    view.etlReferralNumber.error = "* Enter a valid code"
                    return@setOnClickListener
                }
                isCouponApplied -> {
                    //if coupn is  applied that means the next apply trigger will be to remove the coupon.
                    //So when remove is clciked it will set the live data to false and close the BS.
                    //observer will trigger the price change and all that is with it.
                    viewModel.removeCouponCode()
                    dialogAddCouponBs.dismiss()
                    Toast.makeText(this, "Coupon Removed", Toast.LENGTH_SHORT).show()
                }
                !isCouponApplied -> {
                    if(viewModel.isCouponAvailable(code, mProduct.category)) {
                        view.etlReferralNumber.isErrorEnabled = false
                        Toast.makeText(this, "Coupon Applied", Toast.LENGTH_SHORT).show()
                        view.btnApply.text = "Remove"
                        view.btnApply.setBackgroundColor(
                            ContextCompat.getColor(
                                this,
                                R.color.matteRed
                            )
                        )
                        UIUtil.hideKeyboard(this)
                        dialogAddCouponBs.dismiss()
                    } else {
                        view.etlReferralNumber.error =
                            "Coupon expired or does not exist. Try different code"
                    }
                }
            }
        }
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED)
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        dialogAddCouponBs.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initLiveData() {

        viewModel.getProfileData()
        viewModel.getAllCoupons(Constants.ACTIVE)

        viewModel.getProductById(mProductId).observe(this, {
            viewModel.product = it
            mProduct = it
            setProductDetailsToScreen()
            refreshLimitedItemCount()
            viewModel.setUpProductListener(it.id)
            viewModel.getProductReviews(it.id)
        })

        viewModel.itemCount.observe(this, {
            mProduct.variants.clear()
            mProduct.variants.addAll(it)
            refreshLimitedItemCount()
        })

        viewModel.getAllCartItems().observe(this, { it ->
            mCartItems = it
            cartAdapter.setCartData(mCartItems)
            setCheckoutText()
        })

        viewModel.getCartItemsPrice().observe(this, {
            if (it != null) {
                mCartPrice = it
                if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                    checkoutText.text = "Rs: $mCartPrice"
                }
            } else {
                mCartPrice = 0f
                if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                    checkoutText.text = "Rs: $mCartPrice"
                }
            }
        })

        //getting all the coupons
        viewModel.couponIndex.observe(this, {
            mCoupon = viewModel.coupons[it]
        })

        viewModel.isCouponApplied.observe(this, {
                isCouponApplied = it
                setPrice(mSelectedVariant)
        })

        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when(result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> showProgressDialog()
                    else -> Unit
                }
            }
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
                mSelectedVariant = variantposition
                setPrice(mSelectedVariant)
                refreshLimitedItemCount()
                setAddButtonContent(getVariantName(mSelectedVariant))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                setAddButtonContent(getVariantName(mSelectedVariant))
            }
        }

        binding.ivFavourite.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            mProduct.favorite = !mProduct.favorite
            viewModel.updateFavorites(userId, mProduct)
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
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        binding.tbAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            title = if (abs(verticalOffset) -appBarLayout.totalScrollRange == 0) {
                mProductName
            } else {
                ""
            }
        })

        binding.ivPreviewImage.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setCheckoutText() {
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED) {
            checkoutText.text = "CHECKOUT"
        } else {
            checkoutText.text = "Rs: $mCartPrice"
        }
    }

    private fun setProductDetailsToScreen() {
        GlideLoader().loadUserPicture(this, mProduct.thumbnailUrl, binding.ivProductThumbnail)
        binding.tvOriginalPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        setPrice(mSelectedVariant)
        setFavorites()
        setVariantAdapter()
        setAddButtonContent(getVariantName(mSelectedVariant))
    }

    private fun setFavorites() {
        //setting the favorties icon for the products
        if (mProduct.favorite) {
            binding.ivFavourite.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            binding.ivFavourite.setImageResource(R.drawable.ic_favorite_outline)
        }
    }

    private fun setVariantAdapter() {
        if (mVariants.isEmpty()) {
            mProduct.variants.indices.forEach { i ->
                mVariants.add(getVariantName(i))
            }
            val spinnerAdapter = ArrayAdapter(
                binding.spProductVariant.context,
                R.layout.support_simple_spinner_dropdown_item,
                mVariants
            )
            binding.spProductVariant.adapter = spinnerAdapter
        }
    }

    private fun setAddButtonContent(variant: String) {
        if (mProduct.variantInCart.contains(variant)) {
            setRemoveButton()
        } else {
            setAddButton()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLimitedItemCount() {
        val variant = mProduct.variants[mSelectedVariant]
        when(variant.status) {
            Constants.NO_LIMIT -> {
                binding.tvLimited.hide()
            }
            Constants.OUT_OF_STOCK -> {
                binding.tvLimited.visible()
                binding.tvLimited.text = "Out of Stock "
            }
            Constants.LIMITED -> {
                binding.tvLimited.visible()
                binding.tvLimited.text = "(Only ${variant.inventory} in Stock) "
            }
            else -> {
                binding.tvLimited.hide()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setPrice(position: Int) {
        if (mProduct.discountAvailable && mProduct.variants[position].discountPrice != 0.0) {
            binding.tvOriginalPrice.visible()
            binding.tvDiscount.visible()
            binding.tvDiscountPercent.visible()
            binding.tvOriginalPrice.text = "Rs. ${getVariantOriginalPrice(position)} "
//            binding.tvDiscountedPrice.text = "Rs. ${viewModel.getDiscountedPrice(mProduct, position)} "
            //we get the discounted price from viewmodel if there is any and passing it to check if there is coupon discount available
            //here we are checking if the coupon is applied. If applied then we will update with coupon amount else
            //we return the same original amount with or without discount
            mFinalPrice = updatedPriceWithCouponApplied(getVariantPrice(position))
            binding.tvDiscountedPrice.text = "Rs. $mFinalPrice"
            setDiscountPercent()
        } else {
            binding.tvOriginalPrice.remove()
            binding.tvDiscount.remove()
            binding.tvDiscountPercent.remove()
            binding.tvPrice.text = "MRP : "
            mFinalPrice = updatedPriceWithCouponApplied(getVariantPrice(position))
            binding.tvDiscountedPrice.text = "Rs. $mFinalPrice "
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setDiscountPercent() {
        val variant = mProduct.variants[mSelectedVariant]
        //if the variant has discount then that percent will be displayed if not the
        //products general discount percent will be displayed

        if (variant.discountPrice != 0.0) {
            binding.tvDiscountPercent.text = "${getDiscountPercent(variant.variantPrice.toFloat(), variant.discountPrice.toFloat())}% Off"
        }
    }

    private fun getDiscountPercent(price: Float, discountPrice: Float): Float
        = ((price-discountPrice)/price)*100

    private fun getVariantName(position: Int): String
        = "${mProduct.variants[position].variantName} ${mProduct.variants[position].variantType}"


    private fun getVariantOriginalPrice(position: Int)
        = mProduct.variants[position].variantPrice.toFloat()

    private fun getVariantPrice(position: Int): Float {
        return if (mProduct.variants[position].discountPrice == 0.0) {
            mProduct.variants[position].variantPrice.toFloat()
        } else {
            mProduct.variants[position].discountPrice.toFloat()
        }
    }


    private fun addToCart() {
        if(isProductAvailable()) {
            viewModel.upsertCartItem(
                mProductId,
                mProduct.name,
                mProduct.thumbnailUrl,
                getVariantName(mSelectedVariant),
                1,
                mFinalPrice ,
                getVariantOriginalPrice(mSelectedVariant),
                mCoupon.code,
                mProduct.variants[mSelectedVariant].inventory,
                mSelectedVariant
            )
            mProduct.variantInCart.add(getVariantName(mSelectedVariant))
            viewModel.upsertProduct(mProduct)
            setRemoveButton()
        } else {
            showErrorSnackBar("Product Out of Stock", true)
        }
    }

    private fun isProductAvailable(): Boolean {
        return mProduct.variants[mSelectedVariant].status != Constants.OUT_OF_STOCK
    }

    private fun removeFromCart() {
        viewModel.deleteProductFromCart(mProduct, getVariantName(mSelectedVariant))
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

    private fun updatedPriceWithCouponApplied(itemPrice: Float): Float {
        return if (isCouponApplied) {
            if (mCoupon.type == "Percentage") {
                (itemPrice - (itemPrice * mCoupon.amount.toFloat()) / 100).toFloat()
            } else {
                (itemPrice - mCoupon.amount.toFloat())
            }
        } else {
            itemPrice
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
                binding.ivPreviewImage.startAnimation(Animations.scaleSmall)
                binding.ivPreviewImage.visibility = View.GONE
                isPreviewVisible = false
            }
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED -> cartBottomSheet.state =
                BottomSheetBehavior.STATE_COLLAPSED
            else -> {
                Intent(this, ShoppingMainActivity::class.java).also {
                    it.putExtra(Constants.CATEGORY, Constants.ALL_PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
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

    private fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "toast" -> showToast(this, data as String)
            "permission" -> {
                if (PermissionsUtil.hasStoragePermission(this)) {
                    showToast(this, "Storage Permission Granted")
                    viewModel.previewImage("granted")
                } else {
                    showExitSheet(this, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                }
            }
            "review" -> {
                hideProgressDialog()
                showToast(this, data as String)
                viewModel.previewImage("added")
            }
            "preview" -> {
                isPreviewVisible = true
                with(binding) {
                    GlideLoader().loadUserPictureWithoutCrop(this@ProductActivity, data as String, ivPreviewImage)
                    ivPreviewImage.visible()
                    ivPreviewImage.startAnimation(Animations.scaleBig)
                }
            }
        }
        viewModel.setEmptyResult()
    }

    private fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "toast" -> showToast(this, data as String)
            "review" -> {
                hideProgressDialog()
                showToast(this, data as String)
            }
        }
    }
}

