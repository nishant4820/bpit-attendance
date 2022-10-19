package com.bpitindia.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

val attendanceMap = mutableMapOf<String, Boolean>()

class StudentAdapter(
    private val dataSet: JSONArray
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val enrollmentNumber: TextView
        val name: TextView
        val classRollNumber: TextView
        val attendanceCount: TextView
        val studentCard: CardView

        init {
            enrollmentNumber = view.findViewById(R.id.enrollment_number)
            name = view.findViewById(R.id.name)
            classRollNumber = view.findViewById(R.id.class_roll_number)
            attendanceCount = view.findViewById(R.id.attendance_count)
            studentCard = view.findViewById(R.id.student_card)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.student_list_item, parent, false)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        val studentInfo: JSONObject = dataSet.getJSONObject(position)
        holder.enrollmentNumber.text = studentInfo.getString("enrollment_number")
        holder.name.text = studentInfo.getString("name")
        holder.classRollNumber.text = studentInfo.getString("class_roll_number")
        holder.attendanceCount.text = holder.itemView.context.getString(
            R.string.attendance_count,
            studentInfo.getInt("attendance_count")
        )
        val isPresent: Boolean =
            attendanceMap[studentInfo.getString("enrollment_number")]!!
        if (isPresent)
            holder.studentCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.present_color
                )
            )
        else
            holder.studentCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.absent_color
                )
            )
        holder.studentCard.setOnClickListener {
            val isPresent1: Boolean =
                attendanceMap[studentInfo.getString("enrollment_number")]!!
            attendanceMap.replace(
                studentInfo.getString("enrollment_number"),
                !isPresent1
            )
            if (isPresent1) {
                (it as CardView).setCardBackgroundColor(
                    ContextCompat.getColor(
                        it.context,
                        R.color.absent_color
                    )
                )
            } else {
                (it as CardView).setCardBackgroundColor(
                    ContextCompat.getColor(
                        it.context,
                        R.color.present_color
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.length()
    }
}