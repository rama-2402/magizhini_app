package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.bind
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter.ShoppingMainAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityShoppingMainBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LIMITED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_HISTORY_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.setTextAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView
import java.util.*


class ShoppingMainActivity :
    BaseActivity(),
    KodeinAware,
    ShoppingMainAdapter.ShoppingMainListener {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityShoppingMainBinding
    private val factory: ShoppingMainViewModelFactory by instance()
    private lateinit var viewModel: ShoppingMainViewModel

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var checkoutText: TextView
    private var item: MenuItem? = null

    private lateinit var cartAdapter: CartAdapter
    private lateinit var adapter: ShoppingMainAdapter
    var categoryFilter: String = ALL_PRODUCTS
    private var mCartPrice: Float? = 0f
    private var isFiltered: Boolean = false

    private var mItems: MutableList<ProductEntity> = mutableListOf()
    private val mFilteredItems: MutableList<ProductEntity> = mutableListOf()
    private val mLimitedItems: MutableList<ProductEntity> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = setContentView(this, R.layout.activity_shopping_main)
        viewModel = ViewModelProvider(this, factory).get(ShoppingMainViewModel::class.java)
        binding.viewmodel = viewModel
        viewModel.shoppingMainListener = this

        //getting the intent to check the type of chip we want to see
        categoryFilter = intent.getStringExtra(Constants.CATEGORY).toString()
        viewModel.navigateToPage = intent.getStringExtra(NAVIGATION).toString()

        title = ""
        setSupportActionBar(binding.tbToolbar)
        binding.tvToolbarTitle.text = categoryFilter

        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check network connection", true)
        }

        showProgressDialog()
        initRecyclerView()
        observeLiveData()
        clickListeners()
        setCartBottom()
        checkProductsToDisplay()
    }

    private fun observeLiveData() {
        viewModel.position.observe(this) {
            adapter.products[it] = viewModel.productToRefresh
            adapter.notifyItemChanged(it)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getProfile()
        }
        viewModel.allProduct.observe(this) {
            viewModel.selectedChip = Constants.ALL
            viewModel.selectedCategory = ALL_PRODUCTS
            mItems = it as MutableList<ProductEntity>
            adapter.limited = false
            hideShimmer()
            adapter.setData(it)
            binding.cpAll.isChecked = true
        }
        viewModel.subscriptions.observe(this) {
            viewModel.selectedChip = Constants.SUBSCRIPTION
            viewModel.selectedCategory = ALL_PRODUCTS
            adapter.limited = false
            hideShimmer()
            adapter.setData(it as MutableList<ProductEntity>)
        }
        viewModel.allProductsInCategory.observe(this) {
            viewModel.selectedChip = Constants.CATEGORY
            viewModel.selectedCategory = categoryFilter
            adapter.limited = false
            hideShimmer()
            adapter.setData(it as MutableList<ProductEntity>)
        }
        viewModel.allFavorites.observe(this) {
            viewModel.selectedChip = Constants.FAVORITES
            viewModel.selectedCategory = ALL_PRODUCTS
            adapter.limited = false
            hideShimmer()
            adapter.setData(it as MutableList<ProductEntity>)
        }
        viewModel.discountAvailableProducts.observe(this) {
            viewModel.selectedChip = Constants.DISCOUNT
            viewModel.selectedCategory = ALL_PRODUCTS
            adapter.limited = false
            hideShimmer()
            adapter.setData(it as MutableList<ProductEntity>)
        }
        viewModel.getAllCartItems().observe(this) {
            if (it.isEmpty()) {
                cartBtn.badgeValue = 0
//                cartBtn.visibleBadge(false)
            } else {
                cartBtn.visibleBadge(true)
                var quantities = 0
                it.forEach { cart ->
                    quantities += cart.quantity
                }
                cartBtn.badgeValue = quantities
            }
            cartAdapter.setCartData(it as MutableList<CartEntity>)
        }
        viewModel.availableCategoryNames.observe(this) {
            showListBottomSheet(this, it as ArrayList<String>)
        }
        viewModel.getCartItemsPrice().observe(this) {
            mCartPrice = it ?: 0f
            if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                checkoutText.setTextAnimation("Rs: $mCartPrice", 200)
            }
        }
    }

    private fun checkProductsToDisplay() {
        //live data of the products and items in the list
        if (categoryFilter == Constants.ALL_PRODUCTS) {
            viewModel.getAllProductsStatic()
        } else {
            setFilteredProducts()
        }
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            rvShoppingItems.hide()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.remove()
            rvShoppingItems.visible()
        }
    }

    private fun clickListeners() {

        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.cgFilterChipGroup.setOnCheckedChangeListener { _, checkedId ->
            UIUtil.hideKeyboard(this)
            collapseSearchBar()
            when (checkedId) {
                R.id.cpAll -> {
                    binding.cpAll.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Product Store"
                    viewModel.getAllProductsStatic()
                }
                R.id.cpSubscriptions -> {
                    binding.cpSubscriptions.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Subscriptions"
                    viewModel.getAllSubscriptions()
                }
                R.id.cpCategoryFilter -> {
                    //kept this chip as the default check in xml so that the bottom sheet wont be
                    //triggered when a category of product was selected from home screen
                    binding.cpCategoryFilter.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    if (!isFiltered) {
                        openCategoryFilterDialog()
                    }
                }
                R.id.cpFavorites -> {
                    binding.cpFavorites.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Favorites"
                    viewModel.getAllFavoritesStatic()
                }
                R.id.cpDiscounts -> {
                    binding.cpDiscounts.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Discounts"
                    //Querying the discount items straight from the room database and setting it in the adapter
                    viewModel.getAllDiscountProducts()
                }
                R.id.cpLimitedItems -> {
                    binding.cpLimitedItems.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    binding.tvToolbarTitle.text = "Limited Items"
                    if(mLimitedItems.isEmpty()) {
                        showShimmer()
                        binding.flShimmerPlaceholder.startShimmer()
                        viewModel.limitedItemsFilter()
                    } else {
                        displayLimitedItems(mLimitedItems)
                    }
                }
            }
        }
    }

    private fun displayLimitedItems(products: MutableList<ProductEntity>) {
        isFiltered = false
        adapter.limited = true
        viewModel.selectedChip = LIMITED
        adapter.setData(products)
    }

    private fun openCategoryFilterDialog() {
        viewModel.getAllCategoryNames()
    }

    private fun collapseSearchBar() {
        if (item?.isActionViewExpanded == true) {
            item!!.collapseActionView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        item = menu?.findItem(R.id.btnSearch)
        item?.icon?.setTint(ContextCompat.getColor(this, R.color.white))
        val searchView = item?.actionView as androidx.appcompat.widget.SearchView

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                    var searchJob: Job? = Job()
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(500)
                        mFilteredItems.clear()
                        val searchText = newText!!.lowercase(Locale.getDefault())
                        if (searchText.isNotEmpty()) {
                            mItems.forEach loop@ { it ->
                                if (it.name.lowercase().contains(searchText) || it.description.lowercase().contains(searchText)) {
                                    mFilteredItems.add(it)
                                } else {
                                    for (label in it.labels) {
                                        if (label.lowercase().contains(searchText)) {
                                            mFilteredItems.add(it)
                                            return@loop
                                        }
                                    }
                                }
                            }
                            adapter.products = mFilteredItems
                            adapter.notifyDataSetChanged()
                        } else {
                            mFilteredItems.clear()
                            binding.cpAll.isChecked = true
                            adapter.products = mItems
                            adapter.notifyDataSetChanged()
                        }
                    }
                searchJob = null
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    fun setFilteredProducts() {
        showShimmer()
        binding.flShimmerPlaceholder.startShimmer()
        binding.tvToolbarTitle.text = categoryFilter
        isFiltered = true
        binding.cpCategoryFilter.isChecked = true
//        hideListBottomSheet()
        viewModel.getAllProductByCategoryStatic(categoryFilter)
        if (categoryFilter == SUBSCRIPTION) {
            binding.cpSubscriptions.isChecked = true
//            viewModel.getAllSubscriptions()
        }
    }

    private fun initRecyclerView() {

        cartAdapter = CartAdapter(
            this,
            mutableListOf(),
            viewModel
        )

        adapter = ShoppingMainAdapter(
            this,
            mutableListOf(),
            false,
            this
        )
        binding.rvShoppingItems.layoutManager = LinearLayoutManager(this)
        binding.rvShoppingItems.adapter = adapter

        hideProgressDialog()
    }

    private fun setCartBottom() {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
        val filterBtn = findViewById<ImageView>(R.id.ivFilter)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        cartBtn = findViewById(R.id.ivCart)

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartRecycler.adapter = cartAdapter

        cartBottomSheet.isDraggable = true

        cartBottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        checkoutText.setTextAnimation("Rs: $mCartPrice", 200)
//                        checkoutText.text = "Rs: $mCartPrice"
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        checkoutText.setTextAnimation("CHECKOUT", 200)
//                        checkoutText.text = "CHECKOUT"
                    }
                }
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
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            openCategoryFilterDialog()
        }

        checkoutBtn.setOnClickListener {
            if (NetworkHelper.isOnline(this)) {
                Intent(this, InvoiceActivity::class.java).also {
                    it.putExtra(NAVIGATION, viewModel.selectedCategory)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    onPause()
                }
            } else {
                showErrorSnackBar("Please check network connection", true)
            }
        }
    }

