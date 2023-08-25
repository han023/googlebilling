@file:Suppress("ClassName")

package com.hansoft.googlebilling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList


import java.util.concurrent.Executors

class billingactivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billingactivity)

        // initilize the billing client
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build();


        if (ConnectionClass.permium){
            Toast.makeText(this,"Already Subscribed",Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this,"Not Subscribed",Toast.LENGTH_LONG).show()
        }

    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null){
                purchases.forEach{ purchase ->
                    handlePurchase(purchase)
                }
            }else if(billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED){
                ConnectionClass.permium = true
                ConnectionClass.locked = false
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED){

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE){

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED){

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR){

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE){

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR){

            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED){

            } else {
                Toast.makeText(this,"Error "+billingResult.debugMessage,Toast.LENGTH_LONG).show()
            }
        }

    private fun handlePurchase(purchase: Purchase) {

        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build()

        val consumeResponseListener = ConsumeResponseListener{ billingResult, s ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK){

            }
        }

        billingClient.consumeAsync(consumeParams,consumeResponseListener)

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED){
            if (!verifyValidSignature(purchase.originalJson,purchase.signature)){
                Toast.makeText(this,"Error : Invalid Purchase",Toast.LENGTH_LONG).show()
                return
            }
            if (!purchase.isAcknowledged){
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams,acknowledgePurchaseResponseListener)
                Toast.makeText(this,"Subscribe",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"Already subscribe",Toast.LENGTH_LONG).show()
            }
            ConnectionClass.permium = true
            ConnectionClass.locked = false
        }else if (purchase.purchaseState == Purchase.PurchaseState.PENDING){
            Toast.makeText(this,"Pending",Toast.LENGTH_LONG).show()
        }

    }

    val acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult ->
        ConnectionClass.permium = true
        ConnectionClass.locked = false
    }

    private fun verifyValidSignature(signedData:String , signature:String): Boolean {
        return try {
            Security.verifyPurchase(signedData,signature);
        } catch (e:Exception){
            false;
        }
    }

    private fun getData(){
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    val executorservice = Executors.newSingleThreadExecutor()
                    executorservice.execute{

                        //Show products available to buy
                        val queryProductDetailsParams =
                            QueryProductDetailsParams.newBuilder()
                                .setProductList(
                                    ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId("product_id_example")
                                            .setProductType(BillingClient.ProductType.SUBS)  // ProductType.INAPP
                                            .build()))
                                .build()

                        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
                                billingResult,
                                productDetailsList ->
                            // check billingResult
                            // process returned productDetailsList

                            productDetailsList.forEach{ product ->
                                val offertoken = product.subscriptionOfferDetails?.get(0)?.offerToken.toString()
                                val productDetailsParamsList = listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                        .setProductDetails(product)
                                        // to get an offer token, call ProductDetails.subscriptionOfferDetails()
                                        // for a list of offers that are available to the user
                                        .setOfferToken(offertoken)
                                        .build()
                                )
                                val subname = product.name
                                val description = product.description

                                val formattedprice = product.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].formattedPrice
                                val billingperiod = product.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].billingPeriod
                                val recurrenceMode = product.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].recurrenceMode

                                val number = billingperiod.substring(1,2)
                                val duration = billingperiod.substring(2,3)

                                var dur : String = ""
                                if (recurrenceMode == 2){
                                    when (duration) {
                                        "M" -> {
                                            dur = "For $number Month"
                                        }
                                        "Y" -> {
                                            dur = "For $number Year"
                                        }
                                        "W" -> {
                                            dur = "For $number Week"
                                        }
                                        "D" -> {
                                            dur = "For $number Days"
                                        }
                                    }
                                }else{
                                    when(billingperiod) {
                                        "P1M" -> {
                                            dur = "/Monthly"
                                        }
                                        "P6M" -> {
                                            dur = "/Every 6 Month"
                                        }
                                        "P1Y" -> {
                                            dur = "/Yearly"
                                        }
                                        "P1W" -> {
                                            dur = "/Weekly"
                                        }
                                        "P3W" -> {
                                            dur = "Every /3 Week"
                                        }
                                    }
                                }
                                var phase = "$formattedprice $dur"
                                for (i in product.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList.indices){
                                    val period = product.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[i].billingPeriod
                                    val price = product.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[i].formattedPrice

                                    when (period){
                                        "P1M" -> {
                                            dur = "/Monthly"
                                        }
                                        "P6M" -> {
                                            dur = "/Every 6 Month"
                                        }
                                        "P1Y" -> {
                                            dur = "/Yearly"
                                        }
                                        "P1W" -> {
                                            dur = "/Weekly"
                                        }
                                        "P3W" -> {
                                            dur = "Every /3 Week"
                                        }
                                    }

                                    phase += "\n"+price+dur;

                                }

                                // implement your logic
                                // extract the variable you needed
                             }

                        }


                    }
                    runOnUiThread{
                        try {
                            Thread.sleep(1000)
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                        // display your data here to the ui
                    }

                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                getData()
            }
        })
    }

    private fun Launchthepurchaseflow(){
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    val queryProductDetailsParams =
                        QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                ImmutableList.of(
                                    QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId("product_id_example")
                                        .setProductType(BillingClient.ProductType.SUBS)  // ProductType.INAPP
                                        .build()))
                            .build()

                    billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
                            billingResult,
                            productDetailsList ->
                        productDetailsList.forEach{ product ->
                            val offertoken = product.subscriptionOfferDetails?.get(0)?.offerToken.toString()

                            val productDetailsParamsList = listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                    .setProductDetails(product)
                                    // to get an offer token, call ProductDetails.subscriptionOfferDetails()
                                    // for a list of offers that are available to the user
                                    .setOfferToken(offertoken)
                                    .build()
                            )

                            val billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build()

                            // Launch the billing flow
                            val billingResult = billingClient.launchBillingFlow(this@billingactivity, billingFlowParams)

                        }
                    }

                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Launchthepurchaseflow()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient!=null){
            billingClient.endConnection()
        }
    }

}