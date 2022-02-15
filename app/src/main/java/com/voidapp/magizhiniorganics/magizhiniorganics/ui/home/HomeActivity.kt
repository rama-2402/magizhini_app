package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.HomeRvAdapter.CategoryHomeAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.BestSellersAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.TestimonialsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SpecialBanners
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityHomeBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
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
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.HOME_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import kotlinx.coroutines.*
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import android.content.pm.ResolveInfo
import com.google.firebase.BuildConfig
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.AllCWMActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CWM_BANNER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PHONE_NUMBER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER_BANNER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET_BANNER


import java.util.*
import kotlin.collections.ArrayList


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

    private lateinit var dialogBsAddReferral: BottomSheetDialog

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
                    val product: ProductEntity? = viewModel.getProductByID(banner.description)
                    product?.let {
                        moveToProductDetails(product.id, product.name)
                    } ?: showErrorSnackBar("Product is no longer Available", true)
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
            CWM_BANNER -> navigateToCWM()
            WALLET_BANNER -> navigateToWallet()
            QUICK_ORDER_BANNER -> navigateToQuickOrder()
            else -> Unit
        }
    }

    private fun observers() {

        viewModel.getDataToPopulate()
        viewModel.getAllNotifications()

        viewModel.notifications.observe(this) {
            if (it.isNullOrEmpty()) {
                binding.ivNotification.visibleBadge(false)
            } else {
                binding.ivNotification.apply {
                    visibleBadge(true)
                    badgeValue = it.size
                }
            }
        }
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
        viewModel.bestSellers.observe(this) {
            binding.tvBestSellers.text = viewModel.bestSellerHeader
            bestSellersAdapter.recycler = "bestSeller"
            bestSellersAdapter.products = it
            bestSellersAdapter.notifyDataSetChanged()
        }
        viewModel.specialsOne.observe(this) {
            binding.tvSpecialsOne.text = viewModel.specialsOneHeader
            specialsOneAdapter.recycler = "one"
            specialsOneAdapter.products = it
            specialsOneAdapter.notifyDataSetChanged()
        }
        viewModel.specialsTwo.observe(this) {
            binding.tvSpecialsTwo.text = viewModel.specialsTwoHeader
            specialsTwoAdapter.recycler = "two"
            specialsTwoAdapter.products = it
            specialsTwoAdapter.notifyDataSetChanged()
        }
        viewModel.specialsThree.observe(this) {
            binding.tvSpecialsThree.text = viewModel.specialsThreeHeader
            specialsThreeAdapter.recycler = "three"
            specialsThreeAdapter.products = it
            specialsThreeAdapter.notifyDataSetChanged()
        }
        viewModel.recyclerPosition.observe(this) {
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
        }
        viewModel.specialBanners.observe(this) {
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
        }
        viewModel.testimonials.observe(this) {
            testimonialsAdapter.testimonials = it
            testimonialsAdapter.notifyDataSetChanged()
        }

        binding.svBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY < oldScrollY && binding.fabCart.isGone) {
                binding.fabCart.startAnimation(scaleBig)
                binding.fabCart.visibility = View.VISIBLE
            } else if (scrollY > oldScrollY && binding.fabCart.isVisible) {
                binding.fabCart.startAnimation(scaleSmall)
                binding.fabCart.visibility = View.GONE
            }
        })
        viewModel.referralStatus.observe(this) {
            hideProgressDialog()
            if (it) {
                dialogBsAddReferral.dismiss()
                showErrorSnackBar(
                    "Referral added Successfully. Your referral bonus will be added to your Wallet.",
                    false,
                    Constants.LONG
                )
            } else {
                showToast(this, "No account with the given number. Please check again")
            }
        }
        viewModel.allowReferral.observe(this) {
            if (it == "no") {
                showErrorSnackBar("Referral Already Applied", true)
            } else {
                showReferralBs(it)
            }
        }
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

    private fun showReferralBs(currentUserID: String) {
        //BS to add referral number
        dialogBsAddReferral = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogBottomAddReferralBinding = DataBindingUtil.inflate(LayoutInflater.from(applicationContext),R.layout.dialog_bottom_add_referral,null,false)
        dialogBsAddReferral.setCancelable(true)
        dialogBsAddReferral.setContentView(view.root)
        dialogBsAddReferral.dismissWithAnimation = true

        //verifying if the referral number is empty and assigning it to the userProfile object
        view.btnApply.setOnClickListener {
            val code = view.etReferralNumber.text.toString().trim()
            if (code.isEmpty()) {
                view.etlReferralNumber.error = "* Enter a valid code"
                return@setOnClickListener
            } else {
                showProgressDialog()
                viewModel.applyReferralNumber(currentUserID ,code)
            }
        }

        dialogBsAddReferral.show()
    }

    override fun displaySelectedCategory(category: String) {
        Intent(this, ShoppingMainActivity::class.java).also {
            it.putExtra(CATEGORY, category)
            it.putExtra(NAVIGATION, HOME_PAGE)
            startActivity(it)
            onPause()
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun moveToProductDetails(id: String, name: String) {
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(PRODUCTS, id)
            it.putExtra(Constants.PRODUCT_NAME, name)
            it.putExtra(NAVIGATION, HOME_PAGE)
            startActivity(it)
            onPause()
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
                            it.putExtra(NAVIGATION, ALL_PRODUCTS)
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

    private fun navigateToCWM() {
        lifecycleScope.launch {
            delay(200)
            Intent(this@HomeActivity, AllCWMActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun navigateToWallet() {
        lifecycleScope.launch {
            delay(200)
            Intent(this@HomeActivity, WalletActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun navigateToQuickOrder() {
        lifecycleScope.launch {
            delay(200)
            Intent(this@HomeActivity, QuickOrderActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    override fun onResume() {
        viewModel.getAllNotifications()
        super.onResume()
    }

//    override fun onRestart() {
//        viewModel.getAllNotifications()
//        super.onRestart()
//    }

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
                R.id.menuWallet -> navigateToWallet()
                R.id.menuSubscriptions -> {
                    lifecycleScope.launch {
                        delay(200)
                        Intent(this@HomeActivity, SubscriptionHistoryActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                }
                R.id.menuCWM -> navigateToCWM()
                R.id.menuQuickOrder -> navigateToQuickOrder()
                R.id.menuReferral -> {
                    binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
                    showExitSheet(this, "Magizhini Referral Program Offers Customers Referral Bonus Rewards for each successful New Customer using your PHONE NUMBER as Referral Code. Both You and any New Customer using your phone number as Referral ID will received Exciting Referral Bonus! Click Proceed to Continue")
                }
                R.id.menuSubDetails -> {
                    binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
                    showDescriptionBs(resources.getString(R.string.subscription_info))
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
                R.id.menuContactUs -> {
                    showListBottomSheet(this, arrayListOf("Call", "WhatsApp", "E-Mail"))
                }
                R.id.menuContactDeveloper -> {
                    showListBottomSheet(this, arrayListOf("WhatsApp", "E-Mail"), "developer")
                }
                R.id.menuAboutUs -> {
                    showDescriptionBs("ABOUT US \n\n  Magizhini Organics is a retail-focused Store offering food and related consumables produced from organics farming and Certified Organic food producers according to organic farming standards. \n\n\nABOUT ORGANIC FOOD:\n" +
                            "\n" +
                            "Any food product cultivated relying entirely on natural methods without compromising on the consumer's health can be called 'Organic' food. In other words, no chemicals, no unnatural fertilizers or pesticides and no farming methods that are not humane.\n" +
                            "\n" +
                            "Cultivating food the organic way is not as expensive or not viable as some make it out to be. Though it does cost the farmer more than the usual chemical dependent methods, at the same time it should not also be priced out of the reach of the ordinary consumer. Assuming that the ruling market prices for conventionally-grown food (read chemically-grown food) are fair, it is important that organic food also be prized more or less similar for pushing the customers towards more healthy organic intake, especially when consumers are aware that organic food is better than chemically-grown food in all respects, including taste, flavour and for their own health, besides that of the earth.\n" +
                            "\n" +
                            "Another aspect of the organic food 'issue' at least in India is a common problem faced by organic farmers: the lack of a ready market and often unremunerative prices for their produce. In many cases, the grower does not receive timely payments from middlemen including organic food traders. Interested buyers of organic food on the other hand, cannot find what they need, at least not at reasonable prices. Supplies are often erratic or unreliable and in some cases buyers are not even sure if the food they are buying is indeed organic.\n" +
                            "\n" +
                            "Taking all the above into account, we, Magizhini Organics, have started this initiative to supply organic food and food products in Chennai with a single click of button from your mobile in the comfort of your home. This is an attempt to link growers and processors with buyers to ensure a fair price for growers and easier availability of a wide variety of organic food at reasonable prices for buyers.")
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

    fun selectedContactMethodForDeveloper(selectedItem: String) {
        when(selectedItem) {
            "WhatsApp" -> {
                val message = ""
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://api.whatsapp.com/send?phone=+919486598819&text=$message"
                        )
                    )
                )
            }
            "E-Mail" -> {
                shareToGMail(arrayOf("rama_void@zohomail.in"), "", "")
            }
        }
    }

    fun selectedContactMethod(selectedItem: String) {
        when(selectedItem) {
            "Call" -> {
                this.callNumberIntent("7299827393")
            }
            "WhatsApp" -> {
                val message = ""
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://api.whatsapp.com/send?phone=+917299827393&text=$message"
                        )
                    )
                )
            }
            "E-Mail" -> {
                shareToGMail(arrayOf("magizhiniOrganics2018@gmail.com"), "", "")
            }
        }
    }

    private fun shareToGMail(email: Array<String?>?, subject: String?, content: String?) {
        val emailIntent = Intent(Intent.ACTION_SEND)
        try {
            emailIntent.putExtra(Intent.EXTRA_EMAIL, email)
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.type = "text/plain"
            emailIntent.putExtra(Intent.EXTRA_TEXT, content)
            val pm: PackageManager = this.packageManager
            val matches = pm.queryIntentActivities(emailIntent, 0)
            var best: ResolveInfo? = null
            for (info in matches) if (info.activityInfo.packageName.endsWith(".gm") || info.activityInfo.name.lowercase(
                    Locale.getDefault()
                )
                    .contains("gmail")
            ) best = info
            if (best != null) emailIntent.setClassName(
                best.activityInfo.packageName,
                best.activityInfo.name
            )
            this.startActivity(emailIntent)
        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(this, "Email App is not installed in your phone.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun referralAction(selectedItem: String) {
        val phoneNumber = SharedPref(this).getData(PHONE_NUMBER, STRING, "").toString()
        if(selectedItem == "Share My Referral Code") {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My application name")
                var shareMessage = "\nHey I'm using Magizhini Organics for my Organic Food Purchases. Check it out! You can use my number $phoneNumber as Referral Number to get Exciting Referral Bonus!\n\n"
                shareMessage =
                    """
                    ${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: java.lang.Exception) {
                showToast(this, "Something went wrong! Please try again later")
            }
        } else {
            viewModel.checkForReferral()
        }
    }

    fun showReferralOptions() {
        showListBottomSheet(this, arrayListOf("Share My Referral Code", "Have a Referral Code? Enter here..."), "referral")
    }


}
