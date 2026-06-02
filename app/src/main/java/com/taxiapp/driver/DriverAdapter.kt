package com.taxiapp.driver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DriverAdapter(
    private val drivers: MutableList<Driver>,
    private val onEditClick: (Driver) -> Unit,
    private val onToggleSuspendClick: (Driver) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    inner class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtDriverName: TextView = itemView.findViewById(R.id.txtDriverName)
        val txtDriverVehicle: TextView = itemView.findViewById(R.id.txtDriverVehicle)
        val txtDriverLicensePlate: TextView = itemView.findViewById(R.id.txtDriverLicensePlate)
        val btnEditDriver: Button = itemView.findViewById(R.id.btnEditDriver)
        val btnToggleSuspend: Button = itemView.findViewById(R.id.btnToggleSuspend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_driver, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        val driver = drivers[position]
        holder.txtDriverName.text = driver.name
        holder.txtDriverVehicle.text = "Vehículo: ${driver.vehicle}"
        holder.txtDriverLicensePlate.text = "Placa: ${driver.licensePlate}"
        
        holder.btnToggleSuspend.text = if (driver.suspended) "Activar" else "Suspender"
        holder.btnToggleSuspend.setBackgroundTintList(
            if (driver.suspended) 
                android.content.res.ColorStateList.valueOf(0xFF10b981.toInt()) 
            else 
                android.content.res.ColorStateList.valueOf(0xFFef4444.toInt())
        )
        
        holder.btnEditDriver.setOnClickListener { onEditClick(driver) }
        holder.btnToggleSuspend.setOnClickListener { onToggleSuspendClick(driver) }
    }

    override fun getItemCount(): Int = drivers.size

    fun updateDrivers(newDrivers: List<Driver>) {
        drivers.clear()
        drivers.addAll(newDrivers)
        notifyDataSetChanged()
    }
}
