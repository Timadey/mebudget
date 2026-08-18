package com.mebudget.app.ui.subscription

import com.google.gson.JsonObject
import com.mebudget.app.billing.BillingPlan
import com.mebudget.app.data.sync.PocketBaseApi
import com.mebudget.app.data.sync.PricingConfigManager
import com.mebudget.app.data.sync.models.CheckoutResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var pricing: PricingConfigManager
    private lateinit var api: PocketBaseApi
    private lateinit var client: com.mebudget.app.data.sync.PocketBaseClient

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        pricing = mockk(relaxed = true)
        every { pricing.plans } returns MutableStateFlow(BillingPlan.DEFAULTS)
        api = mockk(relaxed = true)
        client = mockk(relaxed = true)
        every { client.api } returns api
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads plans on init`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(pricing, client)

        assertEquals(2, viewModel.uiState.value.plans.size)
    }

    @Test
    fun `selecting plan updates state`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(pricing, client)

        viewModel.selectPlan(BillingPlan.ANNUAL)

        assertEquals(BillingPlan.ANNUAL, viewModel.uiState.value.selectedPlan)
    }

    @Test
    fun `startCheckout without selected plan is no-op`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(pricing, client)

        viewModel.startCheckout()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.checkoutUrl)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `startCheckout sets checkout url on success`() = runTest(dispatcher) {
        coEvery { api.createCheckout(any()) } returns CheckoutResponse(
            authorizationUrl = "https://checkout.paystack.com/abc",
            accessCode = "acc1",
            reference = "ref1"
        )
        val viewModel = SubscriptionViewModel(pricing, client)
        viewModel.selectPlan(BillingPlan.MONTHLY)

        viewModel.startCheckout()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://checkout.paystack.com/abc", viewModel.uiState.value.checkoutUrl)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `startCheckout surfaces failure`() = runTest(dispatcher) {
        coEvery { api.createCheckout(any()) } throws RuntimeException("offline")
        val viewModel = SubscriptionViewModel(pricing, client)
        viewModel.selectPlan(BillingPlan.MONTHLY)

        viewModel.startCheckout()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.checkoutUrl)
        assertEquals("offline", viewModel.uiState.value.error)
    }

    @Test
    fun `activate after payment flags isSuccess for subscription refresh`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(pricing, client)

        viewModel.onPaymentSucceeded()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(viewModel.uiState.value.checkoutUrl)
    }

    @Test
    fun `cancel clears checkout url`() = runTest(dispatcher) {
        val viewModel = SubscriptionViewModel(pricing, client)
        viewModel.selectPlan(BillingPlan.MONTHLY)
        viewModel.onPaymentCanceled()

        assertNull(viewModel.uiState.value.checkoutUrl)
    }
}