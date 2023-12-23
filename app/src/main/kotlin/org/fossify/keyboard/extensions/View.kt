package org.fossify.keyboard.extensions

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.onScroll(scroll: (Int) -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            scroll(computeVerticalScrollOffset())
        }
    })
}
