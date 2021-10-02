package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments.DescriptionFragment
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments.NewReviewFragment
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.fragments.ReviewsFragment

class ProductViewPager (
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle){

    private val mFragmentList: ArrayList<Fragment> =
        arrayListOf(
            DescriptionFragment(),
            ReviewsFragment(),
            NewReviewFragment()
        )
    private val mFragmentNamesList = ArrayList<String>()

    override fun getItemCount(): Int {
        return mFragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragmentList[position]
    }

    fun addFragment(fragment: Fragment) {
        mFragmentList.add(fragment)
    }
}