package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home


import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.BirthdayCard
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Partners
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityHomeBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.AllCWMActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.BirthdayCardDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification.NotificationsActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CATEGORY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CWM
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CWM_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.DESCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.HOME_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.OPEN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_HISTORY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_HISTORY_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PHONE_NUMBER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.REFERRAL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_HISTORY_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.imaginativeworld.whynotimagecarousel.ImageCarousel
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*


class HomeActivity :
    BaseActivity(),
    KodeinAware,
    TestimonialsAdapter.TestimonialItemClickListener,
    NavigationView.OnNavigationItemSelectedListener,
    PartnersAdapter.PartnersItemClickListener,
    BestSellersAdapter.BestSellerItemClickListener,
    CategoryHomeAdapter.CategoryItemClickListener,
    HomeSpecialsAdapter.HomeSpecialsItemClickListener {
    //DI Injection with kodein
    override val kodein by kodein()
    private val factory: HomeViewModelFactory by instance()

    //initializing the viewModel and binding for activity
    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: ActivityHomeBinding

    private lateinit var adapter: CategoryHomeAdapter
    private lateinit var testimonialsAdapter: TestimonialsAdapter
    private lateinit var homeSpecialsAdapter: HomeSpecialsAdapter
    private lateinit var partnersAdapter: PartnersAdapter

    private lateinit var dialogBsAddReferral: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        setSupportActionBar(binding.tbToolbar)

        val menuToggle = ActionBarDrawerToggle(
            this,
            binding.dlDrawerLayout,
            binding.tbToolbar,
            0, 0
        )

        binding.dlDrawerLayout.addDrawerListener(menuToggle)
        menuToggle.syncState()
        menuToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.matteRed)

        binding.nvNavigationView.setNavigationItemSelectedListener(this)

        if (binding.dlDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
        }

//        showProgressDialog(true)
        binding.flShimmerPlaceholder.startShimmer()

        FirebaseMessaging.getInstance().subscribeToTopic(Constants.BROADCAST)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
//                viewModel.logCrash("FCM token failed", task.exception)
            } else {
                if (
                    intent?.getBooleanExtra("day", false) == true) {
                    viewModel.updateToken(task.result)
                }
            }
        }
        generateRecyclerView()
        initData()
        //getting all the data from room database
        observers()
        clickListeners()

        intent.getStringExtra("navigate")?.let {
            navigateToPageFromNotification(it)
        } ?: let {
            SharedPref(this@HomeActivity).putData(
                Constants.DATE,
                STRING,
                TimeUtil().getCurrentDate()
            )
        }
//        hideProgressDialog()

    }

    private fun initData() {
        viewModel.getDataToPopulate()
    }

    private fun navigateToPageFromNotification(filter: String) {
        when (filter) {
            ORDER_HISTORY_PAGE -> {
                Intent(this@HomeActivity, PurchaseHistoryActivity::class.java).also {
                    startActivity(it)
                }
            }
            SUB_HISTORY_PAGE -> {
                Intent(this@HomeActivity, SubscriptionHistoryActivity::class.java).also {
                    startActivity(it)
                }
            }
            QUICK_ORDER_PAGE -> navigateToQuickOrder()
            CWM_PAGE -> navigateToCWM()
        }
    }

    private fun clickListeners() {
        binding.apply {
            ivNotification.setOnClickListener {
                navigateToNotificationPage()
            }
            svBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                when {
                    scrollY < oldScrollY && binding.fabCart.isGone -> {
                        binding.fabCart.startAnimation(
                            AnimationUtils.loadAnimation(this@HomeActivity, R.anim.scale_big)
                        )
                        binding.fabCart.visibility = View.VISIBLE
                    }
                    scrollY > oldScrollY && binding.fabCart.isVisible -> {
                        binding.fabCart.startAnimation(
                            AnimationUtils.loadAnimation(this@HomeActivity, R.anim.scale_small)
                        )
                        binding.fabCart.visibility = View.GONE
                    }
                    scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight -> {
                        if (NetworkHelper.isOnline(this@HomeActivity)) {
                            if (viewModel.partners.isEmpty() || viewModel.testimonials.isEmpty()) {
                                showProgressDialog(true)
                                llSocials.visible()
                                stickyFollowUs.visible()
                                viewModel.getEndData()
                            }
                        } else {
                            hideProgressDialog()
                            showErrorSnackBar("Please check your Network Connection", true)
                        }
                    }
                }
            })
            binding.cpShowAll.setOnClickListener {
                cpShowAll.startAnimation(
                    AnimationUtils.loadAnimation(
                        binding.cpShowAll.context,
                        R.anim.bounce
                    )
                )
                moveToAllProducts()
            }
            cpTestimonials.setOnClickListener {
                cpTestimonials.startAnimation(
                    AnimationUtils.loadAnimation(
                        binding.cpShowAll.context,
                        R.anim.bounce
                    )
                )
                moveToAllProducts()
            }
            fabCart.setOnClickListener {
                if (NetworkHelper.isOnline(this@HomeActivity)) {
                    Intent(this@HomeActivity, InvoiceActivity::class.java).also {
                        startActivity(it)
                    }
                } else {
                    showErrorSnackBar("Please check network connection", true)
                }
            }
            ivInstagram.setOnClickListener {
                openInBrowser("https://www.instagram.com/magizhini_organics/?utm_medium=copy_link")
            }
            ivFacebook.setOnClickListener {
                openInBrowser("https://www.facebook.com/organicshopping/")
            }
