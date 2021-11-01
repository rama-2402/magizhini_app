package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.navigation.NavigationView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.HomeRvAdapter.CategoryHomeAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.BestSellersAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityHomeBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.*
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class HomeActivity : BaseActivity(), View.OnClickListener, KodeinAware, HomeListener, NavigationView.OnNavigationItemSelectedListener {

    //DI Injection with kodein
    override val kodein by kodein()
    private val factory: HomeViewModelFactory by instance()

    //initializing the viewModel and binding for activity
    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: CategoryHomeAdapter
    private lateinit var bestSellersAdapter: BestSellersAdapter
    private lateinit var specialsOneAdapter: BestSellersAdapter
    private lateinit var specialsTwoAdapter: BestSellersAdapter
    private lateinit var specialsThreeAdapter: BestSellersAdapter

    //initializing the carousel item for the banners
    val mItems: MutableList<CarouselItem> = mutableListOf()
    private val mCategoriesList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)
        binding.viewmodel = viewModel
        viewModel.homeListener = this

        setSupportActionBar(binding.tbToolbar)
        binding.tbToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))

        val menuToggle = ActionBarDrawerToggle(
            this,
            binding.dlDrawerLayout,
            binding.tbToolbar,
            0, 0
        )

        binding.dlDrawerLayout.addDrawerListener(menuToggle)
        menuToggle.syncState()
        menuToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)

        binding.nvNavigationView.setNavigationItemSelectedListener(this)

        if (binding.dlDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
        }

        showProgressDialog()

        generateRecyclerView()
        //getting all the data from room database
        observers()
        clickListeners()

        lifecycleScope.launch {
            delay(1000)
            SharedPref(this@HomeActivity).putData(
                Constants.DATE,
                Constants.STRING,
                TimeUtil().getCurrentDate()
            )
            hideProgressDialog()
        }
    }

    private fun clickListeners() {
        binding.cpShowAll.setOnClickListener(this)
        binding.cpBestSellers.setOnClickListener(this)
        binding.cpSpecialOne.setOnClickListener(this)
        binding.cpSpecialTwo.setOnClickListener(this)
        binding.cpSpecialThree.setOnClickListener(this)
        binding.fabCart.setOnClickListener(this)
    }

    private fun observers() {

        viewModel.getDataToPopulate()

        //observing the banners data and setting the banners
        viewModel.getAllBanners().observe(this, Observer {
            val banners = it
            val bannersCarousel: MutableList<CarouselItem> = mutableListOf()
            for (banner in banners) {
                //creating a mutable list baaner carousel item
                bannersCarousel.add(
                    CarouselItem(
                        imageUrl = banner.url
                    )
                )
            }
            generateBanners(bannersCarousel, banners)
        })
        viewModel.getALlCategories().observe(this, Observer {
            it.forEach { cat ->
                mCategoriesList.add(cat.name)
            }
            adapter.categories = it
            adapter.notifyDataSetChanged()
        })
        viewModel.bestSellers.observe(this, {
            binding.tvBestSellers.text = viewModel.bestSellerHeader
            bestSellersAdapter.recycler = "bestSeller"
            bestSellersAdapter.products = it
            bestSellersAdapter.notifyDataSetChanged()
        })
        viewModel.specialsOne.observe(this, {
            binding.tvSpecialsOne.text = viewModel.specialsOneHeader
            specialsOneAdapter.recycler = "one"
            specialsOneAdapter.products = it
            specialsOneAdapter.notifyDataSetChanged()
        })
        viewModel.specialsTwo.observe(this, {
            binding.tvSpecialsTwo.text = viewModel.specialsTwoHeader
            specialsTwoAdapter.recycler = "two"
            specialsTwoAdapter.products = it
            specialsTwoAdapter.notifyDataSetChanged()
        })
        viewModel.specialsThree.observe(this, {
            binding.tvSpecialsThree.text = viewModel.specialsThreeHeader
            specialsThreeAdapter.recycler = "three"
            specialsThreeAdapter.products = it
            specialsThreeAdapter.notifyDataSetChanged()
        })
        viewModel.recyclerPosition.observe(this, {
            when (viewModel.recyclerToRefresh) {
                "bestSeller" -> {
//                    bestSellersAdapter.products[it] = viewModel.productToUpdate
                    bestSellersAdapter.notifyItemChanged(it)
                    with(viewModel) {
                        getUpdatedSpecialsOne()
                        getUpdatedSpecialsTwo()
                        getUpdatedSpecialsThree()
                    }
                }
                "one" -> {
                    specialsOneAdapter.notifyItemChanged(it)
                    with(viewModel) {
                        getUpdatedBestSellers()
                        getUpdatedSpecialsTwo()
                        getUpdatedSpecialsThree()
                    }
                }
                "two" -> {
                    specialsTwoAdapter.notifyItemChanged(it)
                    with(viewModel) {
                        getUpdatedBestSellers()
                        getUpdatedSpecialsOne()
                        getUpdatedSpecialsThree()
                    }
                }
                "three" -> {
                    specialsThreeAdapter.notifyItemChanged(it)
                    with(viewModel) {
                        getUpdatedBestSellers()
                        getUpdatedSpecialsOne()
                        getUpdatedSpecialsTwo()
                    }
                }
            }
        })
        viewModel.specialBanners.observe(this, {
            with(binding) {
                GlideLoader().loadUserPicture(this@HomeActivity, it[0], ivBannerOne)
                GlideLoader().loadUserPicture(this@HomeActivity, it[1], ivBannerTwo)
                GlideLoader().loadUserPicture(this@HomeActivity, it[2], ivBannerThree)
                GlideLoader().loadUserPicture(this@HomeActivity, it[3], ivBannerFour)
                GlideLoader().loadUserPicture(this@HomeActivity, it[4], ivBannerFive)
                GlideLoader().loadUserPicture(this@HomeActivity, it[5], ivBannerSix)
                GlideLoader().loadUserPicture(this@HomeActivity, it[6], ivBannerSeven)
                GlideLoader().loadUserPicture(this@HomeActivity, it[7], ivBannerEight)
                GlideLoader().loadUserPicture(this@HomeActivity, it[8], ivBannerNine)
                GlideLoader().loadUserPicture(this@HomeActivity, it[9], ivBannerTen)
                GlideLoader().loadUserPicture(this@HomeActivity, it[10], ivBannerEleven)
                GlideLoader().loadUserPicture(this@HomeActivity, it[11], ivBannerTwelve)
            }
        })

        binding.svBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY < oldScrollY && binding.fabCart.isGone) {
                binding.fabCart.startAnimation(Animations.scaleBig)
                binding.fabCart.visibility = View.VISIBLE
            } else if (scrollY > oldScrollY && binding.fabCart.isVisible) {
                binding.fabCart.startAnimation(Animations.scaleSmall)
                binding.fabCart.visibility = View.GONE
            }
        })

