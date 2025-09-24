package com.kairos.ast.ui.planes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kairos.ast.R
import com.kairos.ast.databinding.ItemFeatureBinding
import com.kairos.ast.databinding.ItemPlanBinding
import com.kairos.ast.ui.planes.model.Plan

class PlanesAdapter(
    private val onPlanSelected: (Plan) -> Unit
) : ListAdapter<Plan, PlanesAdapter.PlanViewHolder>(PlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = getItem(position)
        holder.bind(plan, onPlanSelected)
    }

    class PlanViewHolder(private val binding: ItemPlanBinding) : RecyclerView.ViewHolder(binding.root) {
        // Obtenemos el contexto una vez para reutilizarlo
        private val context: Context = binding.root.context

        fun bind(plan: Plan, onPlanSelected: (Plan) -> Unit) {
            // --- 1. Llenar datos básicos ---
            binding.planTitulo.text = plan.name
            binding.planPrecio.text = plan.price
            binding.planEtiqueta.isVisible = plan.isPopular

            // --- 2. Manejar el subtítulo de ahorro (el nuevo campo) ---
            if (plan.priceSubtitle != null) {
                binding.planSubtituloPrecio.text = plan.priceSubtitle
                binding.planSubtituloPrecio.isVisible = true

            } else {
                binding.planSubtituloPrecio.isVisible = false
            }

            // --- 3. Generar la lista de características dinámicamente ---
            // Limpiamos cualquier vista anterior (importante para el reciclaje del RecyclerView)
            binding.planCaracteristicas.removeAllViews()

            plan.features.forEach { featureText ->
                // Inflamos el layout de la característica (item_feature.xml)
                val featureBinding = ItemFeatureBinding.inflate(LayoutInflater.from(context))
                featureBinding.root.text = featureText // El root de item_feature.xml es un TextView

                // Añadimos la vista de la característica al contenedor LinearLayout
                binding.planCaracteristicas.addView(featureBinding.root)
            }

            // --- 4. Cambiar el estilo del botón y la tarjeta según si es popular ---
            if (plan.isPopular) {
                binding.planEtiqueta.text = "Más Popular" // <-- AÑADE ESTA LÍNEA
                binding.planEtiqueta.isVisible = true
                binding.btnSeleccionarPlan.styleAsFilledButton()
                binding.cardPlan.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_popular_stroke_width) // Ej: 2dp
                binding.cardPlan.strokeColor = ContextCompat.getColor(context, R.color.md_theme_primary)
            } else {
                // Estilo para los planes normales (botón delineado)
                binding.btnSeleccionarPlan.styleAsOutlinedButton()
                binding.cardPlan.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_normal_stroke_width) // Ej: 1dp
                binding.cardPlan.strokeColor = ContextCompat.getColor(context, R.color.md_theme_outline)
            }

            // --- 5. Configurar el listener del click ---
            binding.btnSeleccionarPlan.setOnClickListener {
                onPlanSelected(plan)
            }
        }

        // --- Funciones de extensión para limpiar el código de estilos ---
        private fun MaterialButton.styleAsFilledButton() {
            this.backgroundTintList = ContextCompat.getColorStateList(context, R.color.md_theme_surface)
            this.setTextColor(ContextCompat.getColor(context, R.color.md_theme_secondary))
            this.strokeColor = ContextCompat.getColorStateList(context, R.color.md_theme_outline)
            this.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.button_outline_stroke_width)
        }

        private fun MaterialButton.styleAsOutlinedButton() {
            this.backgroundTintList = ContextCompat.getColorStateList(context, R.color.md_theme_surface)
            this.setTextColor(ContextCompat.getColor(context, R.color.md_theme_primary))
            this.strokeColor = ContextCompat.getColorStateList(context, R.color.md_theme_outline)
            this.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.button_outline_stroke_width) // Ej: 1dp
        }
    }


    class PlanDiffCallback : DiffUtil.ItemCallback<Plan>() {
        override fun areItemsTheSame(oldItem: Plan, newItem: Plan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Plan, newItem: Plan): Boolean {
            return oldItem == newItem
        }
    }
}
