package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AllCWMAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.IngredientsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityAllCwmBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityDishBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.AllCWMActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.CWMViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.CWMViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView
import java.util.ArrayList

class DishActivity :
    BaseActivity(),
    KodeinAware,
    IngredientsAdapter.IngredientsClickListener
{

    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityDishBinding
    private lateinit var viewModel: DishViewModel
    private val factory: DishViewModelFactory by instance()

    private lateinit var ingredientsAdapter: IngredientsAdapter
    private lateinit var cartAdapter: CartAdapter

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var checkoutText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dish)
        viewModel = ViewModelProvider(this, factory)[DishViewModel::class.java]

        viewModel.dish = intent.getParcelableExtra<CWMFood>("dish")!!
        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        initData()
        initRecyclerView()
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
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun initData() {
        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadVideo(viewModel.dish.videoID, 0f)
            }
        })

        viewModel.cartItems.clear()
        for (item in viewModel.dish.ingredients) {
            viewModel.cartItems.add(item.toCartEntity())
        }
    }

    private fun populateDishDetails() {
        binding.apply {
            tvToolbarTitle.text = viewModel.dish.dishName
        }
    }

    private fun populateCartBottomSheet() {
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
                        checkoutText.text = "Rs: ${viewModel.dish.totalPrice}"
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
                    it.putExtra(Constants.NAVIGATION, PRODUCTS)
                    it.putParcelableArrayListExtra("dish", viewModel.cartItems as ArrayList<CartEntity>)
                    it.putExtra("cwm", true)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    onPause()
                }
            } else {
                showErrorSnackBar("Please check network connection", true)
            }
        }
    }

    private fun initRecyclerView() {
        ingredientsAdapter = IngredientsAdapter(
            this,
            mutableListOf(),
            this
        )
        cartAdapter = CartAdapter(
            this,
            mutableListOf(),
            viewModel
        )

        binding.apply {
            rvIngredients.layoutManager = LinearLayoutManager(this@DishActivity)
            rvIngredients.adapter = ingredientsAdapter
        }
        populateDishDetails()
        populateCartBottomSheet()
        populateRecyclerView(viewModel.cartItems)
    }

    private fun populateRecyclerView(ingredients: MutableList<CartEntity>) {
        ingredientsAdapter.ingredients = ingredients
        ingredientsAdapter.notifyDataSetChanged()
        cartBtn.badgeValue = ingredients.size
        cartAdapter.cartItems = ingredients
        cartAdapter.notifyDataSetChanged()
    }


    private suspend fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "status" -> {
                hideProgressDialog()

            }
            "update" -> {
                data as MutableList<CartEntity>
                checkoutText.text = "Rs: ${viewModel.dish.totalPrice}"
                populateRecyclerView(data)
            }

        }

        viewModel.setEmptyStatus()
    }

    private suspend fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "status" -> {
                delay(1000)
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
        }
        viewModel.setEmptyStatus()
    }

    override fun selectedItem(productID: String) {
        //todo something
    }

    override fun setFavorites(productID: String) {
        //todo if favorites implemented
    }

    override fun onBackPressed() {
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            Intent(this, AllCWMActivity::class.java).also {
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
            }
        }
    }

    private fun Cart.toCartEntity() = CartEntity(
        id = id,
        variant = variant,
        productId = productId,
        productName = productName,
        thumbnailUrl = thumbnailUrl,
        quantity = quantity,
        maxOrderQuantity = maxOrderQuantity,
        price = price,
        originalPrice = originalPrice
    )
}