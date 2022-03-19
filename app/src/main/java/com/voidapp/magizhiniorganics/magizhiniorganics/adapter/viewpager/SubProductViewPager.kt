package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.fragments.SubDescriptionFragment
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.fragments.SubNewReviewFragment
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.fragments.SubReviewsFragment

class SubProductViewPager(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle){

    private val mFragmentList: ArrayList<Fragment> =
        arrayListOf(
            SubDescriptionFragment(),
            SubReviewsFragment(),
            SubNewReviewFragment()
        )

    override fun getItemCount(): Int {
        return mFragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragmentList[position]
    }
}