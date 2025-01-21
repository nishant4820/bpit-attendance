package com.bpitindia.attendance

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bpitindia.attendance.data.models.LocalAttendanceRecords

class LocalSubjectsAdapter(
    private val dataSet: List<LocalAttendanceRecords>,
) : RecyclerView.Adapter<LocalSubjectsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val subjectName: TextView = view.findViewById(R.id.local_subject_name)
        val subjectDate: TextView = view.findViewById(R.id.local_subject_date)
        val batch: TextView = view.findViewById(R.id.local_batch)
        val takeAttendance: LinearLayout = view.findViewById(R.id.local_info_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.local_subject_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]

        // Safely handle null values in the subject and batch properties
        holder.subjectName.text = item.subject ?: "Unknown Subject"
        val batchText = "Batch: ${item.batch}"
        holder.batch.text = batchText
        holder.subjectDate.text = item.date ?: "Unknown Date"

        // Prepare bundle for navigation
        val bundle = Bundle().apply {
            putString("BATCH", item.batch)
            putString("NAME", item.subject)
            putString("DATE", item.date)
        }


        // Set onClickListener for attendance layout
        holder.takeAttendance.setOnClickListener {
            holder.itemView.findNavController()
                .navigate(R.id.action_localSubjectListFragment_to_localStudentListFragment, bundle)
        }
    }


    override fun getItemCount() = dataSet.size

    private fun findYear(semester: Int): String {
        return when (semester) {
            1, 2 -> "1st Year"
            3, 4 -> "2nd Year"
            5, 6 -> "3rd Year"
            7, 8 -> "4th Year"
            else -> "Invalid Year"
        }
    }
}


