package com.bpitindia.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bpitindia.attendance.data.models.StudentsResponse

class EditAttendanceAdapter(
    internal val dataSet: StudentsResponse
) : RecyclerView.Adapter<EditAttendanceAdapter.MyViewHolder>() {

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRollNumber: TextView
        val tvName: TextView
        val checkBox: CheckBox

        init {
            tvRollNumber = view.findViewById(R.id.edit_roll_no)
            tvName = view.findViewById(R.id.edit_name)
            checkBox = view.findViewById(R.id.edit_check_box)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.edit_attendance_student_item, parent, false)
        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val studentInfo = dataSet[position]
        holder.tvRollNumber.text = studentInfo.classRollNumber
        holder.tvName.text = studentInfo.name
        holder.checkBox.isChecked = studentInfo.status == true
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            studentInfo.status = isChecked
        }
        holder.setIsRecyclable(false)
    }

    override fun getItemCount(): Int = dataSet.size
}