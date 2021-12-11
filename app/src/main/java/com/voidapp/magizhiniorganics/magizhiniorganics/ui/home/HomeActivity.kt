package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
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
import androidx.viewbinding.ViewBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.HomeRvAdapter.CategoryHomeAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.BestSellersAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.TestimonialsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SpecialBanners
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Banner
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityHomeBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.VideoPlayerActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification.NotificationsActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BROADCAST
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CATEGORY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.DESCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NONE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import kotlinx.coroutines.*
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class HomeActivity :
    BaseActivity(),
    View.OnClickListener,
    KodeinAware,
    HomeListener,
    TestimonialsAdapter.TestimonialItemClickListener,
    NavigationView.OnNavigationItemSelectedListener {

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
    private lateinit var testimonialsAdapter: TestimonialsAdapter
    private var mSelectedBanner: SpecialBanners = SpecialBanners()
    private val banners = mutableListOf<BannerEntity>()

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

        FirebaseMessaging.getInstance().subscribeToTopic(BROADCAST)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
//                viewModel.logCrash("FCM token failed", task.exception)
            } else {
                viewModel.updateToken(task.result)
            }
        }

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
        binding.cpTestimonials.setOnClickListener(this)
        binding.fabCart.setOnClickListener(this)
        binding.ivNotification.setOnClickListener {
            Intent(this, NotificationsActivity::class.java).also {
                startActivity(it)
                onPause()
            }
        }
        binding.apply {
            ivBannerOne.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[0]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerTwo.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[1]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerThree.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[2]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerFour.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[3]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerFive.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[4]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerSix.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[5]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerSeven.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[6]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerEight.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[7]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerNine.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[8]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerTen.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[9]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerEleven.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[10]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
            ivBannerTwelve.setOnClickListener {
                mSelectedBanner = viewModel.bannersList[11]
                selectedBanner(mSelectedBanner.toBannerEntity())
            }
        }
    }

    private fun selectedBanner(banner: BannerEntity) = lifecycleScope.launch {
        when(banner.type) {
            PRODUCTS -> {
                try {
                    val product = viewModel.getProductByID(banner.description)
                    moveToProductDetails(product.id, product.name)
                } catch (e: Exception) {}
            }
            CATEGORY -> {
                try {
                    val category = viewModel.getCategoryByID(banner.description)
                    displaySelectedCategory(category)
                } catch (e: Exception) {}
            }
            DESCRIPTION -> {
                showDescriptionBs(banner.description)
            }
            else -> Unit
        }
    }

    private fun observers() {

        viewModel.getDataToPopulate()
        viewModel.getAllNotifications()

        viewModel.notifications.observe(this, {
            if (it.isEmpty()) {
                binding.ivNotification.visibleBadge(false)
            } else {
                binding.ivNotification.apply{
                    visibleBadge(true)
                    badgeValue = it.size
                }
            }
        })
        //observing the banners data and setting the banners
        viewModel.getAllBanners().observe(this, Observer {
            banners.clear()
            banners.addAll(it)
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
                GlideLoader().loadUserPicture(this@HomeActivity, it[0].url, ivBannerOne)
                GlideLoader().loadUserPicture(this@HomeActivity, it[1].url, ivBannerTwo)
                GlideLoader().loadUserPicture(this@HomeActivity, it[2].url, ivBannerThree)
                GlideLoader().loadUserPicture(this@HomeActivity, it[3].url, ivBannerFour)
                GlideLoader().loadUserPicture(this@HomeActivity, it[4].url, ivBannerFive)
                GlideLoader().loadUserPicture(this@HomeActivity, it[5].url, ivBannerSix)
                GlideLoader().loadUserPicture(this@HomeActivity, it[6].url, ivBannerSeven)
                GlideLoader().loadUserPicture(this@HomeActivity, it[7].url, ivBannerEight)
                GlideLoader().loadUserPicture(this@HomeActivity, it[8].url, ivBannerNine)
                GlideLoader().loadUserPicture(this@HomeActivity, it[9].url, ivBannerTen)
                GlideLoader().loadUserPicture(this@HomeActivity, it[10].url, ivBannerEleven)
                GlideLoader().loadUserPicture(this@HomeActivity, it[11].url, ivBannerTwelve)
            }
        })
        viewModel.testimonials.observe(this, {
            testimonialsAdapter.testimonials = it
            testimonialsAdapter.notifyDataSetChanged()
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
                selectedBanner(banners[position])
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
        testimonialsAdapter = TestimonialsAdapter(
            this,
            mutableListOf(),
            this
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
        binding.rvTestimonials.layoutManager =
            LinearLayoutManager(this)
        binding.rvTestimonials.adapter = testimonialsAdapter
//        val snapHelper: SnapHelper = GravitySnapHelper(Gravity.TOP)
//        snapHelper.attachToRecyclerView(binding.rvHomeItems)
    }

    override fun displaySelectedCategory(category: String) {
        Intent(this, ShoppingMainActivity::class.java).also {
            it.putExtra(CATEGORY, category)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun moveToProductDetails(id: String, name: String) {
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(PRODUCTS, id)
            it.putExtra(Constants.PRODUCT_NAME, name)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
/*
    //This will lock the vertical scrolling when horizontal child is scrolling and vice versa
            binding.rvHomeItems.setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
*/

    override fun onDataTransactionFailure(message: String) {
        showErrorSnackBar(message, true)
    }

    override fun onBackPressed() {
        if (binding.dlDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            finish()
            finishAffinity()
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
                binding.cpTestimonials -> {
                    binding.cpTestimonials.startAnimation(
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

    override fun onResume() {
        viewModel.getAllNotifications()
        super.onResume()
    }

    override fun onRestart() {
        viewModel.getAllNotifications()
        super.onRestart()
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

    override fun openVideo(url: String) {
        Intent(this, VideoPlayerActivity::class.java).also {
            it.putExtra("url", url)
            startActivity(it)
        }
    }
}
