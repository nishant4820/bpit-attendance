package com.bpitindia.attendance

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class EditAttendanceAdapter : RecyclerView.Adapter<EditAttendanceAdapter.MyViewHolder>() {

    var dataSet = JSONArray()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val rollNumber: TextView = view.findViewById(R.id.edit_roll_no)
        val name: TextView = view.findViewById(R.id.edit_name)
        val checkBox: CheckBox = view.findViewById(R.id.edit_check_box)

        fun bind(studentData: JSONObject) {
            with(studentData) {
                rollNumber.text = this.getString("class_roll_number")
                name.text = this.getString("name").uppercase()
                checkBox.isChecked = this.getBoolean("status")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.edit_attendance_student_item, parent, false)
        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val subInfo: JSONObject = dataSet.getJSONObject(position)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            subInfo.put("status", isChecked)
            dataSet.put(position, subInfo)
        }
        holder.bind(subInfo)
    }

    override fun getItemCount(): Int = dataSet.length()
}