//        //scroll change listener to hide the fab when scrolling down
//        binding.rvHomeItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
//                super.onScrolled(recyclerView, up, down)
//                if (down > 0 && binding.fabCart.isVisible) {
//                    binding.fabCart.hide()
//                } else if (down < 0 && binding.fabCart.isGone) {
//                    binding.fabCart.show()
//                }
//            }
//        })
    }

    //after generating the banners from the viewModel we are assigning the classes to carousel items and settnig click listeners for it
    private fun generateBanners(
        bannerCarouselItems: MutableList<CarouselItem>,
        bannerItems: List<BannerEntity>
    ) {

        mItems.addAll(bannerCarouselItems)

        //adding the data to carousel items
        binding.cvBanner.addData(bannerCarouselItems)
        //registering the lifecycle to prevent memory leak
        binding.cvBanner.registerLifecycle(this)
        binding.cvBanner.carouselListener = object : CarouselListener {
            override fun onClick(position: Int, carouselItem: CarouselItem) {
                try {
                    //we are using try catch because there is a bug in the library where it sometimes returns larger index numbers causing app crash
                    //checking the click response content type
                    when (bannerItems[position].type) {
                        Constants.OPEN_LINK -> bannerDescriptionClickAction(
                            bannerItems[position].description,
                            Constants.OPEN_LINK
                        )
                        Constants.SHOW_DETAILS -> bannerDescriptionClickAction(
                            bannerItems[position].description,
                            Constants.SHOW_DETAILS
                        )
                        else -> {
                        }
                    }
                } catch (e: Exception) {
                    Log.e("void", e.message.toString())
                }
            }
        }
    }

    private fun generateRecyclerView() {

        adapter = CategoryHomeAdapter(
            this,
            listOf(),
            viewModel
        )

        bestSellersAdapter = BestSellersAdapter(
            this,
            listOf(),
            viewModel,
            "bestSeller"
        )
        specialsOneAdapter = BestSellersAdapter(
            this,
            listOf(),
            viewModel,
            "one"
        )
        specialsTwoAdapter = BestSellersAdapter(
            this,
            listOf(),
            viewModel,
            "two"
        )
        specialsThreeAdapter = BestSellersAdapter(
            this,
            listOf(),
            viewModel,
            "three"
        )

        binding.rvHomeItems.layoutManager = GridLayoutManager(this, 3)
        binding.rvHomeItems.adapter = adapter
        binding.rvTopPurchases.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTopPurchases.adapter = bestSellersAdapter
        binding.rvSpecialsOne.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSpecialsOne.adapter = specialsOneAdapter
        binding.rvSpecialsTwo.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSpecialsTwo.adapter = specialsTwoAdapter
        binding.rvSpecialsThree.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSpecialsThree.adapter = specialsThreeAdapter
//        val snapHelper: SnapHelper = GravitySnapHelper(Gravity.TOP)
//        snapHelper.attachToRecyclerView(binding.rvHomeItems)
    }

    override fun displaySelectedCategory(category: String) {
        Intent(this, ShoppingMainActivity::class.java).also {
            it.putExtra(Constants.CATEGORY, category)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun moveToProductDetails(id: String, name: String) {
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(Constants.PRODUCTS, id)
            it.putExtra(Constants.PRODUCT_NAME, name)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    fun bannerDescriptionClickAction(content: String, type: String) {
        when (type) {
            //show a bottom sheet dialog with the description
            Constants.SHOW_DETAILS -> showDescriptionBs(content)
            //open a link in browser when pressed
            Constants.OPEN_LINK -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.data = Uri.parse(content)
                    startActivity(Intent.createChooser(intent, "Open link with"))
                } catch (e: Exception) {
                    println("The current phone does not have a browser installed")
                }
            }

        }


/*
    //This will lock the vertical scrolling when horizontal child is scrolling and vice versa
            binding.rvHomeItems.setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
*/
    }

    override fun onDataTransactionFailure(message: String) {
        showErrorSnackBar(message, true)
    }

    override fun onBackPressed() {
        if (binding.dlDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            finishAffinity()
            finish()
            super.onBackPressed()
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v) {
                binding.cpShowAll -> {
                    binding.cpShowAll.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.cpShowAll.context,
                            R.anim.bounce
                        )
                    )
                    moveToAllProducts()
                }
                binding.cpBestSellers -> {
                    binding.cpBestSellers.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.cpShowAll.context,
                            R.anim.bounce
                        )
                    )
                    moveToAllProducts()
                }
                binding.cpSpecialOne -> {
                    binding.cpSpecialOne.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.cpShowAll.context,
                            R.anim.bounce
                        )
                    )
                    moveToAllProducts()
                }
                binding.cpSpecialTwo -> {
                    binding.cpSpecialTwo.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.cpShowAll.context,
                            R.anim.bounce
                        )
                    )
                    moveToAllProducts()
                }
                binding.cpSpecialThree -> {
                    binding.cpSpecialThree.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.cpShowAll.context,
                            R.anim.bounce
                        )
                    )
                    moveToAllProducts()
                }
                binding.fabCart -> {
                    if (NetworkHelper.isOnline(this)) {
                        Intent(this, InvoiceActivity::class.java).also {
                            startActivity(it)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }
                    } else {
                        showErrorSnackBar("Please check network connection", true)
                    }
                }
            }
        }
    }

    private fun moveToAllProducts() {
        lifecycleScope.launch {
            delay(150)
            displaySelectedCategory(Constants.ALL_PRODUCTS)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (NetworkHelper.isOnline(this)) {
            when (item.itemId) {
                R.id.menuLogOut -> {
                    SharedPref(this).clearAllData()
                    viewModel.signOut()
                    finishAffinity()
                    finish()
                }
                R.id.menuProfile -> {
                    lifecycleScope.launch {
                        delay(200)
                        Intent(this@HomeActivity, ProfileActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                }
                R.id.menuOrders -> {
                    lifecycleScope.launch {
                        delay(200)
                        Intent(this@HomeActivity, PurchaseHistoryActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                }
                R.id.menuContact -> {
                    lifecycleScope.launch {
                        delay(200)
                        Intent(this@HomeActivity, ChatActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                }
                R.id.menuWallet -> {
                    lifecycleScope.launch {
                        delay(200)
                        Intent(this@HomeActivity, WalletActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                }
                R.id.menuSubscriptions -> {
                    lifecycleScope.launch {
                        delay(200)
                        Intent(this@HomeActivity, SubscriptionHistoryActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                }
                R.id.menuPrivacyPolicy -> {
                    lifecycleScope.launch {
                        delay(200)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.data = Uri.parse("https://rama-2402.github.io/privacy-policy/")
                            startActivity(Intent.createChooser(intent, "Open link with"))
                        } catch (e: Exception) {
                            println("The current phone does not have a browser installed")
                        }
                    }
                }
                R.id.menuDisclaimer -> {
                    lifecycleScope.launch {
                        delay(200)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.data = Uri.parse("https://rama-2402.github.io/disclaimer/")
                            startActivity(Intent.createChooser(intent, "Open link with"))
                        } catch (e: Exception) {
                            println("The current phone does not have a browser installed")
                        }
                    }
                }
                R.id.menuTermsOfUse -> {
                    lifecycleScope.launch {
                        delay(200)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.data = Uri.parse("https://rama-2402.github.io/terms-of-use/")
                            startActivity(Intent.createChooser(intent, "Open link with"))
                        } catch (e: Exception) {
                            println("The current phone does not have a browser installed")
                        }
                    }
                }
                R.id.menuReturn -> {
                    lifecycleScope.launch {
                        delay(200)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.data = Uri.parse("https://rama-2402.github.io/return-policy/")
                            startActivity(Intent.createChooser(intent, "Open link with"))
                        } catch (e: Exception) {
                            println("The current phone does not have a browser installed")
                        }
                    }
                }
            }
            return true
        } else {
            showErrorSnackBar("Please check network connection", true)
            return false
        }
    }
}