//          binding.ivTelegram -> openInBrowser("https://t.me/coder_lane")
            ivLinkedIn.setOnClickListener {
                openInBrowser("https://www.linkedin.com/in/ramasubramanian-r-7557a59b?lipi=urn%3Ali%3Apage%3Ad_flagship3_profile_view_base_contact_details%3BT1%2FqEmlxTEWb9fHPRfHpCw%3D%3D")
            }
        }
    }

    private fun navigateToNotificationPage() {
        Intent(this, NotificationsActivity::class.java).also {
            startActivity(it)
        }
    }

    private fun selectedBanner(banner: BannerEntity) = lifecycleScope.launch {
        when (banner.type) {
            PRODUCTS -> {
                try {
                    val product: ProductEntity? = viewModel.getProductByID(banner.description)
                    product?.let {
                        navigateToProductDetails(product.id, product.name, null)
                    } ?: showErrorSnackBar("Product is no longer Available", true)
                } catch (e: Exception) {
                }
            }
            CATEGORY -> {
                try {
                    val category = viewModel.getCategoryByID(banner.description)
                    navigateToSelectedCategory(category)
                } catch (e: Exception) {
                }
            }
            DESCRIPTION -> {
                showDescriptionBs(banner.description)
            }
            QUICK_ORDER -> navigateToQuickOrder()
            CWM -> navigateToCWM()
            WALLET -> navigateToWallet()
            REFERRAL -> showReferralBs(
                SharedPref(this@HomeActivity).getData(USER_ID, STRING, "").toString()
            )
            ORDER_HISTORY -> {
                Intent(this@HomeActivity, PurchaseHistoryActivity::class.java).also {
                    startActivity(it)
                }
            }
            SUBSCRIPTION -> navigateToSelectedCategory(SUBSCRIPTION)
            OPEN -> openPreview(banner.url, binding.cvBanner)
            else -> Unit
        }
    }

    private fun observers() {
        viewModel.uiUpdate.observe(this) { event ->
            when (event) {
                is HomeViewModel.UiUpdate.PopulateData -> {
                    binding.flShimmerPlaceholder.remove()
                    homeSpecialsAdapter.setBestSellerData(
                        viewModel.bannersList,
                        viewModel.bestSellersList,
                        viewModel.specialsTitles
                    )
                }
                is HomeViewModel.UiUpdate.ShowNotificationsCount -> {
                    event.count?.let { binding.ivNotification.badgeValue = it }
                }
                is HomeViewModel.UiUpdate.PopulateEnd -> {
                    hideProgressDialog()
                    testimonialsAdapter.setTestimonialData(event.testimonials)
                    event.partners?.let { partnersAdapter.setPartnersData(it) }
                }
                is HomeViewModel.UiUpdate.ShowBirthDayCard -> {
                    event.birthdayCard?.let { card ->
                        showBirthDayCard(card)
                    }
                }
                is HomeViewModel.UiUpdate.AllowReferral -> {
                    hideProgressDialog()
                    if (event.status) {
                        showReferralBs(event.message!!)
                    } else {
                        showErrorSnackBar(event.message!!, true)
                    }
                }
                is HomeViewModel.UiUpdate.ReferralStatus -> {
                    hideProgressDialog()
                    if (event.status) {
                        dialogBsAddReferral.dismiss()
                        showErrorSnackBar(
                            "Referral added Successfully. Referral bonus will be added to your Wallet.",
                            false,
                            Constants.LONG
                        )
                    } else {
                        showToast(this, "No account with the given number. Please check again")
                    }
                }
                else -> HomeViewModel.UiUpdate.Empty
            }
        }
        //observing the banners data and setting the banners
        viewModel.getAllBanners().observe(this, Observer {
            val bannersCarousel: MutableList<CarouselItem> = mutableListOf()
            for (banner in it) {
                //creating a mutable list banner carousel item
                bannersCarousel.add(
                    CarouselItem(
                        imageUrl = banner.url
                    )
                )
            }
            generateBanners(bannersCarousel, it)
        })
        viewModel.getALlCategories().observe(this, Observer {
            adapter.setCategoriesData(it)
        })
    }

    private fun showBirthDayCard(card: BirthdayCard?) {
        card?.let {
            BirthdayCardDialog.newInstance(card).show(supportFragmentManager, "cardDialog")
            viewModel.updateBirthdayCard(card.customerID)
        }
    }

    //after generating the banners from the viewModel we are assigning the classes to carousel items and settnig click listeners for it
    private fun generateBanners(
        bannerCarouselItems: MutableList<CarouselItem>,
        bannerItems: List<BannerEntity>
    ) {
        //adding the data to carousel items
        binding.cvBanner.addData(bannerCarouselItems)
        //registering the lifecycle to prevent memory leak
        binding.cvBanner.registerLifecycle(this)
        binding.cvBanner.carouselListener = object : CarouselListener {
            override fun onClick(position: Int, carouselItem: CarouselItem) {
                selectedBanner(bannerItems[position])
            }
        }
    }

    private fun generateRecyclerView() {
        adapter = CategoryHomeAdapter(
            this,
            listOf(),
            this
        ).also { adapter ->
            binding.rvHomeItems.layoutManager = GridLayoutManager(this, 3)
            binding.rvHomeItems.adapter = adapter
            binding.rvHomeItems.setItemViewCacheSize(20)
        }
        homeSpecialsAdapter = HomeSpecialsAdapter(
            this,
            mutableListOf(),
            mutableListOf(),
            mutableListOf(),
            this,
            this
        ).also {
            binding.rvHomeSpecials.layoutManager = LinearLayoutManager(this)
            binding.rvHomeSpecials.adapter = it
            binding.rvHomeSpecials.setItemViewCacheSize(20)
        }
        testimonialsAdapter = TestimonialsAdapter(
            mutableListOf(),
            this
        ).also {
            binding.rvTestimonials.layoutManager = LinearLayoutManager(this)
            binding.rvTestimonials.adapter = it
        }
        partnersAdapter = PartnersAdapter(
            listOf(),
            this
        ).also {
            binding.rvPartners.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvPartners.adapter = it
        }
/*
    //This will lock the vertical scrolling when horizontal child is scrolling and vice versa
            binding.rvHomeItems.setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
*/
//        val snapHelper: SnapHelper = GravitySnapHelper(Gravity.TOP)
//        snapHelper.attachToRecyclerView(binding.rvHomeSpecials)
    }

    override fun showAllProducts() {
        moveToAllProducts()
    }

    override fun selectedSpecialBanner(banner: BannerEntity) {
        selectedBanner(banner)
    }

    private fun showReferralBs(currentUserID: String) {
        //BS to add referral number
        dialogBsAddReferral = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogBottomAddReferralBinding = DataBindingUtil.inflate(
            LayoutInflater.from(applicationContext),
            R.layout.dialog_bottom_add_referral,
            null,
            false
        )
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
                showProgressDialog(false)
                viewModel.applyReferralNumber(currentUserID, code)
            }
        }

        dialogBsAddReferral.show()
    }

    override fun onBackPressed() {
        when {
            binding.dlDrawerLayout.isDrawerOpen(GravityCompat.START) ->
                binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
            else -> {
                finish()
                finishAffinity()
                super.onBackPressed()
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

    private fun moveToAllProducts() {
        lifecycleScope.launch {
            delay(150)
            navigateToSelectedCategory(ALL_PRODUCTS)
        }
    }

    private fun navigateToCWM() {
        if (NetworkHelper.isOnline(this@HomeActivity)) {
            lifecycleScope.launch {
                delay(200)
                Intent(this@HomeActivity, AllCWMActivity::class.java).also {
                    startActivity(it)
                }
            }
        } else {
            showErrorSnackBar("Please check your Network Connection", true)
        }
    }

    private fun navigateToWallet() {
        if (NetworkHelper.isOnline(this@HomeActivity)) {
            lifecycleScope.launch {
                delay(200)
                Intent(this@HomeActivity, WalletActivity::class.java).also {
                    startActivity(it)
                }
            }
        } else {
            showErrorSnackBar("Please check your Network Connection", true)
        }
    }

    private fun navigateToQuickOrder() {
        if (NetworkHelper.isOnline(this@HomeActivity)) {
            lifecycleScope.launch {
                delay(200)
                Intent(this@HomeActivity, QuickOrderActivity::class.java).also {
                    startActivity(it)
                }
            }
        } else {
            showErrorSnackBar("Please check your Network Connection", true)
        }
    }

    private fun navigateToSelectedCategory(category: String) {
        Intent(this, ShoppingMainActivity::class.java).also {
            it.putExtra(CATEGORY, category)
            it.putExtra(NAVIGATION, HOME_PAGE)
            startActivity(it)
        }
    }

    private fun navigateToProductDetails(
        productID: String,
        productName: String,
        thumbnail: ShapeableImageView?
    ) {
        Intent(this, ProductActivity::class.java).also { intent ->
            intent.putExtra(PRODUCTS, productID)
            intent.putExtra(Constants.PRODUCT_NAME, productName)
            thumbnail?.let { image ->
                val options: ActivityOptionsCompat =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        image,
                        ViewCompat.getTransitionName(image)!!
                    )
                if (Build.MANUFACTURER == "Xiaomi") {
                    startActivity(intent)
                } else {
                    startActivity(intent, options.toBundle())
                }
            } ?: let {
                startActivity(intent)
            }
        }
    }

    private fun navigateToPreview(
        url: String,
        thumbnail: ShapeableImageView?,
        carouselItem: ImageCarousel?,
        contentType: String
    ) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", contentType)
            thumbnail?.let {
                val options: ActivityOptionsCompat =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, it, "video")
                startActivity(intent, options.toBundle())
            }
            carouselItem?.let {
                val options: ActivityOptionsCompat =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, it, "thumbnail")
                startActivity(intent, options.toBundle())
            }
