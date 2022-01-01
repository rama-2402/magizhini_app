package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messages
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.ChatDiffUtils
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.ReviewDiffUtil
import kotlin.collections.ArrayList

class ReviewAdapter(
    val context: Context,
    var reviews: ArrayList<Review>,
    private val onItemClickListener: ReviewItemClickListener
): RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.ivProfilePic)
        val profileName: TextView = itemView.findViewById(R.id.tvProfileName)
        val timeStamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val ratings: ImageView = itemView.findViewById(R.id.ivReview)
        val reviewText: TextView = itemView.findViewById(R.id.tvReviewText)
        val reviewContent: TextView = itemView.findViewById(R.id.tvReview)
        val reviewImage: ShapeableImageView = itemView.findViewById(R.id.ivReviewImage)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReviewViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_review_items, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        with(holder) {
            GlideLoader().loadUserPicture(profilePic.context, review.userProfilePicUrl, profilePic)
            if (review.reviewImageUrl.isEmpty()) {
                reviewImage.visibility = View.GONE
            } else {
                reviewImage.visibility = View.VISIBLE
                GlideLoader().loadUserPicture(reviewImage.context, review.reviewImageUrl, reviewImage)
            }
            profileName.text = review.userName
            timeStamp.text = TimeUtil().getCustomDate(dateLong = review.timeStamp)

            when (review.rating) {
                1 -> reviewText.text = "1"
                2 -> reviewText.text = "2"
                3 -> reviewText.text = "3"
                4 -> reviewText.text = "4"
                5 -> reviewText.text = "5"
            }

            reviewContent.text = getReviewContent(review.review)

            reviewImage.setOnClickListener {
                onItemClickListener.previewImage(review.reviewImageUrl)
            }

            profilePic.setOnClickListener {
                onItemClickListener.previewImage(review.userProfilePicUrl)
            }
        }
    }

    private fun getReviewContent(review: String): String {
        return if (review.isEmpty()) {
            ""
        } else {
            review
        }
    }

    override fun getItemCount(): Int {
        return reviews.size
    }

    fun setData(newList: ArrayList<Review>) {
        val chatDiffUtil = ReviewDiffUtil(reviews, newList)
        val diffResult = DiffUtil.calculateDiff(chatDiffUtil)
        reviews = newList
        diffResult.dispatchUpdatesTo(this)
    }

    interface ReviewItemClickListener {
        fun previewImage(url: String)
    }
}