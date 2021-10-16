package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.navigation.NavigationView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.HomeRvAdapter.CategoryHomeAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityHomeBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
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
            0, 0)

        binding.dlDrawerLayout.addDrawerListener(menuToggle)
        menuToggle.syncState()
        menuToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)

        binding.nvNavigationView.setNavigationItemSelectedListener(this)

        showProgressDialog()

        generateRecyclerView()
        //getting all the data from room database
        observers()
        clickListeners()

        lifecycleScope.launch {
            delay(1000)
            SharedPref(this@HomeActivity).putData(Constants.DATE, Constants.STRING, Time().getCurrentDate())
            hideProgressDialog()
        }
    }

    private fun clickListeners() {
        binding.cpShowAll.setOnClickListener(this)
        binding.fabCart.setOnClickListener(this)
    }

    private fun observers() {
        //observing the banners data and setting the banners
        viewModel.getAllBanners().observe(this, Observer {
            val banners = it
            val bannersCarousel: MutableList<CarouselItem> = mutableListOf()
            for (banner in banners) {
                //creating a mutable list baaner carousel item
                bannersCarousel.add(CarouselItem(
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

        //scroll change listener to hide the fab when scrolling down
        binding.rvHomeItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                super.onScrolled(recyclerView, up, down)
                if (down > 0 && binding.fabCart.isVisible) {
                    binding.fabCart.hide()
                } else if (down < 0 && binding.fabCart.isGone) {
                    binding.fabCart.show()
                }
            }
        })
    }

    //after generating the banners from the viewModel we are assigning the classes to carousel items and settnig click listeners for it
    private fun generateBanners(bannerCarouselItems: MutableList<CarouselItem>, bannerItems: List<BannerEntity>) {

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
                    when(bannerItems[position].type) {
                        Constants.OPEN_LINK -> bannerDescriptionClickAction(bannerItems[position].description, Constants.OPEN_LINK)
                        Constants.SHOW_DETAILS -> bannerDescriptionClickAction(bannerItems[position].description, Constants.SHOW_DETAILS)
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e("void", e.message.toString())
                }
            }
        }
    }

    private fun generateRecyclerView(){

        adapter = CategoryHomeAdapter(
            this,
            listOf(),
            viewModel
        )

        binding.rvHomeItems.layoutManager = GridLayoutManager(this, 3)
        binding.rvHomeItems.adapter = adapter
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

    fun bannerDescriptionClickAction(content: String, type: String) {
        when(type) {
            //show a bottom sheet dialog with the description
            Constants.SHOW_DETAILS -> showDescriptionBs(content)
            //open a link in browser when pressed
            Constants.OPEN_LINK -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse(content))
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
                    displaySelectedCategory(Constants.ALL_PRODUCTS)
//                    populateData()
                }
                binding.fabCart -> {
                    Intent(this, CheckoutActivity::class.java).also {
                        startActivity(it)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuLogOut -> {
                SharedPref(this).clearAllData()
                viewModel.signOut()
                finishAffinity()
                finish()
            }
            R.id.menuProfile -> {
                Intent(this, ProfileActivity::class.java).also {
                    startActivity(it)
                }
            }
            R.id.menuOrders -> {
                Intent(this, PurchaseHistoryActivity::class.java).also {
                    startActivity(it)
                }
            }
            R.id.menuContact -> {
                Intent(this, ChatActivity::class.java).also {
                    startActivity(it)
                }
            }
            R.id.menuWallet -> {
                Intent(this, WalletActivity::class.java).also {
                    startActivity(it)
                }
            }
            R.id.menuSubscriptions -> {
                Intent(this, SubscriptionHistoryActivity::class.java).also {
                    startActivity(it)
                }
            }
        }
        binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
