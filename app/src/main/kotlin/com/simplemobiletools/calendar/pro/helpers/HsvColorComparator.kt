package com.simplemobiletools.calendar.pro.helpers

import android.graphics.Color

/**
 * A color comparator which compares based on hue, saturation, and value.
 * Source: AOSP Color picker, https://cs.android.com/android/platform/superproject/+/master:frameworks/opt/colorpicker/src/com/android/colorpicker/HsvColorComparator.java
 */
class HsvColorComparator : Comparator<Int?> {
    override fun compare(lhs: Int?, rhs: Int?): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(lhs!!, hsv)
        val hue1 = hsv[0]
        val sat1 = hsv[1]
        val val1 = hsv[2]
        val hsv2 = FloatArray(3)
        Color.colorToHSV(rhs!!, hsv2)
        val hue2 = hsv2[0]
        val sat2 = hsv2[1]
        val val2 = hsv2[2]
        if (hue1 < hue2) {
            return 1
        } else if (hue1 > hue2) {
            return -1
        } else {
            if (sat1 < sat2) {
                return 1
            } else if (sat1 > sat2) {
                return -1
            } else {
                if (val1 < val2) {
                    return 1
                } else if (val1 > val2) {
                    return -1
                }
            }
        }
        return 0
    }
}
