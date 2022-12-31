package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AllCWMAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityAllCwmBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Converters
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.delay
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*

class AllCWMActivity :
    BaseActivity(),
    KodeinAware,
    AllCWMAdapter.CWMClickListener {

    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityAllCwmBinding
    private lateinit var viewModel: CWMViewModel
    private val factory: CWMViewModelFactory by instance()

    private lateinit var allCWMAdapter: AllCWMAdapter

    private var item: MenuItem? = null
    private val mFilteredItems: MutableList<CWMFood> = mutableListOf()
    private val mItems: MutableList<CWMFood> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_cwm)
        viewModel = ViewModelProvider(this, factory)[CWMViewModel::class.java]

        title = ""
        setSupportActionBar(binding.tbToolbar)

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            onBackPressed()
        }

        initData()
        initRecyclerView()
        initObservers()
        initListeners()
    }

    private fun initObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when (result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> {
                        if (result.message == "") {
                            showProgressDialog(true)
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
                collapseSearchBar()
                onBackPressed()
            }
//            ivHowTo.setOnClickListener {
//                viewModel.getHowToVideo("AllCwm")
//            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        item = menu.findItem(R.id.btnSearch)
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
                    mItems.forEach loop@{ it ->
                        if (it.dishName.lowercase().contains(searchText)) {
                            mFilteredItems.add(it)
                        }
                    }
                    allCWMAdapter.dishes = mFilteredItems
                    allCWMAdapter.notifyDataSetChanged()
                } else {
                    mFilteredItems.clear()
                    allCWMAdapter.dishes = mItems
                    allCWMAdapter.notifyDataSetChanged()
                }
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun collapseSearchBar() {
        if (item?.isActionViewExpanded == true) {
            item!!.collapseActionView()
        }
    }

    private fun navigateToDish(dish: CWMFood) {
        Intent(this@AllCWMActivity, DishActivity::class.java).also {
            it.putExtra("dish", Converters().cwmToStringConverter(dish))
            startActivity(it)
        }
    }

    private fun initData() {
        viewModel.getAllCWMDishes()
    }

    private fun initRecyclerView() {
        allCWMAdapter = AllCWMAdapter(
            this,
            mutableListOf(),
            this
        )

        binding.apply {
            rvCWM.layoutManager = LinearLayoutManager(this@AllCWMActivity)
            rvCWM.adapter = allCWMAdapter
        }
    }

    private fun populateRecyclerView(dishes: MutableList<CWMFood>) {
        mItems.clear()
        mItems.addAll(dishes)
        allCWMAdapter.dishes = dishes
        allCWMAdapter.notifyDataSetChanged()
    }

    private suspend fun onSuccessCallback(message: String, data: Any?) {
        when (message) {
            "status" -> {
                hideProgressDialog()
                data as MutableList<CWMFood>
                if (data.isNullOrEmpty()) {
                    showToast(this, "No Dishes available to display")
                } else {
                    populateRecyclerView(data)
                }
            }
            "how" -> {
                hideProgressDialog()
                data as String
                if (data.isNullOrEmpty()) {
                    showToast(
                        this,
                        "demo video will be available soon. sorry for the inconvenience."
                    )
                } else {
                    openInBrowser(data)
                }
            }
        }

        viewModel.setEmptyStatus()
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

    private suspend fun onFailedCallback(message: String, data: Any?) {
        when (message) {
            "status" -> {
                delay(1000)
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "how" -> {
                hideProgressDialog()
                showToast(this, "Demo Video will be available soon. Sorry for the inconvenience.")
            }
        }
        viewModel.setEmptyStatus()
    }

    override fun selectedItem(dish: CWMFood) {
        collapseSearchBar()
        navigateToDish(dish)
    }
}