package com.hansoolabs.billing

import android.app.Activity
import android.app.Application
import com.android.billingclient.api.*
import com.hansoolabs.and.utils.HLog
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Runnable
import kotlin.collections.HashSet

/**
 * Created by brownsoo on 2018-09-08.
 * refer: https://github.com/googlesamples/android-play-billing/blob/master/TrivialDrive_v2/shared-module/src/main/java/com/example/billingmodule/billing/BillingManager.java
 */

@Suppress("MemberVisibilityCanBePrivate")
class BillingManager private constructor(private val application: Application) :
    PurchasesUpdatedListener {

    companion object {

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(application: Application): BillingManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(application).also { INSTANCE = it }
            }

        private const val TAG = "quick"
        const val BILLING_MANAGER_NOT_INITIALIZED = -1

        /* BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        var BASE_64_ENCODED_PUBLIC_KEY = ""

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

    /**
     * The [BillingClient] is the most reliable and primary source of truth for all purchases
     * made through the Google Play Store. The Play Store takes security precautions in guarding
     * the data. Also, the data is available offline in most cases, which means the app incurs no
     * network charges for checking for purchases using the [BillingClient]. The offline bit is
     * because the Play Store caches every purchase the user owns, in an
     * [eventually consistent manner](https://developer.android.com/google/play/billing/billing_library_overview#Keep-up-to-date).
     * This is the only billing client an app is actually required to have on Android. The other
     * two (webServerBillingClient and localCacheBillingClient) are optional.
     *
     * ASIDE. Notice that the connection to [playStoreBillingClient] is created using the
     * applicationContext. This means the instance is not [Activity]-specific. And since it's also
     * not expensive, it can remain open for the life of the entire [Application]. So whether it is
     * (re)created for each [Activity] or [Fragment] or is kept open for the life of the application
     * is a matter of choice.
     */
    private lateinit var billingClient: BillingClient

    private val klass = "BillingManager@${Integer.toHexString(hashCode())}"

//    private val mPurchases = ArrayList<Purchase>()

//    private var tokensToBeConsumed: MutableSet<String>? = null

    /**
     * Returns the value BillingController client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * client connection response was not received yet.
     */
    @BillingClient.BillingResponseCode
    var billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED
        private set

    var updatesListener: BillingUpdatesListener? = null

    private var consumableSkus: Set<String> = emptySet()
    private var nonConsumableSkus: Set<String> = emptySet()

    // Start setup. This is asynchronous and the specified listener will be called
    // once setup completes.
    // It also starts to report all the new purchases through onBillingPurchasesUpdated() callback
    fun init(consumableSkus: Set<String>, nonConsumableSkus: Set<String>) {
        this.consumableSkus = consumableSkus
        this.nonConsumableSkus = nonConsumableSkus
        billingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases()
            .setListener(this).build()
        HLog.d(TAG, klass, "starting ")

        startServiceConnection(Runnable {
            HLog.d(TAG, klass, "setup successful.")
            updatesListener?.onBillingClientSetupFinished()
            queryPurchases()
        })
    }

    fun destroy() {
        billingClient.endConnection()
        HLog.d(TAG, klass, "end billing manager")
    }


    /**
     * This method is called by the [billingClient] when new purchases are detected.
     * The purchase list in this method is not the same as the one in
     * [queryPurchases][BillingClient.queryPurchases]. Whereas queryPurchases returns everything
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
                startServiceConnection(Runnable {
                    HLog.d(TAG, klass, "re-start connection successful.")
                    updatesListener?.onBillingClientSetupFinished()
                    queryPurchases()
                })
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                HLog.i(TAG, klass, "user cancelled")
            }
            else -> {
                updatesListener?.onBillingError(result.responseCode)
            }
        }

//        if (responseCode == BillingClient.BillingResponseCode.OK) {
//            if (purchases != null) {
//                val size = purchases.size
//                var handleCount = 0
//                for (purchase in purchases) {
//                    // 검사
//                    handlePurchase(purchase) {
//                        handleCount ++
//                        HLog.d(TAG, klass, "handlePurchase $handleCount / $size")
//                        if (handleCount == size) {
//                            // 최종
//                            updatesListener.onBillingPurchasesUpdated(this.mPurchases)
//                            // 새로 구매한 것 추측
//                            val now = Date().time
//                            val newPurchases = ArrayList<Purchase>()
//                            for (p in this.mPurchases) {
//                                if (abs(now - p.purchaseTime) < 4000) {
//                                    // Considers this is new purchasing if the differ from purchased time to now is within 4 secs.
//                                    newPurchases.add(p)
//                                    HLog.d(TAG, klass, "새로 구매 ${p.orderId}")
//                                }
//                            }
//                            if (newPurchases.isNotEmpty()) {
//                                updatesListener.onBillingPurchasesCreated(newPurchases)
//                            }
//                        }
//                    }
//                }
//            }
//        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
//            HLog.i(TAG, klass, "user cancelled")
//        } else {
//            HLog.w(TAG, klass, "onBillingPurchasesUpdated error : ${errorMessage(responseCode)}")
//            if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED && !handledAlreadyOwned) {
//                handledAlreadyOwned = true // 이미 소지한 것으로 보면, 한번 구매이력을 검색한다.
//                queryPurchases()
//            }
//            updatesListener.onBillingError(responseCode)
//        }
    }

//    private var handledAlreadyOwned = false

    private fun processPurchases(
        purchasesResult: Set<Purchase>,
        queryCallback: ((Set<Purchase>) -> Unit)?
    ) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            val validPurchases = HashSet<Purchase>(purchasesResult.size)
            HLog.d(TAG, klass, "processPurchases newBatch content $purchasesResult")
            purchasesResult.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (verifyValidSignature(purchase)) {
                        validPurchases.add(purchase)
                    }
                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    // handle pending purchases, e.g. confirm with users about the pending
                    // purchases, prompt them to complete it, etc.
                    HLog.w(TAG, klass, "PENDING ${purchase.sku}")
                }
            }

            queryCallback?.let { callback ->
                GlobalScope.launch(Dispatchers.Main) {
                    callback.invoke(validPurchases)
                }
            }

            val (consumables, nonConsumables) = validPurchases.partition {
                consumableSkus.contains(it.sku)
            }
            HLog.d(TAG, klass, "processPurchases consumables content $consumables")
            HLog.d(TAG, klass, "processPurchases non-consumables content $nonConsumables")
            /*
              As is being done in this sample, for extra reliability you may store the
              receipts/purchases to a your own remote/local database for until after you
              disburse entitlements. That way if the Google Play Billing library fails at any
              given point, you can independently verify whether entitlements were accurately
              disbursed. In this sample, the receipts are then removed upon entitlement
              disbursement.
             */
            GlobalScope.launch(Dispatchers.Main) {
                handleConsumablePurchasesAsync(consumables)
                acknowledgeNonConsumablePurchasesAsync(nonConsumables)
            }
        }


    private fun handleConsumablePurchasesAsync(consumables: List<Purchase>) {
        HLog.d(TAG, klass, "handleConsumablePurchasesAsync called")
        consumables.forEach {
            HLog.d(TAG, klass, "handleConsumablePurchasesAsync foreach it is $it")
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(it.purchaseToken)
                .build()
            billingClient.consumeAsync(params) { billingResult, purchaseToken ->
                updatesListener?.onBillingConsumeFinished(purchaseToken, billingResult.responseCode)
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
                            updatesListener?.onBillingPurchasesCreated(listOf(purchase))
                        }
                        else -> {
                            HLog.w(
                                TAG,
                                klass,
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
        skuDetails: SkuDetails,
        oldPurchase: Purchase? = null
    ) {
        val purchaseFlowRequest = Runnable {
            HLog.d(
                TAG,
                klass,
                "Launching in-app purchase flow. Replace old SKU? ${oldPurchase != null}"
            )
            val builder = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
            if (oldPurchase != null) {
                builder.setOldSku(oldPurchase.sku, oldPurchase.purchaseToken)
            }
            billingClient.launchBillingFlow(activity, builder.build())
        }
        executeServiceRequest(purchaseFlowRequest)
    }

    /**
     * 상품 조회
     */
    fun querySkuDetailsAsync(
        @BillingClient.SkuType itemType: String,
        skuList: List<String>,
        listener: SkuDetailsResponseListener
    ) {
        val queryRequest = Runnable {
            val params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(itemType)
                .build()
            billingClient.querySkuDetailsAsync(params) { result, skuDetailsList ->
                billingClientResponseCode = result.responseCode
                listener.onSkuDetailsResponse(result, skuDetailsList)
            }
        }
        executeServiceRequest(queryRequest)
    }

//    @Suppress("unused")
//    fun consumeAsync(purchaseToken: String) {
//        // If we've already scheduled to consume this token - no action is needed (this could happen
//        // if you received the token when querying purchases inside onReceive() and later from
//        // onActivityResult()
//        if (tokensToBeConsumed == null) {
//            tokensToBeConsumed = HashSet()
//        } else if (tokensToBeConsumed!!.contains(purchaseToken)) {
//            HLog.i(TAG, klass, "Token was already scheduled to be consumed = skipping...")
//            return
//        }
//        tokensToBeConsumed!!.add(purchaseToken)
//        // Generating Consume Response listener
//        val onConsumeListener = ConsumeResponseListener { result, token ->
//            updatesListener?.onBillingConsumeFinished(token, result.responseCode)
//        }
//        // Creating a runnable from the request to use it inside our connection retry policy below
//        val consumeParams = ConsumeParams.newBuilder()
//            .setPurchaseToken(purchaseToken)
//            .build()
//        val consumeRequest = Runnable {
//            billingClient.consumeAsync(consumeParams, onConsumeListener)
//        }
//        executeServiceRequest(consumeRequest)
//    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     * @param purchase Purchase to be handled
     */
//    private fun handlePurchase(purchase: Purchase, complete: (()-> Unit)?) {
//        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
//            if (!verifyValidSignature(purchase)) {
//                HLog.i(TAG, klass, "Got a purchase: $purchase; but signature is bad. Skipping..")
//                complete?.invoke()
//                return
//            }
//            // Acknowledge the purchase if it hasn't already been acknowledged.
//            if (purchase.isAcknowledged) {
//                mPurchases.add(purchase)
//                complete?.invoke()
//            } else {
//                HLog.w(TAG, klass, "acknowledgePurchase ${purchase.sku}")
//                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
//                    .setPurchaseToken(purchase.purchaseToken)
//                    .build()
//                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) {
//                    HLog.v(TAG, klass, it.debugMessage)
//                    if (it.responseCode == BillingClient.BillingResponseCode.OK || it.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
//                        mPurchases.add(purchase)
//                    } else {
//                        HLog.e(TAG, klass, it.debugMessage)
//                    }
//                    complete?.invoke()
//                }
//            }
//        } else {
//            complete?.invoke()
//        }
//    }
    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
//    private fun onQueryPurchasesFinished(result: Purchase.PurchasesResult) {
//        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
//            // Have we been disposed of in the meantime? If so, or bad result code, then quit
//            HLog.w(TAG, klass, "BillingController client was null or result code (${result.responseCode}) was bad")
//            return
//        }
//        HLog.d(TAG, klass, "Query inventory was successful.")
//        // Update the UI and purchases inventory with new list of purchases
//        mPurchases.clear()
//        onPurchasesUpdated(result.billingResult, result.purchasesList)
//    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
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
        executeServiceRequest(Runnable {
            val time = System.currentTimeMillis()
            val purchasesResult = HashSet<Purchase>()
            var result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
            HLog.i(
                TAG,
                klass,
                "Querying purchases elapsed time: ${System.currentTimeMillis() - time} ms"
            )
            result.purchasesList.let { purchasesResult.addAll(it) }

            if (isSubscriptionSupported()) {
                result = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                HLog.i(
                    TAG, klass, "Querying purchases and subscriptions elapsed time: "
                            + (System.currentTimeMillis() - time) + "ms"
                )
                result.purchasesList.let { purchasesResult.addAll(it) }
            }
            processPurchases(purchasesResult) {
                updatesListener?.onBillingPurchasesUpdated(it.toList())
            }
        })
    }

    fun startServiceConnection(executeOnSuccess: Runnable?) {
        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    HLog.d(TAG, klass, "onBillingServiceDisconnected")
                    startServiceConnection(executeOnSuccess)
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
                            updatesListener?.onBillingError(responseCode)
                        }
                        else -> {
                            //do nothing. Someone else will connect it through retry policy.
                            //May choose to send to server though
                            HLog.d(TAG, klass, result.debugMessage)
                        }
                    }
                    //connectedListener?.onServiceConnected(responseCode)
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

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private fun verifyValidSignature(purchase: Purchase): Boolean {
        return try {
            Security.verifyPurchase(
                BASE_64_ENCODED_PUBLIC_KEY,
                purchase.originalJson,
                purchase.signature
            )
        } catch (e: IOException) {
            HLog.e(TAG, klass, "Got an exception trying to validate a purchase: $e")
            false
        }
    }
}