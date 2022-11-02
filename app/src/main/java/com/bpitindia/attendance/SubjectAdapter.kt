package com.bpitindia.attendance

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class SubjectAdapter(private val dataSet: JSONArray) :
    RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val subjectName: TextView = view.findViewById(R.id.subject_name)
        private val year: TextView = view.findViewById(R.id.year)
        private val branchSec: TextView = view.findViewById(R.id.branch_sec)
        private val theoryOrLab: TextView = view.findViewById(R.id.theory_lab)
        private val groupTextView: TextView = view.findViewById(R.id.group)
        val takeAttendance: LinearLayout = view.findViewById(R.id.infoLayout)
        val optionMenu: TextView = view.findViewById(R.id.textViewOptions)

        fun bind(subInfo: JSONObject) {
            subjectName.text = subInfo.getString("subject_name")
            year.text = findYear(subInfo.getInt("semester"))
            val branchSection =
                "${findBranch(subInfo.getString("branch_code"))}-${subInfo.getString("section")}"
            branchSec.text = branchSection
            val isLab: String = if (subInfo.getBoolean("is_lab")) "Lab" else "Theory"
            theoryOrLab.text = isLab
            val group = subInfo.getString("group")
            if (group != "null") groupTextView.text = group else groupTextView.visibility =
                View.INVISIBLE

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val subLayout =
            LayoutInflater.from(parent.context).inflate(R.layout.subject_list_item, parent, false)
        return ViewHolder(subLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subInfo: JSONObject = dataSet.getJSONObject(position)
        holder.bind(subInfo)

        val bundle = Bundle()
        bundle.putString("batch", subInfo.getString("batch"))
        bundle.putString("section", subInfo.getString("section"))
        bundle.putString("branch", subInfo.getString("branch_code"))
        bundle.putBoolean("is_lab", subInfo.getBoolean("is_lab"))
        bundle.putString("group", subInfo.getString("group"))
        bundle.putString("subject", subInfo.getString("subject_code"))

        holder.optionMenu.setOnClickListener {
            val popup = PopupMenu(it.context, holder.optionMenu)
            popup.inflate(R.menu.menu_subject_options)
            popup.setOnMenuItemClickListener { menuItem ->
                Log.e(">>", menuItem.toString())
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
        return dataSet.length()
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

    private fun findBranch(branch: String): String {
        val br: String = when (branch) {
            "027" -> "CSE"
            "031" -> "IT"
            "028" -> "ECE"
            "049" -> "EEE"
            "039" -> "MBA"
            "017" -> "BBA"
            else -> "Invalid Branch"
        }
        return br
    }

}