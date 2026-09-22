package com.joshfactory.qurantafseel.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joshfactory.qurantafseel.data.SurahEntry

class SurahListAdapter(
    private val surahs: List<SurahEntry>,
    private val onClick: (SurahEntry) -> Unit
) : RecyclerView.Adapter<SurahListAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    override fun getItemCount() = surahs.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = surahs[position]
        holder.text.text = "${s.surahNum}. ${s.surahName}"
        holder.text.textSize = 16f
        holder.text.setPadding(56, 40, 56, 40)
        holder.itemView.setOnClickListener { onClick(s) }
    }
}
