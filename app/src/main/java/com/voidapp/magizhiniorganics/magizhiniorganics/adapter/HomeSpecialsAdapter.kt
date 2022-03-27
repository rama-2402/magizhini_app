package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AbsListView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeSpecialsItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.URL

class HomeSpecialsAdapter(
    val context: Context,
//    var bestSellers: MutableList<List<ProductEntity>>,
    var titles: MutableList<String>,
//    var banners: MutableList<List<BannerEntity>>,
    val onItemClickListener: HomeSpecialsItemClickListener,
    private val bestSellerItemClickListener: BestSellersAdapter.BestSellerItemClickListener
): RecyclerView.Adapter<HomeSpecialsAdapter.HomeSpecialsViewHolder>() {

    val bsOne: MutableList<ProductEntity> = mutableListOf()
    val bsTwo: MutableList<ProductEntity> = mutableListOf()
    val bsThree: MutableList<ProductEntity> = mutableListOf()
    val bsFour: MutableList<ProductEntity> = mutableListOf()

    val bannerOne: MutableList<BannerEntity> = mutableListOf()
    val bannerTwo: MutableList<BannerEntity> = mutableListOf()
    val bannerThree: MutableList<BannerEntity> = mutableListOf()
    val bannerFour: MutableList<BannerEntity> = mutableListOf()

    private val viewPool = RecyclerView.RecycledViewPool()

    inner class HomeSpecialsViewHolder(val binding: RvHomeSpecialsItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeSpecialsViewHolder {
        val view = RvHomeSpecialsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeSpecialsViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeSpecialsViewHolder, position: Int) {
//        val bestSellerList = bestSellers[position]
        val title = titles[position]
//        val banner = banners[position]
        holder.binding.apply {
            tvBestSellers.text = title

            rvTopPurchases.setRecycledViewPool(viewPool)

            when(position) {
                0 -> populateData(holder, bsOne, bannerOne)
                1 -> populateData(holder, bsTwo, bannerTwo)
                2-> populateData(holder, bsThree, bannerThree)
                3 -> populateData(holder, bsFour, bannerFour)
            }

        }
    }

    private fun populateData(holder: HomeSpecialsViewHolder, bestSellers: MutableList<ProductEntity>, banner: MutableList<BannerEntity>) {
        holder.binding.apply {
            setBestSeller(rvTopPurchases, bestSellers)
                ivBannerOne.loadImg(banner[0].url){}
                ivBannerTwo.loadImg(banner[1].url){}
                ivBannerThree.loadImg(banner[2].url){}

                cpBestSellers.setOnClickListener {
                    it.startAnimation(
                        AnimationUtils.loadAnimation(
                            it.context,
                            R.anim.bounce
                        )
                    )
                    onItemClickListener.showAllProducts()
                }

                ivBannerOne.setOnClickListener {
                    onItemClickListener.selectedSpecialBanner(banner[0])
                }
                ivBannerTwo.setOnClickListener {
                    onItemClickListener.selectedSpecialBanner(banner[1])
                }
                ivBannerThree.setOnClickListener {
                    onItemClickListener.selectedSpecialBanner(banner[2])
                }
            }
    }

    override fun getItemCount(): Int {
        return titles.size
    }

    private fun setBestSeller(recyclerView: RecyclerView, bestSellerList: List<ProductEntity>){
        val bestSellerAdapter = BestSellersAdapter(context, bestSellerList, bestSellerItemClickListener)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recyclerView.adapter = bestSellerAdapter

    }

    interface HomeSpecialsItemClickListener {
        fun showAllProducts()
        fun selectedSpecialBanner(banner: BannerEntity)
    }

//
//    fun setBestSellerData(
//        newBanners: MutableList<List<BannerEntity>>,
//        newBestSellers: MutableList<List<ProductEntity>>,
//        newList: List<String>
//    ) {
//        val diffUtil = BestSellersDiffUtils(titles, newList)
//        val diffResult = DiffUtil.calculateDiff(diffUtil)
//        titles = newList as ArrayList<String>
//        banners = newBanners
//        bestSellers = newBestSellers
//        diffResult.dispatchUpdatesTo(this)
//    }
//
//    class BestSellersDiffUtils(
//        private val oldList: List<String>,
//        private val newList: List<String>
//    ): DiffUtil.Callback() {
//        override fun getOldListSize(): Int {
//            return oldList.size
//        }
//
//        override fun getNewListSize(): Int {
//            return newList.size
//        }
//
//        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            return oldList[oldItemPosition] == newList[newItemPosition]
//        }
//
//        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            return when {
//                oldList[oldItemPosition] != newList[newItemPosition] -> false
//                else -> true
//            }
//        }
//    }

}
