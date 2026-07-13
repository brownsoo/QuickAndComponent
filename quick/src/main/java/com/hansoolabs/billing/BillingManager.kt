package com.hansoolabs.billing

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.*
import com.hansoolabs.and.utils.HLog
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.collections.HashSet
import kotlin.coroutines.resume

@Suppress("MemberVisibilityCanBePrivate", "unused")
class BillingManager(
    private val verification: BillingVerification
) : PurchasesUpdatedListener {

    companion object {

        @Volatile
        private var INSTANCE: BillingManager? = null
        
        @Deprecated("use single instance for app", level = DeprecationLevel.ERROR)
        fun getInstance(application: Application, verification: BillingVerification): BillingManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(verification).also {
                    INSTANCE = it
                }
            }

        private const val TAG = "quick"
        const val BILLING_MANAGER_NOT_INITIALIZED = -1

        // 검증 콜백이 유실/지연될 때 배치가 무한 대기하지 않도록 하는 상한
        private const val VERIFY_TIMEOUT_MS = 10_000L

        fun errorMessage(@BillingClient.BillingResponseCode code: Int): String {
            when (code) {
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                    return "Requested feature is not supported by Play Store on the current device.[-2]"
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ->
                    return "Play Store service is not connected now[-1]"
                BillingClient.BillingResponseCode.OK ->
                    return "Success"
                BillingClient.BillingResponseCode.USER_CANCELED ->
                    return "User pressed back or canceled a dialog[1]"
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                    return "Network connection is down[2]"
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                    return "Billing API version is not supported for the type requested[3]"
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                    return "Requested product is not available for purchase[4]"
                BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                    return "Invalid arguments provided to the API.[5]"
                BillingClient.BillingResponseCode.ERROR ->
                    return "Fatal error during the API action[6]"
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                    return "Failure to purchase since item is already owned[7]"
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED ->
                    return "Failure to consume since item is not owned[8]"
                else ->
                    return "Unknown code [$code]"
            }
        }
    }
    
    interface BillingVerification {
        fun verifyValidSignature(purchase: Purchase, result: BillingVerificationResult)
    }

    interface BillingVerificationResult {
        fun onVerificationResult(purchase: Purchase, verified: Boolean)
    }

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onBillingConsumeFinished(
            token: String,
            @BillingClient.BillingResponseCode response: Int
        )

        fun onBillingPurchasesUpdated(purchases: List<Purchase>)
        fun onBillingError(@BillingClient.BillingResponseCode response: Int)

        /**
         * New Purchase
         */
        fun onBillingPurchasesCreated(purchases: List<Purchase>)
    }

    data class QueryProduct(
        val productType: String,
        val productId: String
    )
    
    private lateinit var billingClient: BillingClient

    private val klass = "BillingManager@${Integer.toHexString(this.hashCode())}"

    /**
     * Returns the value BillingController client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * client connection response was not received yet.
     */
    @BillingClient.BillingResponseCode
    var billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED
        private set

    private val updatesListeners = HashSet<BillingUpdatesListener>()
    fun addUpdateListener(listener: BillingUpdatesListener): Boolean {
        if (!updatesListeners.contains(listener)) {
            return updatesListeners.add(listener)
        }
        return false
    }

    fun removeUpdateListener(listener: BillingUpdatesListener): Boolean {
        return updatesListeners.remove(listener)
    }

    fun removeAllUpdateListeners() {
        updatesListeners.clear()
    }

    private val consumableSkus = HashSet<String>()
    private val nonConsumableSkus = HashSet<String>()

    private lateinit var mainHandler: Handler

    // Start setup. This is asynchronous and the specified listener will be called
    // once setup completes.
    // It also starts to report all the new purchases through onBillingPurchasesUpdated() callback
    @MainThread
    fun startConnection(
        application: Application,
        consumableSkus: Set<String>,
        nonConsumableSkus: Set<String>
    ) {
        this.mainHandler = Handler(Looper.getMainLooper())
        this.consumableSkus.clear()
        this.consumableSkus.addAll(consumableSkus)
        this.nonConsumableSkus.clear()
        this.nonConsumableSkus.addAll(nonConsumableSkus)
        val params = PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        billingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases(params)
            .setListener(this).build()
        HLog.d(TAG, klass, "starting ")

        startServiceConnection {
            HLog.d(TAG, klass, "setup successful.")
            mainHandler.post {
                updatesListeners.forEach { li -> li.onBillingClientSetupFinished() }
            }
            queryPurchases()
        }
    }

    fun endConnection() {
        removeAllUpdateListeners()
        billingClient.endConnection()
        HLog.d(TAG, klass, "end billing manager")
    }

    fun setConsumableSkus(skus: Set<String>) {
        this.consumableSkus.clear()
        this.consumableSkus.addAll(skus)
    }

    fun setNonConsumableSkus(skus: Set<String>) {
        this.nonConsumableSkus.clear()
        this.nonConsumableSkus.addAll(skus)
    }

    /**
     * This method is called by the [billingClient] when new purchases are detected.
     * The purchase list in this method is not the same as the one in
     * [queryPurchases][BillingClient.queryPurchasesAsync]. Whereas queryPurchases returns everything
     * this user owns, [onPurchasesUpdated] only returns the items that were just now purchased or
     * billed.
     *
     * The purchases provided here should be passed along to the secure server for
     * [verification](https://developer.android.com/google/play/billing/billing_library_overview#Verify)
     * and safekeeping. And if this purchase is consumable, it should be consumed, and the secure
     * server should be told of the consumption. All that is accomplished by calling
     * [queryPurchases].
     */
    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // will handle server verification, consumables, and updating the local cache
                purchases?.apply { processPurchases(this.toSet(), null) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                queryPurchases()
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                startServiceConnection {
                    HLog.d(TAG, klass, "re-start connection successful.")
                    mainHandler.post {
                        updatesListeners.forEach { li -> li.onBillingClientSetupFinished() }
                    }
                    queryPurchases()
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                HLog.i(TAG, klass, "user cancelled")
            }
            else -> {
                mainHandler.post {
                    updatesListeners.forEach { li -> li.onBillingError(result.responseCode) }
                }
            }
        }
    }

    private fun processPurchases(
        purchasesResult: Set<Purchase>,
        queryCallback: ((Set<Purchase>) -> Unit)?
    ) {

        if (purchasesResult.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                queryCallback?.invoke(emptySet())
            }
            return
        }

        CoroutineScope(Job() + Dispatchers.IO).launch {

            val purchased = purchasesResult.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            purchasesResult
                .filter { it.purchaseState == Purchase.PurchaseState.PENDING }
                .forEach {
                    // handle pending purchases, e.g. confirm with users about the pending
                    // purchases, prompt them to complete it, etc.
                    HLog.w(TAG, klass, "PENDING ${it.products}")
                }

            // 1) Google 가 PURCHASED 로 확정한 구매는 자체 서명검증과 분리하여 즉시 승인/소비한다.
            //    승인이 누락되면 3일 후 자동 환불되므로, 불안정할 수 있는 자체 검증(원격 키 조회 등)에
            //    승인 흐름을 종속시키지 않는다. acknowledge/consume 는 모두 멱등(idempotent)이다.
            val (consumables, nonConsumables) = purchased.partition { p ->
                consumableSkus.any { p.products.contains(it) }
            }
            HLog.d(TAG, klass, "processPurchases consumables content $consumables")
            HLog.d(TAG, klass, "processPurchases non-consumables content $nonConsumables")
            withContext(Dispatchers.Main) {
                handleConsumablePurchasesAsync(consumables)
                acknowledgeNonConsumablePurchasesAsync(nonConsumables)
            }

            // 2) 자체 서명검증은 '권한 부여(entitlement)' 판단에만 사용한다. 승인 흐름은 막지 않는다.
            //    콜백 기반 검증을 코루틴으로 순차 브릿지하여 카운터 레이스/콜백 유실을 제거한다.
            val validPurchases = LinkedHashSet<Purchase>(purchased.size)
            for (purchase in purchased) {
                if (verifySignatureSuspend(purchase)) {
                    validPurchases.add(purchase)
                } else {
                    HLog.w(TAG, klass, "NOT valid ${purchase.products}")
                }
            }
            HLog.d(TAG, klass, "verifyValidSignature complete valid=$validPurchases")

            withContext(Dispatchers.Main) {
                queryCallback?.invoke(validPurchases)
            }
        }
    }

    /**
     * 콜백 기반 [BillingVerification.verifyValidSignature] 를 코루틴으로 브릿지한다.
     * 검증 구현이 콜백을 누락하거나 지연시켜도 [VERIFY_TIMEOUT_MS] 후 false 로 degrade 되어
     * 배치 전체가 멈추지 않는다. (이 시점에는 이미 승인/소비가 완료된 상태다)
     */
    private suspend fun verifySignatureSuspend(purchase: Purchase): Boolean =
        withTimeoutOrNull(VERIFY_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                verification.verifyValidSignature(purchase, object : BillingVerificationResult {
                    override fun onVerificationResult(purchase: Purchase, verified: Boolean) {
                        if (cont.isActive) cont.resume(verified)
                    }
                })
            }
        } ?: run {
            HLog.w(TAG, klass, "verifyValidSignature timed out ${purchase.products}")
            false
        }


    private fun handleConsumablePurchasesAsync(consumables: List<Purchase>) {
        HLog.d(TAG, klass, "handleConsumablePurchasesAsync called")
        consumables.forEach {
            HLog.v(TAG, klass, "handleConsumablePurchasesAsync foreach it is $it")
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(it.purchaseToken)
                .build()
            billingClient.consumeAsync(params) { billingResult, purchaseToken ->
                mainHandler.post {
                    updatesListeners.forEach { li -> li.onBillingConsumeFinished(purchaseToken, billingResult.responseCode) }
                }
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // Update the appropriate tables/databases to grant user the items
                    }
                    else -> {
                        HLog.w(TAG, klass, billingResult.debugMessage)
                    }
                }
            }
        }
    }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchase] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: List<Purchase>) {
        nonConsumables.forEach { purchase ->
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { billingResult ->
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            mainHandler.post {
                                updatesListeners.forEach { li -> li.onBillingPurchasesCreated(listOf(purchase)) }
                            }
                        }
                        else -> {
                            HLog.w(TAG, klass,
                                "acknowledgeNonConsumablePurchasesAsync response is ${billingResult.debugMessage}"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 구매 과정 시작
     */
    fun launchPurchaseFlow(
        activity: Activity,
        products: List<ProductDetails>,
        oldPurchase: Purchase? = null
    ) {
        val purchaseFlowRequest = Runnable {
            HLog.d(TAG, klass, "Launching Flow, old SKU? ${oldPurchase != null}")
            val details = products.map {
                val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(it)
                it.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { token ->
                    builder.setOfferToken(token)
                }
                builder.build()
            }
            val builder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(details)
            if (oldPurchase != null) {
                builder.setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(oldPurchase.purchaseToken)
                        .build()
                )
            }
            billingClient.launchBillingFlow(activity, builder.build())
        }
        executeServiceRequest(purchaseFlowRequest)
    }
    
    @Deprecated("Use queryProductDetailsAsync",
        ReplaceWith("queryProductDetailsAsync(products, listener)"),
        DeprecationLevel.ERROR)
    fun querySkuDetailsAsync(
        @BillingClient.ProductType itemType: String,
        skuList: List<String>,
        listener: ProductDetailsResponseListener
    ){}
    /**
     * 상품 조회
     */
    fun queryProductDetailsAsync(
        products: List<QueryProduct>,
        listener: ProductDetailsResponseListener
    ) {
        val queryRequest = Runnable {
            val items = products.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(it.productType)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(items)
                .build()
            billingClient.queryProductDetailsAsync(params) { result, detailsList ->
                billingClientResponseCode = result.responseCode
                mainHandler.post {
                    listener.onProductDetailsResponse(result, detailsList)
                }
            }
        }
        executeServiceRequest(queryRequest)
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    fun isSubscriptionSupported(): Boolean {
        val result = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    fun queryPurchases() {
        executeServiceRequest {
            CoroutineScope(Job() + Dispatchers.Default).launch {
                val time = System.currentTimeMillis()
                val purchasesResult = HashSet<Purchase>()

                // 조회가 실패했는데 빈 목록을 그대로 전달하면, 소유 중인 구매가 '사라진 것'처럼 보여
                // 리스너가 권한(구독/광고제거)을 잘못 해제한다. 성공적으로 확인된 결과만 전달한다.
                val inAppResult = billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP).build())
                HLog.d(
                    TAG, klass,
                    "Querying purchases elapsed time: ${System.currentTimeMillis() - time} ms"
                )
                if (inAppResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    HLog.w(TAG, klass, "queryPurchases INAPP failed: ${inAppResult.billingResult.responseCode}; skip update")
                    return@launch
                }
                purchasesResult.addAll(inAppResult.purchasesList)

                // 구독 지원 여부가 확인되지 않으면(서비스 일시 단절 등) 구독 소유를 검증할 수 없으므로
                // 빈 결과로 구독을 지우지 않도록 이번 업데이트를 건너뛴다.
                if (!isSubscriptionSupported()) {
                    HLog.w(TAG, klass, "queryPurchases: subscriptions not currently checkable; skip update")
                    return@launch
                }
                val subsResult = billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS).build())
                HLog.d(
                    TAG, klass,
                    "Querying subscriptions elapsed time: " + (System.currentTimeMillis() - time) + "ms"
                )
                if (subsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    HLog.w(TAG, klass, "queryPurchases SUBS failed: ${subsResult.billingResult.responseCode}; skip update")
                    return@launch
                }
                purchasesResult.addAll(subsResult.purchasesList)

                processPurchases(purchasesResult) { valid ->
                    mainHandler.post {
                        updatesListeners.forEach { li -> li.onBillingPurchasesUpdated(valid.toList()) }
                    }
                }
            }
        }
    }

    fun startServiceConnection(executeOnSuccess: Runnable?) {
        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    HLog.d(TAG, klass, "onBillingServiceDisconnected")
                    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        startServiceConnection(executeOnSuccess)
                    }
                }

                override fun onBillingSetupFinished(result: BillingResult) {
                    val responseCode = result.responseCode
                    HLog.d(TAG, klass, "Setup finished. Response code: $responseCode")
                    billingClientResponseCode = responseCode
                    when (responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            executeOnSuccess?.run()
                        }
                        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                            mainHandler.post {
                                updatesListeners.forEach { li -> li.onBillingError(responseCode) }
                            }
                        }
                        else -> {
                            //do nothing. Someone else will connect it through retry policy.
                            //May choose to send to server though
                            HLog.d(TAG, klass, result.debugMessage)
                        }
                    }
                }
            })
        } else {
            executeOnSuccess?.run()
        }
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (billingClient.isReady) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
        }
    }
}