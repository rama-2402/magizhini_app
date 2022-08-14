package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.FoodSubscriptionAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.FoodSubscriptionItemClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Banner
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityFoodSubscriptionBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_subscription)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

        initData()
        initLiveData()
        initListeners()
    }

    private fun initData() {
        showProgressDialog(true)
        viewModel.getAmmaSpecials()
    }

    private fun initListeners() {
        binding.apply {
            ivHistory.setOnClickListener {
                Intent(this@FoodSubscriptionActivity, FoodOrderActivity::class.java).also {
                    startActivity(it)
                }
            }
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
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
                        FoodSubscriptionAdapter(specials.sortedBy { it.displayOrder }, this).let { adapter ->
                            binding.rvFoods.adapter = adapter
                            binding.rvFoods.layoutManager = LinearLayoutManager(this)
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

    override fun itemClicked() {
        Intent(this, FoodOrderActivity::class.java).also {
            it.putExtra("lunch", viewModel.lunchMap)
            startActivity(it)
        }
    }
}