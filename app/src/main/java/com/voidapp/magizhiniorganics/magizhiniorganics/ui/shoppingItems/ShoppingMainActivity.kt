package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter.ShoppingMainAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityShoppingMainBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.collections.ArrayList

class ShoppingMainActivity :
    BaseActivity(),
    KodeinAware,
    ShoppingMainListener {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityShoppingMainBinding
    private val factory: ShoppingMainViewModelFactory by instance()
    private lateinit var viewModel: ShoppingMainViewModel

    private var cartBottomSheet: BottomSheetBehavior<LinearLayout> = BottomSheetBehavior()
    private lateinit var checkoutText: TextView
    private var item: MenuItem? = null

    private lateinit var cartAdapter: CartAdapter
    private lateinit var adapter: ShoppingMainAdapter
    var categoryFilter: String = Constants.ALL_PRODUCTS
    private var mCartPrice: Float? = 0f
    private var isFiltered: Boolean = false

    private var mItems: List<ProductEntity> = listOf()
    private val mFilteredItems: MutableList<ProductEntity> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = setContentView(this, R.layout.activity_shopping_main)
        viewModel = ViewModelProvider(this, factory).get(ShoppingMainViewModel::class.java)
        binding.viewmodel = viewModel
        viewModel.shoppingMainListener = this

        setSupportActionBar(binding.tbToolbar)

        //getting the intent to check the type of chip we want to see
        categoryFilter = intent.getStringExtra(Constants.CATEGORY).toString()

        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        title = ""
        setSupportActionBar(binding.tbToolbar)
        binding.tvToolbarTitle.text = categoryFilter

        showProgressDialog()

        initRecyclerView()
        observeLiveData()
        clickListeners()
        setCartBottom()
        checkProductsToDisplay()
    }

    private fun observeLiveData() {
        viewModel.allProduct.observe(this, {
            viewModel.selectedChip = Constants.ALL
            mItems = it
            adapter.limited = false
            adapter.setData(it)
            binding.cpAll.isChecked = true
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideShimmer()
            }
        })
        viewModel.allProductsInCategory.observe(this, {
            viewModel.selectedChip = Constants.CATEGORY
            viewModel.selectedCategory = categoryFilter
            adapter.limited = false
            adapter.setData(it)
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideShimmer()
            }
        })
        viewModel.allFavorites.observe(this, {
            viewModel.selectedChip = Constants.FAVORITES
            adapter.limited = false
            adapter.setData(it)
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideShimmer()
            }
        })
        viewModel.discountAvailableProducts.observe(this, {
            viewModel.selectedChip = Constants.DISCOUNT
            adapter.limited = false
            adapter.setData(it)
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideShimmer()
            }
        })
        viewModel.getAllCartItems().observe(this, {
            cartAdapter.setCartData(it)
        })
        viewModel.availableCategoryNames.observe(this, {
            showListBottomSheet(this, it as ArrayList<String>)
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
            flShimmerPlaceholder.show()
            rvShoppingItems.hide()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.gone()
            rvShoppingItems.show()
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
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Product Store"
                    viewModel.getAllProductsStatic()
                }
                R.id.cpCategoryFilter -> {
                    //kept this chip as the default check in xml so that the bottom sheet wont be
                    //triggered when a category of product was selected from home screen
                    if (!isFiltered) {
                        openCategoryFilterDialog()
                    }
                }
                R.id.cpFavorites -> {
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Favorites"
                    viewModel.getAllFavoritesStatic()
                }
                R.id.cpDiscounts -> {
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Discounts"
                    //Querying the discount items straight from the room database and setting it in the adapter
                    viewModel.getAllDiscountProducts()
                }
                R.id.cpLimitedItems -> {
                    showShimmer()
                    isFiltered = false
                    binding.flShimmerPlaceholder.startShimmer()
                    binding.tvToolbarTitle.text = "Limited Items"
                    viewModel.limitedItemsFilter()
                }
            }
        }
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
                mFilteredItems.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if (searchText.isNotEmpty()) {
                    mItems.forEach {
                        if (it.name.lowercase(Locale.getDefault()).contains(searchText)) {
                            mFilteredItems.add(it)
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
    }

    private fun initRecyclerView() {

        cartAdapter = CartAdapter(
            this,
            listOf(),
            viewModel
        )

        adapter = ShoppingMainAdapter(
            this,
            listOf(),
            false,
            viewModel
        )
        binding.rvShoppingItems.layoutManager = LinearLayoutManager(this)
        binding.rvShoppingItems.adapter = adapter

        hideProgressDialog()
    }

    private fun setCartBottom() {
        val bottomSheet = findViewById<LinearLayout>(R.id.clBottomCart)
        val filterBtn = findViewById<ImageView>(R.id.ivFilter)
        val cartBtn = findViewById<ImageView>(R.id.ivCart)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

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

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        cartBtn.setOnClickListener {
            cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        }

        filterBtn.setOnClickListener {
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            openCategoryFilterDialog()
        }

        checkoutBtn.setOnClickListener {
            Intent(this, CheckoutActivity::class.java).also {
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
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

    //updating the limited items products list
    override fun limitedItemList(products: List<ProductEntity>) {
        adapter.limited = true
        adapter.setData(products)
        hideShimmer()
    }

    override fun moveToProductDetails(id: String, name: String) {
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(Constants.PRODUCTS, id)
            it.putExtra(Constants.PRODUCT_NAME, name)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
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
            Intent(this, HomeActivity::class.java).also {
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
            }
        }
    }
}