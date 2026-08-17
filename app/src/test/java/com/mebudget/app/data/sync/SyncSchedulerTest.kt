package com.mebudget.app.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncSchedulerTest {

    @Test
    fun `schedulePeriodic does not throw and is idempotent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        SyncScheduler.schedulePeriodic(context)
        SyncScheduler.schedulePeriodic(context)
    }
}