package com.mebudget.app.ui.subscription

import android.app.Activity
import co.paystack.android.Transaction
import com.mebudget.app.billing.BillingPlan
import com.mebudget.app.billing.PaystackManager
import io.mockk.coEvery
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
class SubscriptionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var paystackManager: PaystackManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        paystackManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads plans on init`() {
        coEvery { paystackManager.getAvailablePlans() } returns listOf(BillingPlan.Monthly, BillingPlan.Annual)

        val viewModel = SubscriptionViewModel(paystackManager)

        assertEquals(2, viewModel.uiState.value.plans.size)
    }

    @Test
    fun `selecting plan updates state`() {
        val viewModel = SubscriptionViewModel(paystackManager)

        viewModel.selectPlan(BillingPlan.Annual)

        assertEquals(BillingPlan.Annual, viewModel.uiState.value.selectedPlan)
    }

    @Test
    fun `subscribe without card is not submitted`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(paystackManager)
        viewModel.selectPlan(BillingPlan.Monthly)

        viewModel.subscribe(mockk<Activity>())
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertTrue(viewModel.uiState.value.error != null)
    }

    @Test
    fun `successful charge marks success`() = runTest(dispatcher) {
        coEvery { paystackManager.chargeCard(any(), any(), any(), any()) } returns Result.success(
            mockk<Transaction>()
        )
        val viewModel = SubscriptionViewModel(paystackManager)
        viewModel.selectPlan(BillingPlan.Monthly)
        viewModel.onEmailChanged("a@b.com")
        viewModel.onCardNumberChanged("4084084084084081")
        viewModel.onExpiryMonthChanged("01")
        viewModel.onExpiryYearChanged("30")
        viewModel.onCvvChanged("408")

        viewModel.subscribe(mockk<Activity>())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `failed charge surfaces error`() = runTest(dispatcher) {
        coEvery { paystackManager.chargeCard(any(), any(), any(), any()) } returns Result.failure(
            RuntimeException("Declined")
        )
        val viewModel = SubscriptionViewModel(paystackManager)
        viewModel.selectPlan(BillingPlan.Monthly)
        viewModel.onEmailChanged("a@b.com")
        viewModel.onCardNumberChanged("4084084084084081")
        viewModel.onExpiryMonthChanged("01")
        viewModel.onExpiryYearChanged("30")
        viewModel.onCvvChanged("408")

        viewModel.subscribe(mockk<Activity>())
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertEquals("Declined", viewModel.uiState.value.error)
    }
}