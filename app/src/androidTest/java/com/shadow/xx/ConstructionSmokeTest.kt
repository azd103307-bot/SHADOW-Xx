package com.shadow.xx

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConstructionSmokeTest {
    @Test
    fun launcherShowsConstructionEntry() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("ابدأ البناء والتصميم")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun constructionWorkspaceOpens() {
        ActivityScenario.launch(ConstructionActivity::class.java).use {
            onView(withText("ورشة البناء ثلاثية الأبعاد")).check(matches(isDisplayed()))
        }
    }
}
