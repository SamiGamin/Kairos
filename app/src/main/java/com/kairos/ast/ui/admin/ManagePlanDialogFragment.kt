package com.kairos.ast.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kairos.ast.databinding.DialogManagePlanBinding
import com.kairos.ast.model.Usuario
import com.kairos.ast.servicios.utils.DateUtils
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ManagePlanDialogFragment : DialogFragment() {

    private lateinit var binding: DialogManagePlanBinding
    private var selectedExpiration: Instant? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogManagePlanBinding.inflate(LayoutInflater.from(context))

        val userEmail = requireArguments().getString(ARG_USER_EMAIL)
        val userId = requireArguments().getString(ARG_USER_ID)!!
        val userPlan = requireArguments().getString(ARG_USER_PLAN)
        val userExpirationMillis = requireArguments().getLong(ARG_USER_EXPIRATION)
        selectedExpiration = Instant.ofEpochMilli(userExpirationMillis)

        setupView(userEmail, userPlan)
        setupDatePicker()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar Cambios") { _, _ ->
                val selectedPlan = binding.autoCompletePlan.text.toString().lowercase()
                val motivo = binding.etMotivo.text.toString()

                val result = bundleOf(
                    "newPlan" to selectedPlan,
                    "newExpiration" to selectedExpiration?.toEpochMilli(),
                    "reason" to motivo,
                    "userId" to userId
                )
                setFragmentResult(REQUEST_KEY, result)
                dismiss()
            }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    private fun setupView(email: String?, plan: String?) {
        binding.tvDialogUserEmail.text = email ?: "Email no disponible"
        binding.etFechaExpiracion.setText(DateUtils.formatReadableDateTime(selectedExpiration))

        val planOptions = arrayOf("Gratuito", "Mensual", "Anual")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, planOptions)
        binding.autoCompletePlan.setAdapter(adapter)
        binding.autoCompletePlan.setText(plan?.replaceFirstChar { it.uppercase() } ?: "Gratuito", false)
    }

    private fun setupDatePicker() {
        binding.etFechaExpiracion.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha de expiración")
                .setSelection(selectedExpiration?.toEpochMilli() ?: Instant.now().toEpochMilli())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedExpiration = Instant.ofEpochMilli(selection)
                val sdf = SimpleDateFormat("d MMM yyyy", Locale("es", "ES"))
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                binding.etFechaExpiracion.setText(sdf.format(Date(selection)))
            }

            datePicker.show(childFragmentManager, "DATE_PICKER")
        }
    }

    companion object {
        const val TAG = "ManagePlanDialog"
        const val REQUEST_KEY = "MANAGE_PLAN_REQUEST"

        private const val ARG_USER_ID = "USER_ID"
        private const val ARG_USER_EMAIL = "USER_EMAIL"
        private const val ARG_USER_PLAN = "USER_PLAN"
        private const val ARG_USER_EXPIRATION = "USER_EXPIRATION"

        fun newInstance(user: Usuario): ManagePlanDialogFragment {
            return ManagePlanDialogFragment().apply {
                arguments = bundleOf(
                    ARG_USER_ID to user.id,
                    ARG_USER_EMAIL to user.email,
                    ARG_USER_PLAN to user.tipo_plan,
                    ARG_USER_EXPIRATION to user.fecha_expiracion_plan.toEpochMilli()
                )
            }
        }
    }
}