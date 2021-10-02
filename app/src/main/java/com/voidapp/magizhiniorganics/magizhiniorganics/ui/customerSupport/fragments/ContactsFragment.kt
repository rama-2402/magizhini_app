package com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ContactsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.SupportProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.FragmentContactsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class ContactsFragment : Fragment(), KodeinAware {

    override val kodein: Kodein by kodein()
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private val factory: ChatViewModelFactory by instance()
    private lateinit var chatViewModel: ChatViewModel

    private lateinit var mProgressDialog: Dialog
    private lateinit var adapter: ContactsAdapter
    private var mCurrentUserId: String = ""
    private var mSupportProfiles: ArrayList<SupportProfile> = arrayListOf()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        chatViewModel = ViewModelProvider(requireActivity(), factory).get(ChatViewModel::class.java)
        binding.viewmodel = chatViewModel

        mCurrentUserId = SharedPref(requireContext()).getData(Constants.USER_ID, Constants.STRING, "").toString()
        chatViewModel.getAllSupportProfiles()
        showProgressDialog()

        setRecyclerView()
        liveDataObservers()
        return binding.root
    }

    private fun liveDataObservers() {
        chatViewModel.supportProfiles.observe(viewLifecycleOwner, {
            adapter.supportProfiles = it
            adapter.notifyDataSetChanged()
        })
    }

    private fun setRecyclerView() {
        adapter = ContactsAdapter(
            requireContext(),
            mSupportProfiles,
            chatViewModel
        )
        binding.rvCustomerId.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomerId.adapter = adapter
        val divider = DividerItemDecoration(binding.rvCustomerId.context, LinearLayoutManager.VERTICAL)
        binding.rvCustomerId.addItemDecoration(divider)
        hideProgressDialog()
    }

    fun showProgressDialog() {

        mProgressDialog = Dialog(requireContext())

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog.setContentView(R.layout.dialog_loading)

//        val lottie = mProgressDialog.findViewById<LottieAnimationView>(R.id.lottie_progress)
//        lottie.animate()

        mProgressDialog.setCancelable(false)
        mProgressDialog.setCanceledOnTouchOutside(false)
        mProgressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        //Start the dialog and display it on screen.
        mProgressDialog.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    fun hideProgressDialog() {
//        val lottie = mProgressDialog.findViewById<LottieAnimationView>(R.id.lottie_progress)
//        lottie.cancelAnimation()
        mProgressDialog.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}