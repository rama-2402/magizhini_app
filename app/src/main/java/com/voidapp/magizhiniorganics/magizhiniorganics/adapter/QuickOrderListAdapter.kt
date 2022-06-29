package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrderTextItem
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvQuickOrderTextBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvQuickorderListItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg

class QuickOrderListAdapter(
    private val context: Context,
    var quickOrderList: List<Uri>,
    var quickOrderListUrl: List<String>,
    var addImage: Boolean,
    private val onItemClickListener: QuickOrderClickListener
): RecyclerView.Adapter<QuickOrderListAdapter.QuickOrderViewHolder>() {

    inner class QuickOrderViewHolder(val binding: RvQuickorderListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickOrderViewHolder {
        val view =
            RvQuickorderListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickOrderViewHolder, position: Int) {
        holder.binding.apply {
            if (addImage) {
                if (position == 0) {
                    ivDelete.visibility = View.GONE
                    view.visibility = View.GONE
                    tvListPageNumber.visibility = View.GONE
                    ivListPage.setImageDrawable(
                        ContextCompat.getDrawable(
                            ivListPage.context,
                            R.drawable.ic_add
                        )
                    )
                    ivListPage.imageTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray500))
                    ivListPage.clipToOutline = true

                    ivListPage.setOnClickListener {
                        onItemClickListener.addImage()
                    }
                } else {
                    val uri = quickOrderList[position - 1]

                    if (uri == Uri.EMPTY) {
                        cvProductItem.visibility = View.INVISIBLE
                        return@apply
                    } else {
                        cvProductItem.visibility = View.VISIBLE
                    }

                    tvListPageNumber.text = "Page: ${position}"

                    //loading the product images
                    ivListPage.loadImg(uri.toString()) {}
//                    ivListPage.clipToOutline = true

                    ivListPage.setOnClickListener {
                        onItemClickListener.selectedListImage(position - 1, uri, ivListPage)
                    }
                    ivDelete.setOnClickListener {
                        onItemClickListener.deleteListItem(position - 1, uri)
                    }
                }
            } else {
                val url = quickOrderListUrl[position]
                ivDelete.visibility = View.GONE
                tvListPageNumber.text = "Page: ${position + 1}"

                //loading the product images
                ivListPage.loadImg(url) {}
//                ivListPage.clipToOutline = true

                ivListPage.setOnClickListener {
                    onItemClickListener.selectedListImage(position, url, ivListPage)
                }
                ivDelete.setOnClickListener {
                    onItemClickListener.deleteListItem(position, url)
                }

            }
        }
    }

        override fun getItemCount(): Int {
//        return quickOrderList?.size ?: quickOrderListUrl?.size ?: quickOrderTextItems?.size ?: 1
            return if (quickOrderList.isNotEmpty()) {
                quickOrderList.size + 1
            } else {
                quickOrderListUrl.size
            }
        }
    }

interface QuickOrderClickListener {
    //quick order image item
    fun selectedListImage(position: Int, imageUri: Any, thumbnail: ShapeableImageView)
    fun deleteListItem(position: Int, imageUri: Any)
    fun addImage()

    //quick order text item
    fun removeTextItem(position: Int)
    fun updateTextItem(position: Int)
}

class QuickOrderTextAdapter(
    var isEditable: Boolean,
    var quickOrderTextItems: MutableList<QuickOrderTextItem>,
    val onItemClickListener: QuickOrderClickListener
): RecyclerView.Adapter<QuickOrderTextAdapter.QuickOrderTextItemViewHolder>() {

    inner class QuickOrderTextItemViewHolder(val binding: RvQuickOrderTextBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuickOrderTextItemViewHolder {
        val view =
            RvQuickOrderTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuickOrderTextItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickOrderTextItemViewHolder, position: Int) {
        holder.binding.apply {
            val textItem = quickOrderTextItems[position]
            tvProductName.text = "${position+1}. ${textItem.productName}"
            tvVariantName.text = "Type: ${textItem.variantName}"
            tvQuantity.text = "Qty: X${textItem.quantity}"

           btnDelete.isVisible = isEditable

            btnDelete.setOnClickListener {
               if(isEditable) {
                   onItemClickListener.removeTextItem(
                        position
                   )
               }
            }

            clTextItem.setOnClickListener {
                if (isEditable) {
                    onItemClickListener.updateTextItem(
                        position
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return quickOrderTextItems.size
    }

    fun deleteTextItem(position: Int) {
        var items: MutableList<QuickOrderTextItem>? =
            quickOrderTextItems.map { it.copy() } as ArrayList<QuickOrderTextItem>
        items?.let {
            it.removeAt(position)
            setQuickOrderData(it)
//            this.notifyItemRemoved(position)
            this.notifyDataSetChanged()
        }
        items = null
    }

    fun addTextItem(newTextItem: QuickOrderTextItem) {
        var items: MutableList<QuickOrderTextItem>? =
            quickOrderTextItems.map { it.copy() } as MutableList<QuickOrderTextItem>
        items?.let {
            it.add(newTextItem)
            setQuickOrderData(it)
        }
        items = null
    }

    fun updateTextItem(position: Int, updatedItem: QuickOrderTextItem) {
        var items: MutableList<QuickOrderTextItem>? =
            quickOrderTextItems.map { it.copy() } as MutableList<QuickOrderTextItem>
        items?.let {
            it[position] = updatedItem
            setQuickOrderData(it)
            this.notifyItemChanged(position)
        }
        items = null
    }

    fun setQuickOrderData(newList: MutableList<QuickOrderTextItem>) {
        val diffUtil = QuickOrderTextItemDiffUtil(quickOrderTextItems, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        quickOrderTextItems = newList as ArrayList<QuickOrderTextItem>
        diffResult.dispatchUpdatesTo(this)
    }

    class QuickOrderTextItemDiffUtil(
        private val oldList: List<QuickOrderTextItem>,
        private val newList: List<QuickOrderTextItem>
    ) : DiffUtil.Callback() {
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
                oldList[oldItemPosition].productName != newList[newItemPosition].productName -> false
                oldList[oldItemPosition].variantName != newList[newItemPosition].variantName -> false
                oldList[oldItemPosition].quantity != newList[newItemPosition].quantity -> false
                else -> true
            }
        }
    }
}

