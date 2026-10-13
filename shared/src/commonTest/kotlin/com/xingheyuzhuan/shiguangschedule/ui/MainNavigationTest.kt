// Modified by AconHol (2026): verify spatial ordering in both directions.
package com.xingheyuzhuan.shiguangschedule.ui
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.ui.components.mainTabDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class MainNavigationTest {
    @Test fun allMainTabPairsFollowTheirSpatialOrder() {
        val tabs = listOf(Destination.TodaySchedule, Destination.CourseSchedule, Destination.Hbmu)
        tabs.forEachIndexed { first, from -> tabs.forEachIndexed { second, to ->
            assertEquals((second - first).compareTo(0), mainTabDirection(from, to), "$from -> $to")
        } }
    }
    @Test fun detailScreensAreOutsideTheTabTransition() {
        assertEquals(0, mainTabDirection(Destination.Settings, Destination.Hbmu))
        assertEquals(0, mainTabDirection(Destination.TodaySchedule, Destination.ContributionList))
    }
}
