package org.fossify.keyboard.extensions

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView

val View.safeContext: Context
    get() = context.safeStorageContext

fun RecyclerView.onScroll(scroll: (Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            scroll(computeVerticalScrollOffset())
        }
    })
}
