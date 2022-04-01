package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeSpecialsItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadSimple

class HomeSpecialsAdapter(
    val context: Context,
    var bestSellers: MutableList<List<ProductEntity>>,
    var titles: MutableList<String>,
    var banners: MutableList<BannerEntity>,
//    var banners: MutableList<List<BannerEntity>>,
    val onItemClickListener: HomeSpecialsItemClickListener,
    private val bestSellerItemClickListener: BestSellersAdapter.BestSellerItemClickListener
): RecyclerView.Adapter<HomeSpecialsAdapter.HomeSpecialsViewHolder>() {

    inner class HomeSpecialsViewHolder(val binding: RvHomeSpecialsItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeSpecialsViewHolder {
        val view = RvHomeSpecialsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeSpecialsViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeSpecialsViewHolder, position: Int) {
        val bestSellerList = bestSellers[position]
        val title = titles[position]
//        val banner = banners[position]
        holder.binding.apply {
            tvBestSellers.text = title

            setBestSeller(rvTopPurchases, bestSellerList)

            ivBannerOne.loadSimple(banners[getBannerPosition(position, 0)].url)
            ivBannerTwo.loadSimple(banners[getBannerPosition(position, 1)].url)
            ivBannerThree.loadSimple(banners[getBannerPosition(position, 2)].url)


//            ivBannerOne.loadSimple(banner[0].url)
//            ivBannerTwo.loadSimple(banner[1].url)
//            ivBannerThree.loadSimple(banner[2].url)

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
                onItemClickListener.selectedSpecialBanner(banners[getBannerPosition(position, 0)])
            }
            ivBannerTwo.setOnClickListener {
                onItemClickListener.selectedSpecialBanner(banners[getBannerPosition(position, 1)])
            }
            ivBannerThree.setOnClickListener {
                onItemClickListener.selectedSpecialBanner(banners[getBannerPosition(position, 2)])
            }
        }
    }

    private fun getBannerPosition(position: Int, bannerNumber: Int): Int {
        return when (position) {
            0 -> {
                return when (bannerNumber) {
                    0 -> 0
                    1 -> 1
                    2 -> 2
                    else -> 0
                }
            }
            1 -> {
                return when (bannerNumber) {
                    0 -> 3
                    1 -> 4
                    2 -> 5
                    else -> 3
                }
            }
            2 -> {
                return when (bannerNumber) {
                    0 -> 6
                    1 -> 7
                    2 -> 8
                    else -> 6
                }
            }
            3 -> {
                return when (bannerNumber) {
                    0 -> 9
                    1 -> 10
                    2 -> 11
                    else -> 9
                }
            }
            else -> 0
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

    fun setBestSellerData(
        newBanners: MutableList<BannerEntity>,
//        newBanners: MutableList<List<BannerEntity>>,
        newBestSellers: MutableList<List<ProductEntity>>,
        newList: List<String>
    ) {
        val diffUtil = BestSellersDiffUtils(titles, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        titles = newList as ArrayList<String>
        banners = newBanners
        bestSellers = newBestSellers
        diffResult.dispatchUpdatesTo(this)
    }

    class BestSellersDiffUtils(
        private val oldList: List<String>,
        private val newList: List<String>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition] != newList[newItemPosition] -> false
                else -> true
            }
        }
    }

}
