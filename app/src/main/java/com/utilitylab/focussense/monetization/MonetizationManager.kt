package com.utilitylab.focussense.monetization

import android.app.Activity
import android.app.Application
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.PrivacyOptionsRequirementStatus
import com.google.android.ump.UserMessagingPlatform
import com.utilitylab.focussense.BuildConfig
import com.utilitylab.focussense.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MonetizationState(
    val premium: Boolean = false,
    val billingReady: Boolean = false,
    val productPrice: String? = null,
    val canRequestAds: Boolean = false,
    val adsEnabled: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val message: String? = null
)

class MonetizationManager(
    private val app: Application,
    private val preferences: UserPreferences
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(app)

    private val _state = MutableStateFlow(MonetizationState())
    val state: StateFlow<MonetizationState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null
    private var billingResolved = false
    private var adsInitialized = false

    private val billingClient: BillingClient =
        BillingClient.newBuilder(app)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

    init {
        scope.launch {
            preferences.state.collectLatest { prefs ->
                if (!billingResolved) {
                    updateState(premium = prefs.premiumCached)
                }
            }
        }
        connectBilling()
    }

    fun refreshConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refreshAdState()
                }
                refreshAdState()
            },
            {
                refreshAdState()
            }
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            refreshAdState()
        }
    }

    fun purchasePro(activity: Activity) {
        if (_state.value.premium) return
        queryProductDetails { details ->
            if (details == null) {
                updateState(message = "Prodotto Pro non disponibile su questo account Play.")
                return@queryProductDetails
            }

            val paramsBuilder =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)

            val offerToken = details.oneTimePurchaseOfferDetailsList
                ?.firstOrNull()
                ?.offerToken

            if (!offerToken.isNullOrBlank()) {
                paramsBuilder.setOfferToken(offerToken)
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                updateState(message = result.debugMessage.ifBlank {
                    "Impossibile aprire Google Play."
                })
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connectBilling()
            return
        }
        queryOwnedPurchases()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED ->
                updateState(message = null)
            else ->
                updateState(
                    message = billingResult.debugMessage.ifBlank {
                        "Acquisto non completato."
                    }
                )
        }
    }

    private fun connectBilling() {
        if (billingClient.isReady) {
            queryOwnedPurchases()
            queryProductDetails()
            return
        }

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    val ready =
                        result.responseCode == BillingClient.BillingResponseCode.OK
                    updateState(billingReady = ready)
                    if (ready) {
                        queryOwnedPurchases()
                        queryProductDetails()
                    }
                }

                override fun onBillingServiceDisconnected() = Unit
            }
        )
    }

    private fun queryOwnedPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                billingResolved = true
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val owned = purchases.firstOrNull { purchase ->
            BuildConfig.PRO_PRODUCT_ID in purchase.products &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (owned == null) {
            setPremium(false)
            return
        }

        setPremium(true)

        if (!owned.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(owned.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    updateState(
                        message = "Pro attivo; riconoscimento Play da riprovare."
                    )
                }
            }
        }
    }

    private fun queryProductDetails(
        onResult: ((ProductDetails?) -> Unit)? = null
    ) {
        if (!billingClient.isReady) {
            onResult?.invoke(null)
            return
        }

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BuildConfig.PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            val details =
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    detailsResult.productDetailsList.firstOrNull()
                } else {
                    null
                }

            productDetails = details
            val price = details
                ?.oneTimePurchaseOfferDetailsList
                ?.firstOrNull()
                ?.formattedPrice

            updateState(productPrice = price)
            onResult?.invoke(details)
        }
    }

    private fun setPremium(premium: Boolean) {
        updateState(premium = premium, message = null)
        scope.launch {
            preferences.setPremiumCached(premium)
        }
        refreshAdState()
    }

    private fun refreshAdState() {
        val canRequest = consentInformation.canRequestAds()
        val configured =
            BuildConfig.USE_TEST_ADS || BuildConfig.ADMOB_PRODUCTION_CONFIGURED
        val adsEnabled = !_state.value.premium && canRequest && configured

        if (adsEnabled && !adsInitialized) {
            adsInitialized = true
            MobileAds.initialize(app) {}
        }

        _state.value = _state.value.copy(
            canRequestAds = canRequest,
            adsEnabled = adsEnabled,
            privacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    PrivacyOptionsRequirementStatus.REQUIRED
        )
    }

    private fun updateState(
        premium: Boolean = _state.value.premium,
        billingReady: Boolean = _state.value.billingReady,
        productPrice: String? = _state.value.productPrice,
        canRequestAds: Boolean = _state.value.canRequestAds,
        adsEnabled: Boolean = _state.value.adsEnabled,
        privacyOptionsRequired: Boolean = _state.value.privacyOptionsRequired,
        message: String? = _state.value.message
    ) {
        _state.value = MonetizationState(
            premium = premium,
            billingReady = billingReady,
            productPrice = productPrice,
            canRequestAds = canRequestAds,
            adsEnabled = adsEnabled,
            privacyOptionsRequired = privacyOptionsRequired,
            message = message
        )
    }
}
