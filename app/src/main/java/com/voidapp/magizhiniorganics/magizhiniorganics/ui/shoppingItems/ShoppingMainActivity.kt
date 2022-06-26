package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
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
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CategoryHomeAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter.ShoppingMainAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityShoppingMainBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.ItemsBottomSheet
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CATEGORY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.DISCOUNT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.FAVORITES
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LIMITED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
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
    ShoppingMainAdapter.ShoppingMainListener,
    CategoryHomeAdapter.CategoryItemClickListener {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityShoppingMainBinding
    private val factory: ShoppingMainViewModelFactory by instance()
    private lateinit var viewModel: ShoppingMainViewModel

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var checkoutText: TextView
    private lateinit var filterBtn: ImageView
    private var item: MenuItem? = null

    private lateinit var cartAdapter: CartAdapter
    private lateinit var adapter: ShoppingMainAdapter

    //    var categoryFilter: String = ALL_PRODUCTS
    private var mCartPrice: Float? = 0f
    private var isFiltered: Boolean = false

    private var mItems: MutableList<ProductEntity> = mutableListOf()
    private val mLimitedItems: MutableList<ProductEntity> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = setContentView(this, R.layout.activity_shopping_main)
        viewModel = ViewModelProvider(this, factory).get(ShoppingMainViewModel::class.java)
        binding.viewmodel = viewModel
        viewModel.shoppingMainListener = this

        //getting the intent to check the type of chip we want to see
        viewModel.categoryFilter = intent.getStringExtra(Constants.CATEGORY).toString()

        title = ""
        setSupportActionBar(binding.tbToolbar)
        binding.tvToolbarTitle.text = viewModel.categoryFilter

        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check network connection", true)
        }

        initRecyclerView()
        observeLiveData()
        clickListeners()
        setCartBottom()
        checkProductsToDisplay()
    }

    override fun onResume() {
        super.onResume()
        val productIDString = SharedPref(this).getData(PRODUCTS, STRING, "").toString()
        if (productIDString != "") {
            val productIDs: List<String> = productIDString.split(":")
            viewModel.updateProducts(productIDs)
            SharedPref(this).putData(PRODUCTS, STRING, "").toString()
        }
    }

    private fun observeLiveData() {
        viewModel.position.observe(this) { position ->
            viewModel.productToRefresh?.let { product ->
                adapter.products[position] = product
                adapter.notifyItemChanged(position)
            }
        }

        viewModel.changedPositions.observe(this) {
            for (i in viewModel.changedProductsPositions.indices) {
                adapter.products[viewModel.changedProductsPositions[i]] =
                    viewModel.changedProducts[i]
                adapter.notifyItemChanged(viewModel.changedProductsPositions[i])
            }
        }

        viewModel.howToVideo.observe(this) { url ->
            url?.let {
                hideProgressDialog()
                if (it == "") {
                    showToast(this, "demo video will be available soon. sorry for the inconvenience.")
                } else {
                    openInBrowser(it)
                    viewModel.setNullHowTo()
                }
            }

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
        }
        viewModel.subscriptions.observe(this) {
            viewModel.selectedChip = Constants.SUBSCRIPTION
            viewModel.selectedCategory = ALL_PRODUCTS
            mItems = it as MutableList<ProductEntity>
            adapter.limited = false
            hideShimmer()
            adapter.setData(it)
        }
        viewModel.allProductsInCategory.observe(this) {
            viewModel.selectedChip = CATEGORY
            viewModel.selectedCategory = viewModel.categoryFilter
            mItems = it as MutableList<ProductEntity>
            adapter.limited = false
            hideShimmer()
            adapter.setData(it)
        }
        viewModel.allFavorites.observe(this) {
            viewModel.selectedChip = Constants.FAVORITES
            viewModel.selectedCategory = ALL_PRODUCTS
            mItems = it as MutableList<ProductEntity>
            adapter.limited = false
            hideShimmer()
            adapter.setData(it)
        }
        viewModel.discountAvailableProducts.observe(this) {
            viewModel.selectedChip = Constants.DISCOUNT
            viewModel.selectedCategory = ALL_PRODUCTS
            mItems = it as MutableList<ProductEntity>
            adapter.limited = false
            hideShimmer()
            adapter.setData(it)
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
        viewModel.getALlCategories().observe(this) {
            CategoryHomeAdapter(
                this,
                it,
                this
            ).let { adapter ->
                viewModel.categoriesAdapter = ItemsBottomSheet(
                    this,
                    null,
                    adapter
                )
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

    private fun checkProductsToDisplay() {
        //live data of the products and items in the list
        when (viewModel.categoryFilter) {
            ALL_PRODUCTS -> binding.cpAll.isChecked = true
            SUBSCRIPTION -> binding.cpSubscriptions.isChecked = true
            FAVORITES -> binding.cpFavorites.isChecked = true
            DISCOUNT -> binding.cpDiscounts.isChecked = true
            LIMITED -> binding.cpLimitedItems.isChecked = true
            else -> selectedCategory(viewModel.categoryFilter)
        }
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            binding.flShimmerPlaceholder.startShimmer()
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

        KeyboardVisibilityEvent.setEventListener(
            this
        ) { isOpen ->
            if (isOpen) {
                cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.ivHowTo.setOnClickListener {
            showProgressDialog(true)
            viewModel.howToVideo("Main")
        }

        binding.rvShoppingItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                when {
                    dy > 0 -> {
                        cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                        binding.fabUp.visibility = View.VISIBLE
                    }
                    dy < 0 -> {
                        binding.fabUp.visibility = View.INVISIBLE
                        cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    binding.fabUp.visibility = View.INVISIBLE
                    cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        })

        binding.fabUp.setOnClickListener {
            binding.rvShoppingItems.smoothScrollToPosition(0)
        }

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
                    binding.svHorizontalChipScroll.fullScroll(View.FOCUS_LEFT)
                    binding.tvToolbarTitle.text = "Magizhini Store"
                    viewModel.getAllProductsStatic()
                }
                R.id.cpSubscriptions -> {
                    binding.cpSubscriptions.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.tvToolbarTitle.text = "Subscriptions"
                    viewModel.getAllSubscriptions()
                }
                R.id.cpCategoryFilter -> {
                    //kept this chip as the default check in xml so that the bottom sheet wont be
                    //triggered when a category of product was selected from home screen
//                    binding.svHorizontalChipScroll.fullScroll(View.FOCUS_RIGHT)
                    binding.cpCategoryFilter.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    if (!isFiltered) {
                        openCategoryFilterDialog()
                    }
                    binding.svHorizontalChipScroll.fullScroll(View.FOCUS_RIGHT)
                }
                R.id.cpFavorites -> {
                    binding.cpFavorites.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.tvToolbarTitle.text = "Favorites"
                    viewModel.getAllFavoritesStatic()
                }
                R.id.cpDiscounts -> {
                    binding.cpDiscounts.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    showShimmer()
                    isFiltered = false
                    binding.tvToolbarTitle.text = "Discounts"
                    //Querying the discount items straight from the room database and setting it in the adapter
                    viewModel.getAllDiscountProducts()
                }
                R.id.cpLimitedItems -> {
                    binding.cpLimitedItems.also {
                        it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                    }
                    binding.tvToolbarTitle.text = "Limited Items"
                    if (mLimitedItems.isEmpty()) {
                        showShimmer()
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
        viewModel.categoriesAdapter?.let {
            it.show()
        }
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

        searchView.findViewById<ImageView>(R.id.search_close_btn)
            .setColorFilter(ContextCompat.getColor(this, R.color.matteRed))
        searchView.findViewById<TextView>(R.id.search_src_text)
            .setTextColor(ContextCompat.getColor(this, R.color.matteRed))

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
                    viewModel.currentProductsList.clear()
                    val searchText = newText!!.lowercase(Locale.getDefault())
                    if (searchText.isNotEmpty()) {
                        mItems.forEach loop@{ it ->
                            if (it.name.lowercase()
                                    .contains(searchText) || it.description.lowercase()
                                    .contains(searchText)
                            ) {
                                viewModel.currentProductsList.add(it)
                            } else {
                                for (label in it.labels) {
                                    if (label.lowercase().contains(searchText)) {
                                        viewModel.currentProductsList.add(it)
                                        return@loop
                                    }
                                }
                            }
                        }
                        adapter.setData(viewModel.currentProductsList.map { it.copy() } as MutableList<ProductEntity>)
                    } else {
                        viewModel.currentProductsList.clear()
                        adapter.setData(mItems.map { it.copy() } as MutableList<ProductEntity>)
                    }
                }
                searchJob = null
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
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
        binding.rvShoppingItems.layoutManager = GridLayoutManager(this, 2)
        binding.rvShoppingItems.adapter = adapter
        binding.rvShoppingItems.setHasFixedSize(true)
    }

    private fun setCartBottom() {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
        filterBtn = findViewById<ImageView>(R.id.ivFilter)
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
                        setBottomSheetIcon("delete")
                        checkoutText.setTextAnimation("Rs: $mCartPrice", 200)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        setBottomSheetIcon("filter")
                        checkoutText.setTextAnimation("CHECKOUT", 200)
                        if (!viewModel.removedProductIDsFromCart.isNullOrEmpty()) {
                            viewModel.updateProducts(viewModel.removedProductIDsFromCart)
                        }
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
            if (cartBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED) {
                openCategoryFilterDialog()
            } else {
                viewModel.clearCart(cartAdapter.cartItems)
            }
        }

        checkoutBtn.setOnClickListener {
            if (NetworkHelper.isOnline(this)) {
                Intent(this, InvoiceActivity::class.java).also {
                    it.putExtra(NAVIGATION, viewModel.selectedCategory)
                    startActivity(it)
                }
            } else {
                showErrorSnackBar("Please check network connection", true)
            }
        }
    }

    private fun setBottomSheetIcon(content: String) {
        val icon = when (content) {
            "filter" -> R.drawable.ic_filter
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

//    private fun getOnlyAvailableProducts(products: List<ProductEntity>?): List<ProductEntity> {
//        val prodList = arrayListOf<ProductEntity>()
//        products!!.forEach { product ->
//            if (mCategoriesList.contains(product.category)) {
//                prodList.add(product)
//            }
//        }
//        return prodList
//    }

    private fun navigateToProductDetails(product: ProductEntity, thumbnail: ShapeableImageView) {
        if (NetworkHelper.isOnline(this)) {
            if (product.productType == SUBSCRIPTION) {
                Intent(this, SubscriptionProductActivity::class.java).also {
                    it.putExtra(Constants.PRODUCTS, product.id)
                    it.putExtra(Constants.PRODUCT_NAME, product.name)
                    val options: ActivityOptionsCompat =
                        ViewCompat.getTransitionName(thumbnail)?.let { it1 ->
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this, thumbnail,
                                it1
                            )
                        }!!
                    startActivity(it, options.toBundle())
                }
            } else {
                Intent(this, ProductActivity::class.java).also {
                    it.putExtra(Constants.PRODUCTS, product.id)
                    it.putExtra(Constants.PRODUCT_NAME, product.name)
                    it.putExtra(NAVIGATION, viewModel.selectedCategory)
                    val options: ActivityOptionsCompat =
                        ViewCompat.getTransitionName(thumbnail)?.let { it1 ->
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this, thumbnail,
                                it1
                            )
                        }!!
                    startActivity(it, options.toBundle())
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
        when {
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED ->
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            viewModel.selectedChip != ALL -> binding.cpAll.isChecked = true
            else -> super.onBackPressed()
        }
    }

    override fun updateFavorites(id: String, product: ProductEntity, position: Int) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.updateFavorites(id, product, position)
    }

    override fun navigateToProduct(
        product: ProductEntity,
        thumbnail: ShapeableImageView,
        position: Int
    ) {
        navigateToProductDetails(product, thumbnail)
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
        viewModel.upsertCartItem(
            product,
            position,
            variant,
            count,
            price,
            originalPrice,
            variantIndex,
            maxOrderQuantity
        )
    }

    override fun deleteCartItemFromShoppingMain(
        product: ProductEntity,
        variantName: String,
        position: Int
    ) {
        viewModel.deleteCartItemFromShoppingMain(product, variantName, position)
    }

    //updating the limited items products list
    override fun limitedItemList(products: List<ProductEntity>) {
        hideShimmer()
        displayLimitedItems(products = products as MutableList<ProductEntity>)
    }

    //category BS
    override fun selectedCategory(categoryName: String) {
        viewModel.categoriesAdapter?.let {
            it.dismiss()
        }
        viewModel.categoryFilter = categoryName
        binding.tvToolbarTitle.text = categoryName
        isFiltered = true
        binding.cpCategoryFilter.isChecked = true
        viewModel.getAllProductByCategoryStatic(categoryName)
    }
}


