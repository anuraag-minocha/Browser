package com.kotlin.browser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kotlin.browser.application.findViewOften

typealias ItemClickListener = (view: View, position: Int) -> Unit


abstract class BaseAdapter<T>(var context: Context, var mList: ArrayList<T>) : RecyclerView.Adapter<BaseViewHolder>() {
    override fun getItemCount() = mList.size

    var mClickListener: ItemClickListener? = null
    fun setOnClickListener(todo: ItemClickListener) {
        mClickListener = todo
    }
}


open class BaseViewHolder(itemView: View, private val listener: ItemClickListener?)
    : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    override fun onClick(v: View) {
        listener?.invoke(v, adapterPosition)
    }

    init {
        itemView.setOnClickListener(this)
    }

    fun <T : View> findView(viewId: Int): T {
        return itemView.findViewOften(viewId)
    }
}


class RecordsAdapter(context: Context, mRecords: ArrayList<Record>) : BaseAdapter<Record>(context, mRecords) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
        return BaseViewHolder(view, mClickListener)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bean = mList[position]
        holder.findView<TextView>(R.id.title).text = bean.title
        holder.findView<TextView>(R.id.url).text = bean.url
    }

}