//            startActivity(it)
        }
    }

    override fun onResume() {
        viewModel.getAllNotifications()
        super.onResume()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (NetworkHelper.isOnline(this)) {
            when (item.itemId) {
                R.id.menuLogOut -> {
                    if (NetworkHelper.isOnline(this@HomeActivity)) {
                        SharedPref(this).clearAllData()
                        viewModel.signOut()
                        finishAffinity()
                        finish()
                    } else {
                        showErrorSnackBar("Please check your Network Connection", true)
                    }
                }
                R.id.menuProfile -> {
                    if (NetworkHelper.isOnline(this@HomeActivity)) {
                        lifecycleScope.launch {
//                            delay(200)
                            Intent(this@HomeActivity, ProfileActivity::class.java).also {
                                startActivity(it)
                            }
                        }
                    } else {
                        showErrorSnackBar("Please check your Network Connection", true)
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
                    if (NetworkHelper.isOnline(this@HomeActivity)) {
                        lifecycleScope.launch {
                            delay(200)
                            Intent(this@HomeActivity, ChatActivity::class.java).also {
                                startActivity(it)
                            }
                        }
                    } else {
                        showErrorSnackBar("Please check your Network Connection", true)
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
                    showExitSheet(
                        this,
                        "Magizhini Referral Program Offers Customers Referral Bonus Rewards for each successful New Customer using your PHONE NUMBER as Referral Code. Both You and any New Customer using your phone number as Referral ID will received Exciting Referral Bonus after their first delivery! Click Proceed to Continue"
                    )
                }
                R.id.menuSubDetails -> {
                    binding.dlDrawerLayout.closeDrawer(GravityCompat.START)
                    showDescriptionBs(resources.getString(R.string.subscription_info))
                }
                R.id.menuPrivacyPolicy -> {
                    lifecycleScope.launch {
                        delay(200)
                        openInBrowser("https://rama-2402.github.io/privacy-policy/")
                    }
                }
                R.id.menuDisclaimer -> {
                    lifecycleScope.launch {
                        delay(200)
                        openInBrowser("https://rama-2402.github.io/disclaimer/")
                    }
                }
                R.id.menuTermsOfUse -> {
                    lifecycleScope.launch {
                        delay(200)
                        openInBrowser("https://rama-2402.github.io/terms-of-use/")
                    }
                }
                R.id.menuReturn -> {
                    lifecycleScope.launch {
                        delay(200)
                        openInBrowser("https://rama-2402.github.io/return-policy/")
                    }
                }
                R.id.menuContactUs -> {
                    showListBottomSheet(this, arrayListOf("Call", "WhatsApp", "E-Mail"), "contact")
                }
                R.id.menuContactDeveloper -> {
                    showListBottomSheet(this, arrayListOf("WhatsApp", "E-Mail"), "developer")
                }
                R.id.menuAboutUs -> {
                    showDescriptionBs(
                        "ABOUT US \n\n  Magizhini Organics is a retail-focused Store offering food and related consumables produced from organics farming and Certified Organic food producers according to organic farming standards. \n\n\nABOUT ORGANIC FOOD:\n" +
                                "\n" +
                                "Any food product cultivated relying entirely on natural methods without compromising on the consumer's health can be called 'Organic' food. In other words, no chemicals, no unnatural fertilizers or pesticides and no farming methods that are not humane.\n" +
                                "\n" +
                                "Cultivating food the organic way is not as expensive or not viable as some make it out to be. Though it does cost the farmer more than the usual chemical dependent methods, at the same time it should not also be priced out of the reach of the ordinary consumer. Assuming that the ruling market prices for conventionally-grown food (read chemically-grown food) are fair, it is important that organic food also be prized more or less similar for pushing the customers towards more healthy organic intake, especially when consumers are aware that organic food is better than chemically-grown food in all respects, including taste, flavour and for their own health, besides that of the earth.\n" +
                                "\n" +
                                "Another aspect of the organic food 'issue' at least in India is a common problem faced by organic farmers: the lack of a ready market and often unremunerative prices for their produce. In many cases, the grower does not receive timely payments from middlemen including organic food traders. Interested buyers of organic food on the other hand, cannot find what they need, at least not at reasonable prices. Supplies are often erratic or unreliable and in some cases buyers are not even sure if the food they are buying is indeed organic.\n" +
                                "\n" +
                                "Taking all the above into account, we, Magizhini Organics, have started this initiative to supply organic food and food products in Chennai with a single click of button from your mobile in the comfort of your home. This is an attempt to link growers and processors with buyers to ensure a fair price for growers and easier availability of a wide variety of organic food at reasonable prices for buyers."
                    )
                }
                R.id.menuBecomePartner -> {
                    showExitSheet(this, "Become a Business Partner with Magizhini Organics by providing your contact details and a short description of your business in the form next page. We will Contact you shortly once reviewing your profile. Please click PROCEED to continue", "business")
                }
                R.id.menuCareers -> {
                    openInBrowser("https://forms.gle/3EffsX681hftF3SR9")
                }
            }
            return true
        } else {
            showErrorSnackBar("Please check network connection", true)
            return false
        }
    }

    override fun openVideo(url: String, thumbnail: ShapeableImageView) {
        navigateToPreview(url, thumbnail, null, "video")
    }

    fun selectedContactMethodForDeveloper(selectedItem: String) {
        when (selectedItem) {
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

    fun addNewPartnerAccount() {
        openInBrowser("https://forms.gle/eaCWzYVCetunTigd9")
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
            Toast.makeText(this, "Email App is not installed in your phone.", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }

    fun referralAction(selectedItem: String) {
        val phoneNumber = SharedPref(this).getData(PHONE_NUMBER, STRING, "").toString()
        if (selectedItem == "Share My Referral Code") {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My application name")
                var shareMessage =
                    "\nHey I'm using Magizhini Organics for my Organic Food Purchases. Check it out! You can use my number $phoneNumber as Referral Number to get Exciting Referral Bonus!\n\n"
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
        showListBottomSheet(
            this,
            arrayListOf("Share My Referral Code", "Have a Referral Code? Enter here..."),
            "referral"
        )
    }

    private fun openPreview(imageUrl: String, carouselItem: ImageCarousel) {
        navigateToPreview(imageUrl, null, carouselItem, "image")
    }

    //from partners adapter
    override fun selectedPartner(partner: Partners, thumbnail: ShapeableImageView) {
        when (partner.clickAction) {
            "Open" -> navigateToPreview(partner.imageUrl, thumbnail, null, "image")
        }
    }

    override fun moveToProductDetails(
        productID: String,
        productName: String,
        thumbnail: ShapeableImageView
    ) {
        navigateToProductDetails(productID, productName, thumbnail)
    }

    //from categories adapter
    override fun selectedCategory(categoryName: String) {
        navigateToSelectedCategory(categoryName)
    }
}
