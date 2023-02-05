package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.FoodSubscriptionAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.FoodSubscriptionItemClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Banner
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.MenuImage
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityFoodSubscriptionBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Converters
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class FoodSubscriptionActivity :
    BaseActivity(),
    KodeinAware,
    FoodSubscriptionItemClickListener
{

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityFoodSubscriptionBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel
    private var foodType: String = ""

//    private var lunchPrice: Double = 0.0
//    private var dinnerPrice: Double = 0.0
//    private var lunchWoRicePrice: Double = 0.0
//    private var currentPlan: String = "premium"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_subscription)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

        foodType = intent.getStringExtra("food")!!

        initData()
        initLiveData()
        initListeners()
    }

    private fun initData() {
        if (foodType == "aachi") {
            binding.tvToolbarTitle.text = "Amma Samayal (Non-Veg)"
            binding.tvToolbarTitleSubText.text = "Authentic Home-Made Non-Veg Food"
        }
        showProgressDialog(true)
        viewModel.getAmmaSpecials(foodType)
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            btnNext.setOnClickListener {
                viewModel.ammaSpecials.sortedBy { it.displayOrder }
                btnNext.startAnimation(
                    AnimationUtils.loadAnimation(
                        binding.btnNext.context,
                        R.anim.bounce
                    )
                )
                if (foodType == "amma") {
                    Intent(this@FoodSubscriptionActivity, FoodOrderActivity::class.java).also {
                        it.putExtra("menu", Converters().menuToStringConverter(viewModel.ammaSpecials))
                        startActivity(it)
                    }
                } else {
                    Intent(this@FoodSubscriptionActivity, NVFoodOrderActivity::class.java).also {
                        it.putExtra("menu", Converters().menuToStringConverter(viewModel.ammaSpecials))
                        startActivity(it)
                    }
                }
           }
            ivHistory.setOnClickListener {
                Intent(this@FoodSubscriptionActivity, FoodSubHistoryActivity::class.java).also {
                    startActivity(it)
                }
            }
//            btnBudgetPlan.setOnClickListener {
//                btnBudgetPlan.setBackgroundColor(ContextCompat.getColor(this@FoodSubscriptionActivity, R.color.matteRed))
//                btnPremiumPlan.setBackgroundColor(ContextCompat.getColor(this@FoodSubscriptionActivity, R.color.green_base))
//                populateRecipes(viewModel.budgetPlanRecipes)
//            }
//            btnPremiumPlan.setOnClickListener {
//                btnPremiumPlan.setBackgroundColor(ContextCompat.getColor(this@FoodSubscriptionActivity, R.color.matteRed))
//                btnBudgetPlan.setBackgroundColor(ContextCompat.getColor(this@FoodSubscriptionActivity, R.color.green_base))
//                populateRecipes(viewModel.premiumPlanRecipes)
//            }
        }
    }

    private fun initLiveData() {
        viewModel.uiEvent.observe(this) { event ->
            when(event) {
                is UIEvent.Toast -> showToast(this, event.message, event.duration)
                is UIEvent.SnackBar -> showErrorSnackBar(event.message, event.isError)
                is UIEvent.ProgressBar -> {
                    if (event.visibility) {
                        showProgressDialog(true)
                    } else {
                        hideProgressDialog()
                    }
                }
                is UIEvent.EmptyUIEvent -> return@observe
                else -> Unit
            }
            viewModel.setEmptyUiEvent()
        }
        viewModel.uiUpdate.observe(this) { event ->
            when(event) {
                is FoodSubscriptionViewModel.UiUpdate.PopulateAmmaSpecials -> {
                    populateBanners(event.banners)
                    event.ammaSpecials?.let { specials ->
                        if (specials.isEmpty()) {
                            binding.tvFoodStatus.visible()
                            binding.svBody.remove()
                        } else {
                            binding.tvFoodStatus.remove()
                            binding.svBody.visible()

                            populateRecipes(specials)

//                            if (currentPlan == "budget") {
//                                populateRecipes(viewModel.budgetPlanRecipes)
//                            } else {
//                                populateRecipes(viewModel.premiumPlanRecipes)
//                            }

//                            specials.forEach {
//                                if (it.foodTime.lowercase().contains("rice")) {
//                                    lunchWoRicePrice = it.discountedPrice
//                                }
//                                if (it.foodTime.lowercase() == "lunch") {
//                                    lunchPrice = it.discountedPrice
//                                }
//                                if (it.foodTime.lowercase() == "dinner") {
//                                    dinnerPrice = it.discountedPrice
//                                }
//                            }
                        }
                   }?: showErrorSnackBar("Server Error! Please try again later", true)
                    hideProgressDialog()
                }
                is FoodSubscriptionViewModel.UiUpdate.Empty -> {
                    return@observe
                }
                else -> {
                    viewModel.setEmptyStatus()
                }
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun populateRecipes(recipes: List<MenuImage>) {
        FoodSubscriptionAdapter(recipes.sortedBy { it.displayOrder }, this).let { adapter ->
                                    binding.rvFoods.adapter = adapter
                                    binding.rvFoods.layoutManager = GridLayoutManager(this, 2)
                                }
    }

    private fun populateBanners(banners: List<Banner>?) {
        val bannersCarousel: MutableList<CarouselItem> = mutableListOf()
        banners?.forEach { banner ->
            bannersCarousel.add(
                CarouselItem(
                    imageUrl = banner.url
                )
            )
        }
        binding.cvBanner.addData(bannersCarousel)
        binding.cvBanner.registerLifecycle(this)
    }

    override fun itemClicked(url: String, thumbnail: ShapeableImageView) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, thumbnail, "thumbnail")
            startActivity(intent, options.toBundle())
//            startActivity(it)
        }
    }
}