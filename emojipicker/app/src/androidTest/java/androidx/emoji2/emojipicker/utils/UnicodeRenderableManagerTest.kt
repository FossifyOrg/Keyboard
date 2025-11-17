

package com.rishabh.emojipicker.utils

import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class UnicodeRenderableManagerTest {
    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 23)
    fun testGetClosestRenderable_lowerVersionTrimmed() {
        // #️⃣
        assertEquals(
            UnicodeRenderableManager.getClosestRenderable("\u0023\uFE0F\u20E3"),
            "\u0023\u20E3"
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testGetClosestRenderable_higherVersionNoTrim() {
        // #️⃣
        assertEquals(
            UnicodeRenderableManager.getClosestRenderable("\u0023\uFE0F\u20E3"),
            "\u0023\uFE0F\u20E3"
        )
    }
}
