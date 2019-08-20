package com.hansoolabs.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse.*
import com.hansoolabs.and.utils.HLog
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * Created by brownsoo on 2018-09-08.
 * refer: https://github.com/googlesamples/android-play-billing/blob/master/TrivialDrive_v2/shared-module/src/main/java/com/example/billingmodule/billing/BillingManager.java
 */

@Suppress("MemberVisibilityCanBePrivate")
class BillingManager(
    private val activity: Activity,
    private val updatesListener: BillingUpdatesListener,
    var connectedListener: ServiceConnectedListener? = null): PurchasesUpdatedListener {
    
    companion object {
        private const val TAG = "BillingManager"
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

        fun errorMessage(@BillingClient.BillingResponse code: Int): String {
            when(code) {
                FEATURE_NOT_SUPPORTED ->
                    return "Requested feature is not supported by Play Store on the current device.[-2]"
                SERVICE_DISCONNECTED ->
                    return "Play Store service is not connected now[-1]"
                OK ->
                    return "Success"
                USER_CANCELED ->
                    return "User pressed back or canceled a dialog[1]"
                SERVICE_UNAVAILABLE ->
                    return "Network connection is down[2]"
                BILLING_UNAVAILABLE ->
                    return "Billing API version is not supported for the type requested[3]"
                ITEM_UNAVAILABLE ->
                    return "Requested product is not available for purchase[4]"
                DEVELOPER_ERROR ->
                    return "Invalid arguments provided to the API.[5]"
                ERROR ->
                    return "Fatal error during the API action[6]"
                ITEM_ALREADY_OWNED ->
                    return "Failure to purchase since item is already owned[7]"
                ITEM_NOT_OWNED ->
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
        fun onBillingConsumeFinished(token: String, @BillingClient.BillingResponse result: Int)
        fun onBillingPurchasesUpdated(purchases: List<Purchase>)
        // Considers this is new purchasing if the differ from purchased time to now is within 4 secs.
        fun onBillingPurchasesCreated(purchases: List<Purchase>)
        fun onBillingError(@BillingClient.BillingResponse response: Int)
    }
    
    /**
     * Listener for the Billing client state to become connected
     */
    interface ServiceConnectedListener {
        fun onServiceConnected(@BillingClient.BillingResponse resultCode: Int)
    }
    private val klass = "BillingManager@${Integer.toHexString(hashCode())}"
    private var billingClient: BillingClient? = null
    private var isServiceConnected = false
    private val mPurchases = ArrayList<Purchase>()
    private var tokensToBeConsumed: MutableSet<String>? = null
    /**
     * Returns the value BillingController client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * clien connection response was not received yet.
     */
    @BillingClient.BillingResponse
    var billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED
        private set
    
    val context: Context = activity
    
    // Start setup. This is asynchronous and the specified listener will be called
    // once setup completes.
    // It also starts to report all the new purchases through onBillingPurchasesUpdated() callback
    fun init() {
        billingClient = BillingClient.newBuilder(activity).setListener(this).build()
        HLog.d(TAG, klass, "starting ")
        startServiceConnection(Runnable {
            updatesListener.onBillingClientSetupFinished()
            HLog.d(TAG, klass, "setup successful.")
            queryPurchases()
        })
    }
    
    fun destroy() {
        HLog.d(TAG, klass, "destroying the manager")
        if (billingClient?.isReady == true) {
            billingClient?.endConnection()
            billingClient = null
        }
    }
    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    override fun onPurchasesUpdated(@BillingClient.BillingResponse responseCode: Int,
                                    purchases: MutableList<Purchase>?) {
        if (responseCode == OK) {
            if (purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
                updatesListener.onBillingPurchasesUpdated(this.mPurchases)
                val now = Date().time
                val newPurchases = ArrayList<Purchase>()
                for (p in this.mPurchases) {
                    if (abs(now - p.purchaseTime) < 4000) {
                        // Considers this is new purchasing if the differ from purchased time to now is within 4 secs.
                        newPurchases.add(p)
                        HLog.d(TAG, klass, "새로 구매 ${p.orderId}")
                    }
                }
                if (newPurchases.isNotEmpty()) {
                    updatesListener.onBillingPurchasesCreated(newPurchases)
                }
            }
        } else if (responseCode == USER_CANCELED) {
            HLog.i(TAG, klass, "user cancelled")
        } else {
            HLog.w(TAG, klass, "onBillingPurchasesUpdated error : ${errorMessage(responseCode)}")
            if (responseCode == ITEM_ALREADY_OWNED && !handledAlreadyOwned) {
                handledAlreadyOwned = true
                queryPurchases()
            }
            updatesListener.onBillingError(responseCode)
        }
    }

    private var handledAlreadyOwned = false

    /**
     * 구매 과정 시작
     */
    fun initiatePurchaseFlow(skuDetails: SkuDetails, oldSku: String?) {
        if (billingClient == null) return
        val purchaseFlowRequest = Runnable {
            HLog.d(TAG, klass, "Launching in-app purchase flow. Replace old SKU? ${oldSku != null}")
            val builder = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
            if (oldSku != null) {
                builder.setOldSku(oldSku)
            }
            billingClient?.launchBillingFlow(activity, builder.build())
        }
        executeServiceRequest(purchaseFlowRequest)
    }
    
    fun querySkuDetailsAsync(@BillingClient.SkuType itemType: String,
                             skuList: List<String>,
                             listener: SkuDetailsResponseListener) {
        val queryRequest = Runnable {
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(itemType)
            billingClient?.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
                billingClientResponseCode = responseCode
                listener.onSkuDetailsResponse(responseCode, skuDetailsList)
            }
        }
        executeServiceRequest(queryRequest)
    }
    
    @Suppress("unused")
    fun consumeAsync(purchaseToken: String) {
        // If we've already scheduled to consume this token - no action is needed (this could happen
        // if you received the token when querying purchases inside onReceive() and later from
        // onActivityResult()
        if (tokensToBeConsumed == null) {
            tokensToBeConsumed = HashSet()
        } else if (tokensToBeConsumed!!.contains(purchaseToken)) {
            HLog.i(TAG, klass, "Token was already scheduled to be consumed = skipping...")
            return
        }
        tokensToBeConsumed!!.add(purchaseToken)
        // Generating Consume Response listener
        val onConsumeListener = ConsumeResponseListener { responseCode, token ->
            updatesListener.onBillingConsumeFinished(token, responseCode)
        }
        // Creating a runnable from the request to use it inside our connection retry policy below
        val consumeRequest = Runnable {
            billingClient?.consumeAsync(purchaseToken, onConsumeListener)
        }
        executeServiceRequest(consumeRequest)
    }
    
    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     * @param purchase Purchase to be handled
     */
    private fun handlePurchase(purchase: Purchase) {
        if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
            HLog.i(TAG, klass, "Got a purchase: $purchase; but signature is bad. Skipping..")
            return
        }
        mPurchases.add(purchase)
    }
    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private fun onQueryPurchasesFinished(result: Purchase.PurchasesResult) {
        if (billingClient == null || result.responseCode != OK) {
            // Have we been disposed of in the meantime? If so, or bad result code, then quit
            HLog.w(TAG, klass, "BillingController client was null or result code (${result.responseCode}) was bad")
            return
        }
        HLog.d(TAG, klass, "Query inventory was successful.")
        // Update the UI and purchases inventory with new list of purchases
        mPurchases.clear()
        onPurchasesUpdated(OK, result.purchasesList)
    }
    
    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    fun areSubscriptionsSupported(): Boolean {
        val responseCode = billingClient?.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (responseCode != OK) {
            HLog.w(TAG, klass, "areSubscriptionsSupported() got an error response: $responseCode")
        }
        return responseCode == OK
    }
    
    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    fun queryPurchases() {
        val queryToExecute = Runnable {
            val time = System.currentTimeMillis()
            val purchasesResult = billingClient?.queryPurchases(BillingClient.SkuType.INAPP)
            HLog.i(TAG, klass, "Querying purchases elapsed time: ${System.currentTimeMillis() - time} ms")
            // If there are subscriptions supported, we add subscription rows as well
            if (areSubscriptionsSupported()) {
                val subscriptionResult = billingClient?.queryPurchases(BillingClient.SkuType.SUBS)
                HLog.i(TAG, klass, "Querying purchases and subscriptions elapsed time: "
                    + (System.currentTimeMillis() - time) + "ms")
                HLog.i(TAG, klass,"Querying subscriptions result code: "
                    + subscriptionResult?.responseCode
                    + " res: " + subscriptionResult?.purchasesList?.size)
                if (subscriptionResult?.responseCode == OK) {
                    purchasesResult?.purchasesList?.addAll(subscriptionResult.purchasesList)
                } else {
                    HLog.e(TAG, klass, "Got an error response trying to query subscription purchases")
                }
            } else if (purchasesResult?.responseCode == OK) {
                HLog.i(TAG, klass, "Skipped subscription purchases query since they are not supported")
            } else {
                HLog.w(TAG, klass, "queryPurchases() got an error response code: ${purchasesResult?.responseCode}")
            }
            purchasesResult?.let {
                onQueryPurchasesFinished(it)
            }
        }
        executeServiceRequest(queryToExecute)
    }

    fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient?.startConnection(object : BillingClientStateListener{
            override fun onBillingServiceDisconnected() {
                isServiceConnected = false
            }
            
            override fun onBillingSetupFinished(@BillingClient.BillingResponse responseCode: Int) {
                HLog.d(TAG, klass, "Setup finished. Response code: $responseCode")
                billingClientResponseCode = responseCode
                if (responseCode == OK) {
                    isServiceConnected = true
                    executeOnSuccess?.run()
                }
                connectedListener?.onServiceConnected(responseCode)
            }
            
        })
    }
    
    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected) {
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
    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature)
        } catch (e: IOException) {
            HLog.e(TAG, klass, "Got an exception trying to validate a purchase: $e")
            false
        }
    }
}