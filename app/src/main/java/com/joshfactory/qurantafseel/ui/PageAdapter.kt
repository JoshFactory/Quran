package com.joshfactory.qurantafseel.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.joshfactory.qurantafseel.R
import com.joshfactory.qurantafseel.data.PdfPageRenderer

class PageAdapter(
    private val totalPages: Int,
    private val renderer: PdfPageRenderer
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.pageImage)
        val progress: ProgressBar = view.findViewById(R.id.pageProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false)
        return PageViewHolder(view)
    }

    override fun getItemCount(): Int = totalPages

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.imageView.setImageDrawable(null)
        holder.progress.visibility = View.VISIBLE

        val widthPx = holder.itemView.resources.displayMetrics.widthPixels
        renderer.renderPageAsync(position, widthPx) { bmp ->
            if (holder.bindingAdapterPosition == position) {
                holder.progress.visibility = View.GONE
                holder.imageView.setImageBitmap(bmp)
            }
        }
    }
}
