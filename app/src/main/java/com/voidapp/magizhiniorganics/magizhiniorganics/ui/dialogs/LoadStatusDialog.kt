package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogSuccessBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.setTextAnimation

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
        var statusContent: String? = null
        var statusText: MutableLiveData<String?> = MutableLiveData()
        private val _statusText: LiveData<String?> = statusText
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
        _statusText.observe(this) { content ->
            content?.let { it -> loadLottie(it) }
        }
    }

    private fun populateData(title: String, body: String, content: String) {
        loadLottie(content)
        binding.apply {
            tvTitle.text = title
            tvBody.text = body
        }
    }

    private fun loadLottie(content: String) {
//        binding.tvBody.text = statusContent
        statusContent?.let { binding.tvBody.setTextAnimation(it) }
        when(content) {
            "success" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.success)
                    playAnimation()
                }
                statusContent = null
                statusText.value = null
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
            "purchaseValidation" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.validating_purchase)
                    playAnimation()
                }
            }
            //subscription part
            "validating" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.validating_purchase)
                    playAnimation()
                }
            }
            "fail" -> {
                binding.ltAnimImg.apply {
                    setAnimation(R.raw.empty)
                    playAnimation()
                }
            }
        }
    }

    private fun updateBody(content: String) {
        when(content) {
            "success" -> {
                binding.tvBody.text = statusContent
                loadLottie("success")
            }
            "placingOrder" -> {
                binding.tvBody.text = statusContent
                loadLottie("placingOrder")
            }
            "orderPlaced" -> {
                binding.tvBody.text = statusContent
                loadLottie("success")
            }

            else -> binding.tvBody.text = content
        }
    }

    override fun onDetach() {
        binding.ltAnimImg.cancelAnimation()
        _statusText.removeObservers(this)
        _binding = null
        super.onDetach()
    }
}