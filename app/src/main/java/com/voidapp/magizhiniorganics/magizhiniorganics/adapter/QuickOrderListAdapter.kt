package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.HomeRvAdapter.CategoryHomeAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductCategoryEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvNotificationItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvQuickorderListItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.CartDiffUtil

class QuickOrderListAdapter(
    private val context: Context,
    var quickOrderList: List<Uri>,
    var quickOrderListUrl: List<String>,
    private val onItemClickListener: QuickOrderClickListener
): RecyclerView.Adapter<QuickOrderListAdapter.QuickOrderViewHolder>() {

//    private val diff = object: DiffUtil.ItemCallback<Uri>() {
//        override fun areItemsTheSame(
//            oldItem: Uri,
//            newItem: Uri
//        ): Boolean {
//            return oldItem == newItem
//        }
//
//        override fun areContentsTheSame(
//            oldItem: Uri,
//            newItem: Uri
//        ): Boolean {
//            return oldItem == newItem
//        }
//    }
//
//    val calcDiff = AsyncListDiffer(this, diff)

    inner class QuickOrderViewHolder(val binding: RvQuickorderListItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickOrderViewHolder {
        val view = RvQuickorderListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickOrderViewHolder, position: Int) {
        holder.binding.apply {
            if (quickOrderList.isNotEmpty()) {
                if (position == 0) {
                    ivDelete.visibility = View.GONE
                    view.visibility = View.GONE
                    tvListPageNumber.visibility = View.GONE
                    ivListPage.setImageDrawable(ContextCompat.getDrawable(ivListPage.context, R.drawable.ic_add))
                    ivListPage.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray500))
                    ivListPage.clipToOutline = true

                    ivListPage.setOnClickListener {
                        onItemClickListener.addImage()
                    }
                } else {
                    val uri = quickOrderList[position - 1]

                    tvListPageNumber.text = "Page: ${position}"

                    //loading the product images
                    GlideLoader().loadUserPicture(
                        context,
                        uri,
                        ivListPage
                    )
                    ivListPage.clipToOutline = true

                    ivListPage.setOnClickListener {
                        onItemClickListener.selectedListImage(position-1, uri)
                    }
                    ivDelete.setOnClickListener {
                        onItemClickListener.deleteListItem(position-1, uri)
                    }
                }
            } else {
                val url = quickOrderListUrl[position]

                tvListPageNumber.text = "Page: ${position + 1}"

                //loading the product images
                GlideLoader().loadUserPicture(
                    context,
                    url,
                    ivListPage
                )
                ivListPage.clipToOutline = true

                ivListPage.setOnClickListener {
                    onItemClickListener.selectedListImage(position, url)
                }
                ivDelete.setOnClickListener {
                    onItemClickListener.deleteListItem(position, url)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (quickOrderList.isNotEmpty()) {
            quickOrderList.size + 1
        } else {
            quickOrderListUrl.size
        }
    }

    interface QuickOrderClickListener {
        fun selectedListImage(position: Int, imageUri: Any)
        fun deleteListItem(position: Int, imageUri: Any)
        fun addImage()
    }
}