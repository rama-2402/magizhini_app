package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.BirthdayCard
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBirthdayCardBinding

class BirthdayCardDialog: DialogFragment() {

    private var _binding: DialogBirthdayCardBinding? = null
    private val binding get() = _binding!!

    companion object {
      fun newInstance(birthdayCard: BirthdayCard): BirthdayCardDialog {
          val args = Bundle()
          args.putParcelable("card", birthdayCard)
          val fragment = BirthdayCardDialog()
          fragment.arguments = args
          return fragment
      }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.CustomAlertDialog)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBirthdayCardBinding.inflate(LayoutInflater.from(context))
        isCancelable = false

        val card: BirthdayCard? = arguments?.getParcelable("card")

        populateCardData(card!!)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvAccept.setOnClickListener {
            this.dismiss()
        }
    }

    private fun populateCardData(card: BirthdayCard) {
        binding.apply {
            tvName.text = card.customerName
            tvTitle.text = card.title
            tvContent.text = card.message
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        binding.ltAnimImg.cancelAnimation()
        _binding = null
        super.onDismiss(dialog)
    }

}