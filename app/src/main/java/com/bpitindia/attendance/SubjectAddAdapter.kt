package com.bpitindia.attendance

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class SubjectAddAdapter(private val listener: (JSONObject) -> Unit) :
    RecyclerView.Adapter<SubjectAddAdapter.MyViewHolder>() {

    var dataSet = JSONArray()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val subjectCode: TextView = view.findViewById(R.id.subject_code_search)
        private val subjectName: TextView = view.findViewById(R.id.subject_name_search)

        init {
            itemView.setOnClickListener {
                listener.invoke(dataSet.getJSONObject(adapterPosition))
            }
        }

        fun bind(subjectData: JSONObject) {
            with(subjectData) {
                subjectCode.text = this.getString("subject_code")
                subjectName.text = this.getString("subject_name")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.add_subject_item, parent, false)
        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val subInfo: JSONObject = dataSet.getJSONObject(position)
        holder.bind(subInfo)
    }

    override fun getItemCount(): Int = dataSet.length()

}