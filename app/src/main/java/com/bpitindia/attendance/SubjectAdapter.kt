package com.bpitindia.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bpitindia.attendance.data.models.FacultySubject
import com.bpitindia.attendance.data.models.FacultySubjectsResponse

class SubjectAdapter(private val dataSet: FacultySubjectsResponse) :
    RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val subjectName: TextView = view.findViewById(R.id.subject_name)
        private val year: TextView = view.findViewById(R.id.year)
        private val branchSec: TextView = view.findViewById(R.id.branch_sec)
        private val theoryOrLab: TextView = view.findViewById(R.id.theory_lab)
        private val groupTextView: TextView = view.findViewById(R.id.group)
        private val specializationTextView: TextView = view.findViewById(R.id.specialization)
        val takeAttendance: LinearLayout = view.findViewById(R.id.infoLayout)
        val optionMenu: TextView = view.findViewById(R.id.textViewOptions)

        fun bind(subInfo: FacultySubject) {
            subjectName.text = subInfo.subjectName
            year.text = findYear(subInfo.semester!!.toInt())
            val branchSection =
                "${subInfo.branchSlug}-${subInfo.section ?: subInfo.classBatch}"
            branchSec.text = branchSection
            val isLab: String = if (subInfo.isLab == true) "Lab" else "Theory"
            theoryOrLab.text = isLab
            val group = subInfo.group
            if (group == null || group == "null") groupTextView.visibility =
                View.GONE else groupTextView.text = group
            if (subInfo.specialization != 1) {
                specializationTextView.visibility = View.VISIBLE
                specializationTextView.text = subInfo.specializationName
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val subLayout =
            LayoutInflater.from(parent.context).inflate(R.layout.subject_list_item, parent, false)
        return ViewHolder(subLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subInfo: FacultySubject = dataSet[position]
        holder.bind(subInfo)

        val bundle = Bundle()
        bundle.putString("batch", subInfo.batch)
        bundle.putString("section", subInfo.section)
        bundle.putString("branch", subInfo.branchCode)
        bundle.putBoolean("is_lab", subInfo.isLab ?: false)
        bundle.putString("group", subInfo.group)
        bundle.putString("subject", subInfo.subjectCode)
        bundle.putInt("semester", subInfo.semester?.toInt() ?: 0)
        bundle.putString("class_batch", subInfo.classBatch)
        bundle.putInt("specialization", subInfo.specialization ?: 1)

        holder.optionMenu.setOnClickListener {
            val popup = PopupMenu(it.context, holder.optionMenu)
            popup.inflate(R.menu.menu_subject_options)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.stats_button -> {
                        holder.itemView.findNavController()
                            .navigate(R.id.action_subjectListFragment_to_statisticsFragment, bundle)
                        true
                    }

                    R.id.edit_attendance_button -> {
                        holder.itemView.findNavController()
                            .navigate(
                                R.id.action_subjectListFragment_to_editAttendanceFragment,
                                bundle
                            )
                        true
                    }

                    else -> false
                }

            }
            popup.show()
        }

        holder.takeAttendance.setOnClickListener {
            holder.itemView.findNavController()
                .navigate(R.id.action_subjectListFragment_to_studentListFragment, bundle)
        }

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    private fun findYear(semester: Int): String {
        val year: String = when (semester) {
            1, 2 -> "1st Year"
            3, 4 -> "2nd Year"
            5, 6 -> "3rd Year"
            7, 8 -> "4th Year"
            else -> "Invalid Year"
        }
        return year
    }

}