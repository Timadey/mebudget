package com.mebudget.app.ui.sync

import com.mebudget.app.data.sync.SyncDataCounts
import com.mebudget.app.data.sync.SyncEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MergeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var syncEngine: SyncEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        syncEngine = mockk(relaxed = true)
        coEvery { syncEngine.getLocalDataCounts() } returns SyncDataCounts(2, 3, 4)
        coEvery { syncEngine.getCloudDataCounts() } returns SyncDataCounts(1, 1, 1)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads local and cloud counts on init`() = runTest(dispatcher) {
        val viewModel = MergeViewModel(syncEngine)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SyncDataCounts(2, 3, 4), viewModel.uiState.value.localCounts)
        assertEquals(SyncDataCounts(1, 1, 1), viewModel.uiState.value.cloudCounts)
    }

    @Test
    fun `MERGE calls mergeLocalAndCloud and completes`() = runTest(dispatcher) {
        coEvery { syncEngine.mergeLocalAndCloud() } returns Result.success(Unit)
        val viewModel = MergeViewModel(syncEngine)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMergeOption(MergeOption.MERGE)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { syncEngine.mergeLocalAndCloud() }
        assertTrue(viewModel.uiState.value.isComplete)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `KEEP_LOCAL calls uploadLocalData`() = runTest(dispatcher) {
        coEvery { syncEngine.uploadLocalData() } returns Result.success(Unit)
        val viewModel = MergeViewModel(syncEngine)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMergeOption(MergeOption.KEEP_LOCAL)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { syncEngine.uploadLocalData() }
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `KEEP_CLOUD calls downloadCloudData`() = runTest(dispatcher) {
        coEvery { syncEngine.downloadCloudData() } returns Result.success(Unit)
        val viewModel = MergeViewModel(syncEngine)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMergeOption(MergeOption.KEEP_CLOUD)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { syncEngine.downloadCloudData() }
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `failure surfaces error and does not complete`() = runTest(dispatcher) {
        coEvery { syncEngine.mergeLocalAndCloud() } returns Result.failure(
            RuntimeException("offline")
        )
        val viewModel = MergeViewModel(syncEngine)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMergeOption(MergeOption.MERGE)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("offline", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isComplete)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}