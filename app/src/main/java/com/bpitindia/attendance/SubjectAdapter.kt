package com.bpitindia.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class SubjectAdapter(private val dataSet: JSONArray, private val token: String) :
    RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val subjectName: TextView
        val year: TextView
        val branchSec: TextView
        val theoryOrLab: TextView
        val group: TextView
        val takeAttendance: ImageButton
        val viewStats: ImageButton
        val editAttendance: ImageButton


        init {
            // Define click listener for the ViewHolder's View.
            subjectName = view.findViewById(R.id.subject_name)
            year = view.findViewById(R.id.year)
            branchSec = view.findViewById(R.id.branch_sec)
            theoryOrLab = view.findViewById(R.id.theory_lab)
            group = view.findViewById(R.id.group)
            takeAttendance = view.findViewById(R.id.attendance_button)
            viewStats = view.findViewById(R.id.stats_button)
            editAttendance = view.findViewById(R.id.edit_attendance_button)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val subLayout =
            LayoutInflater.from(parent.context).inflate(R.layout.subject_list_item, parent, false)
        return ViewHolder(subLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subInfo: JSONObject = dataSet.getJSONObject(position)
        holder.subjectName.text = subInfo.getString("subject_name")
        holder.year.text = findYear(subInfo.getInt("semester"))
        val branchSection =
            "${findBranch(subInfo.getString("branch_code"))}-${subInfo.getString("section")}"
        holder.branchSec.text = branchSection
        val isLab: String = if (subInfo.getBoolean("is_lab")) "Lab" else "Theory"
        holder.theoryOrLab.text = isLab
        val group = subInfo.getString("group")
        if (group != "null") holder.group.text = group else holder.group.visibility = View.INVISIBLE

        val bundle = Bundle()
        bundle.putString("batch", subInfo.getString("batch"))
        bundle.putString("section", subInfo.getString("section"))
        bundle.putString("branch", subInfo.getString("branch_code"))
        bundle.putBoolean("is_lab", subInfo.getBoolean("is_lab"))
        bundle.putString("group", subInfo.getString("group"))
        bundle.putString("subject", subInfo.getString("subject_code"))

//        val navController = holder.itemView.findNavController()

        holder.takeAttendance.setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_subjectListFragment_to_studentListFragment, bundle)
        }

        holder.viewStats.setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_subjectListFragment_to_statisticsFragment, bundle)
        }

        holder.editAttendance.setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_subjectListFragment_to_editAttendanceFragment, bundle)
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