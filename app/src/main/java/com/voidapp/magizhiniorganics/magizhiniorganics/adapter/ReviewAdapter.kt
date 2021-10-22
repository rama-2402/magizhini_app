package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlin.collections.ArrayList

class ReviewAdapter(
    val context: Context,
    var reviews: ArrayList<Review>,
    val viewModel: ViewModel
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
            timeStamp.text = Time().getCustomDate(dateLong = review.timeStamp)
//            Timer("SettingUp", false).schedule(1000) {
//            ratings.setRating(review.rating, false)
//            ratings.disallowSelection(true)
//            ratings.setFaceColor(SmileyRating.Type.GREAT, Color.BLACK)
//            ratings.setFaceBackgroundColor(SmileyRating.Type.GREAT, Color.YELLOW)
//            }
//                ratings.setFaceColor(SmileyRating.Type.GREAT, Color.BLUE)

            when (review.rating) {
                1 -> {
                    ratings.setImageDrawable(ContextCompat.getDrawable(ratings.context, R.drawable.sm_bad))
                    reviewText.text = "Bad"
                }
                2 -> {
                    ratings.setImageDrawable(ContextCompat.getDrawable(ratings.context, R.drawable.sm_ok))
                    reviewText.text = "Not Satisfied"
                }
                3 -> {
                    ratings.setImageDrawable(ContextCompat.getDrawable(ratings.context, R.drawable.sm_satisfied))
                    reviewText.text = "Satisfied"
                }
                4 -> {
                    ratings.setImageDrawable(ContextCompat.getDrawable(ratings.context, R.drawable.sm_great))
                    reviewText.text = "Great"
                }
                5 -> {
                    ratings.setImageDrawable(ContextCompat.getDrawable(ratings.context, R.drawable.sm_awesome))
                    reviewText.text = "Awesome"
                }
            }

            reviewContent.text = getReviewContent(review.review)

            reviewImage.setOnClickListener {
                when (viewModel) {
                    is ProductViewModel -> viewModel.previewImage(review.reviewImageUrl)
                    is SubscriptionProductViewModel -> viewModel.previewImage(review.reviewImageUrl)
                }
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
}