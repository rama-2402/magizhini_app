package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogSuccessBinding

class LoadStatusDialog: DialogFragment() {

    private var _binding: DialogSuccessBinding? = null
    private val binding get() = _binding!!

    companion object {

        fun newInstance(title: String, body: String, content: String): LoadStatusDialog {
            val args = Bundle()
            args.putString("title", title)
            args.putString("body", body)
            args.putString("content", content)
            val fragment = LoadStatusDialog()
            fragment.arguments = args
            return fragment
        }

        var statusText: MutableLiveData<String> = MutableLiveData()
        private val _statusText: LiveData<String> = statusText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.CustomAlertDialog)
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSuccessBinding.inflate(LayoutInflater.from(context))
        isCancelable = false

        val title = arguments?.getString("title") ?: ""
        val body = arguments?.getString("body") ?: ""
        val content = arguments?.getString("content") ?: ""

        populateData(title, body, content)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _statusText.observe(this) {
            updateBody(it)
        }
    }

    private fun populateData(title: String, body: String, content: String) {
        binding.apply {
            tvTitle.text = title
            tvBody.text = body
        }
        loadLottie(content)
    }

    private fun loadLottie(content: String) {
        when(content) {
            "success" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.success)
                    playAnimation()
                }
            }
            "upload" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.upload)
                    playAnimation()
                }
            }
            "transaction" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.piggy_bank)
                    playAnimation()
                }
            }
            "placingOrder" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.placing_order)
                    playAnimation()
                }
            }
        }
    }

    private fun updateBody(content: String) {
        when(content) {
            "success" -> loadLottie("success")
            "dismiss" -> dismissLoadStatusDialog()
            "placingOrder" -> {
                binding.tvBody.text = "Placing your Order..."
                loadLottie("placingOrder")
            }
            "orderPlaced" -> {
                binding.tvBody.text = "Order Placed Successfully...!"
                loadLottie("success")
            }
            else -> binding.tvBody.text = content
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //attach the item click listener if needed
    }

    override fun onDetach() {
        super.onDetach()
        binding.ltAnimImg.cancelAnimation()
        _statusText.removeObservers(this)
        _binding = null
    }

    private fun dismissLoadStatusDialog() {
        dismiss()
    }
}