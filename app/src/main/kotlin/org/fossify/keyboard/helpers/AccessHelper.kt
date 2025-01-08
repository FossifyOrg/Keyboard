package org.fossify.keyboard.helpers

import android.graphics.Rect
import android.os.Bundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import org.fossify.keyboard.views.MyKeyboardView

class AccessHelper(
    private val keyboardView: MyKeyboardView,
    private val keys: List<MyKeyboard.Key>
) : ExploreByTouchHelper(keyboardView) {

    /**
     * We need to populate the list with the IDs of all of the visible virtual views (the intervals in the chart).
     * In our case, all keys are always visible, so we’ll return a list of all IDs.
     */
    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        val keysSize = keys.size
        for (i in 0 until keysSize) {
            virtualViewIds.add(i)
        }
    }

    /**
     * For this function, we need to return the ID of the virtual view that’s under the x, y position,
     * or ExploreByTouchHelper.HOST_ID if there’s no item at those coordinates.
     */
    override fun getVirtualViewAt(x: Float, y: Float): Int {
        val rects = keys.map {
            Rect(it.x, it.y, it.x + it.width, it.y + it.height)
        }

        return rects.firstOrNull { it.contains(x.toInt(), y.toInt()) }?.let { exactRect ->
            rects.indexOf(exactRect)
        } ?: HOST_ID
    }

    /**
     * This is where we provide all the metadata for our virtual view.
     * We need to set the content description (or text, if it’s presented visually) and set the bounds in parent.
     */
    override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        node.className = keyboardView::class.simpleName
        val key = keys.getOrNull(virtualViewId)
        node.contentDescription = key?.getContentDescription(keyboardView.context) ?: ""
        val bounds = updateBoundsForInterval(virtualViewId)
        node.setBoundsInScreen(bounds)
    }

    /**
     * We need to set the content description (or text, if it’s presented visually) and set the bounds in parent.
     * The bounds in the parent should match the logic in the onDraw() function.
     */
    private fun updateBoundsForInterval(index: Int): Rect {
        val keys = keys
        val key = keys.getOrNull(index) ?: return Rect()
        return Rect().apply {
            left = key.x
            top = key.y
            right = key.x + key.width
            bottom = key.y + key.height
        }
    }

    override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        return false
    }
}
