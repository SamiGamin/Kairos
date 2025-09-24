package com.kairos.ast.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kairos.ast.databinding.ItemUsuarioAdminBinding
import com.kairos.ast.model.Usuario
import com.kairos.ast.servicios.utils.DateUtils

class UserAdminAdapter(
    private val onManageClickListener: (Usuario) -> Unit
) : ListAdapter<Usuario, UserAdminAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsuarioAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val usuario = getItem(position)
        holder.bind(usuario, onManageClickListener)
    }

    class UserViewHolder(private val binding: ItemUsuarioAdminBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(usuario: Usuario, onManageClickListener: (Usuario) -> Unit) {
            binding.tvNombreUsuario.text = usuario.nombre ?: "Sin nombre"
            binding.tvEmailUsuario.text = usuario.email
            binding.tvTipoPlan.text = usuario.tipo_plan.replaceFirstChar { it.uppercase() }
            
            val fechaFormateada = DateUtils.formatReadableDateTime(usuario.fecha_expiracion_plan)
            binding.tvFechaExpiracion.text = fechaFormateada

            binding.btnGestionarPlan.setOnClickListener {
                onManageClickListener(usuario)
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<Usuario>() {
        override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario): Boolean {
            return oldItem == newItem
        }
    }
}
