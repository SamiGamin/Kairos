package com.kairos.ast.ui.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.kairos.ast.databinding.DialogPlanExpiradoBinding

class PlanExpiradoDialogFragment : DialogFragment() {

    interface PlanExpiradoDialogListener {
        fun onVerPlanesClicked()
    }

    private var _binding: DialogPlanExpiradoBinding? = null
    private val binding get() = _binding!!
    private var listener: PlanExpiradoDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as PlanExpiradoDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement PlanExpiradoDialogListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlanExpiradoBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnVerPlanes.setOnClickListener {
            listener?.onVerPlanesClicked()
            dismiss()
        }

        binding.btnAhoraNo.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
