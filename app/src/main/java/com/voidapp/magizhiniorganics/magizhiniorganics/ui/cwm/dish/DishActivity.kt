package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish

import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayoutMediator
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.DishViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityDishBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.AllCWMActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView

/*
* TODO
*  Need to add reviews like in product sub page instead of the cart page
* */
class DishActivity :
    BaseActivity(),
    KodeinAware,
    ReviewAdapter.ReviewItemClickListener
{
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityDishBinding
    private lateinit var viewModel: DishViewModel
    private val factory: DishViewModelFactory by instance()

    private lateinit var cartAdapter: CartAdapter

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var checkoutText: TextView
    private lateinit var filterBtn: ImageView

    private var player: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dish)
        viewModel = ViewModelProvider(this, factory)[DishViewModel::class.java]

        viewModel.dish = intent.getParcelableExtra<CWMFood>("dish")!!
        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        initData()
        initRecyclerView()
        initViewPager()
        initObservers()
        initListeners()
    }

    private fun initObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when(result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> {
                        if (result.message == "") {
                            showProgressDialog()
                        } else {
                            showSuccessDialog("", result.message, result.data)
                        }
                    }
                    else -> Unit
                }
            }
        }

        viewModel.storagePermissionCheck.observe(this) { check ->
            check?.let {
                if (PermissionsUtil.hasStoragePermission(this)) {
                    showToast(this, "Storage Permission Granted")
                    viewModel.previewImage("granted")
                } else {
                    showExitSheet(this, "The App Needs Storage Permission to access Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                }
                viewModel.setStoragePermission(null)
            }
        }
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            KeyboardVisibilityEvent.setEventListener(this@DishActivity
            ) { isOpen ->
                if (isOpen) {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                    player?.pause()
                    binding.youtubePlayerView.remove()
                } else {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    player?.play()
                    binding.youtubePlayerView.visible()
                }
            }
        }
    }

    private fun initData() {
        viewModel.reviewAdapter = ReviewAdapter(
            this,
            arrayListOf(),
            this
        )

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                player = youTubePlayer
                youTubePlayer.loadVideo(viewModel.dish!!.videoID, 0f)
            }
        })

        viewModel.cartItems.clear()
        for (item in viewModel.dish!!.ingredients) {
            viewModel.cartItems.add(item.toCartEntity())
        }

        viewModel.getUserProfile()
    }

    private fun populateDishDetails() {
        binding.apply {
            tvToolbarTitle.text = viewModel.dish?.dishName
        }
    }

    private fun populateCartBottomSheet() {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        cartBtn = findViewById(R.id.ivCart)
        filterBtn = findViewById<ImageView>(R.id.ivFilter)

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartRecycler.adapter = cartAdapter

        cartBottomSheet.isDraggable = true

        cartBottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        checkoutText.setTextAnimation(
                            "Rs: ${viewModel.dish!!.totalPrice}",
                            ProductActivity.ANIMATION_DURATION
                        )
                        player?.pause()
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        checkoutText.setTextAnimation("CHECKOUT",
                            ProductActivity.ANIMATION_DURATION
                        )
                        player?.play()
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        cartBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            cartBottomSheet.state = if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.STATE_COLLAPSED
            } else {
                BottomSheetBehavior.STATE_EXPANDED
            }
        }

        filterBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            cartBottomSheet.state = if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.STATE_COLLAPSED
            } else {
                BottomSheetBehavior.STATE_EXPANDED
            }
        }

        checkoutBtn.setOnClickListener {
            if (NetworkHelper.isOnline(this)) {
                Intent(this, InvoiceActivity::class.java).also {
                    it.putParcelableArrayListExtra("dish", viewModel.cartItems as ArrayList<CartEntity>)
                    it.putExtra("cwm", true)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            } else {
                showErrorSnackBar("Please check network connection", true)
            }
        }
    }

    private fun initRecyclerView() {
        cartAdapter = CartAdapter(
            this,
            mutableListOf(),
            viewModel
        )
        populateDishDetails()
        populateCartBottomSheet()
        populateRecyclerView(viewModel.cartItems)
    }

    private fun populateRecyclerView(ingredients: MutableList<CartEntity>) {
        cartBtn.badgeValue = viewModel.getCartSize(ingredients)
        cartAdapter.cartItems = ingredients
        cartAdapter.notifyDataSetChanged()
    }

    private fun initViewPager() {
        val adapter = DishViewPager(supportFragmentManager, lifecycle)
        binding.vpFragmentContent.adapter = adapter
        TabLayoutMediator(binding.tlTabLayout, binding.vpFragmentContent) { tab, position ->
            when(position) {
                0 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_reviews)
                1 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_write_review)
            }
        }.attach()
    }

    override fun previewImage(url: String, thumbnail: ShapeableImageView) {
        player?.pause()
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, thumbnail, ViewCompat.getTransitionName(thumbnail)!!)
            startActivity(intent, options.toBundle())
        }
    }

    private suspend fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "image" -> {
                hideProgressDialog()
                showToast(this, data as String)
            }
            "status" -> {
                hideProgressDialog()
            }
            "update" -> {
                data as MutableList<CartEntity>
                checkoutText.text = "Rs: ${viewModel.dish!!.totalPrice}"
                populateRecyclerView(data)
            }

        }

        viewModel.setEmptyStatus()
    }

    private suspend fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "image" -> {
                hideProgressDialog()
                showErrorSnackBar(data as String, true)
            }
            "status" -> {
                delay(1000)
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
        }
        viewModel.setEmptyStatus()
    }

    override fun onBackPressed() {
        when {
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED ->
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            else -> super.onBackPressed()
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

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isFinishing) {
            Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
        }
        super.onStop()
    }

    override fun onDestroy() {
        viewModel.apply {
            dish = null
            userProfile = null
            reviewAdapter = null
        }
        super.onDestroy()
    }

}