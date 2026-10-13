// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.hbmu

import kotlin.test.*

class HbmuProfileParserTest {
    @Test fun readsStudentNameWithReorderedAttributes() {
        assertEquals("张同学", HbmuProfileParser.nameFromHtml("<input value='张同学' type='hidden' id='xsxm' name='xsxm' />"))
    }
    @Test fun missingNameStaysEmpty() { assertEquals("", HbmuProfileParser.nameFromHtml("<input name='xsdm' value='0001'>")) }
}
