package com.xingheyuzhuan.shiguangschedule.hbmu

import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.repository.legacyDefaultTimeSlots
import com.xingheyuzhuan.shiguangschedule.data.repository.defaultTimeSlots
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport

object HbmuDefaultTimes {
    fun withCalibratedBaseline(model: CourseImportExport.CourseTableImportModel) = model.copy(
        timeSlots = defaultTimeSlots.map { CourseImportExport.TimeSlotJsonModel(it.number, it.startTime, it.endTime) },
        config = model.config?.copy(defaultClassDuration = 40, defaultBreakDuration = 10)
    )

    fun shouldReplaceLegacy(slots: List<TimeSlot>): Boolean =
        slots.size == legacyDefaultTimeSlots.size && slots.sortedBy { it.number }
            .zip(legacyDefaultTimeSlots).all { (current, old) ->
                current.number == old.number && current.startTime == old.startTime &&
                    current.endTime == old.endTime && current.alias == old.alias
            }
}