//    private fun getOnlyAvailableProducts(products: List<ProductEntity>?): List<ProductEntity> {
//        val prodList = arrayListOf<ProductEntity>()
//        products!!.forEach { product ->
//            if (mCategoriesList.contains(product.category)) {
//                prodList.add(product)
//            }
//        }
//        return prodList
//    }

    private fun navigateToPreviousPage() {
        when(viewModel.navigateToPage) {
            ORDER_HISTORY_PAGE -> {
                Intent(this, PurchaseHistoryActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
            else -> {
                Intent(this, HomeActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    finishAffinity()
                }
            }
        }
    }

    private fun navigateToProductDetails(product: ProductEntity) {
        if (NetworkHelper.isOnline(this)) {
            if (product.productType == SUBSCRIPTION) {
                Intent(this, SubscriptionProductActivity::class.java).also {
                    it.putExtra(Constants.PRODUCTS, product.id)
                    it.putExtra(Constants.PRODUCT_NAME, product.name)
                    it.putExtra(NAVIGATION, viewModel.selectedCategory)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    onPause()
                }
            } else {
                Intent(this, ProductActivity::class.java).also {
                    it.putExtra(Constants.PRODUCTS, product.id)
                    it.putExtra(Constants.PRODUCT_NAME, product.name)
                    it.putExtra(NAVIGATION, viewModel.selectedCategory)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    onPause()
                }
            }
        } else {
            showErrorSnackBar("Please check network connection", true)
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
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            navigateToPreviousPage()
        }
    }

    override fun updateFavorites(id: String, product: ProductEntity, position: Int) {
        viewModel.updateFavorites(id, product, position)
    }

    override fun navigateToProduct(product: ProductEntity) {
        navigateToProductDetails(product)
    }

    override fun upsertCartItem(
        product: ProductEntity,
        position: Int,
        variant: String,
        count: Int,
        price: Float,
        originalPrice: Float,
        variantIndex: Int,
        maxOrderQuantity: Int
    ) {
        viewModel.upsertCartItem(product, position, variant, count, price, originalPrice, variantIndex, maxOrderQuantity)
    }

    override fun deleteCartItemFromShoppingMain(product: ProductEntity, variantName: String, position: Int) {
        viewModel.deleteCartItemFromShoppingMain(product, variantName, position)
    }

    //updating the limited items products list
    override fun limitedItemList(products: List<ProductEntity>) {
        hideShimmer()
        displayLimitedItems(products = products as MutableList<ProductEntity>)
    }
}