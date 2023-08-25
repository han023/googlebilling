package com.hansoft.googlebilling

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient;
    var ispermium = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initilize the billing client
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build();

        query_purchase()

    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }


    fun btnclick(view:View){
        startActivity(Intent(this@MainActivity,billingactivity::class.java))
    }

    private fun query_purchase(){
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    val executorservice = Executors.newSingleThreadExecutor()
                    executorservice.execute{
                        try {
                            billingClient.queryPurchasesAsync(
                                QueryPurchasesParams.newBuilder().
                                setProductType((BillingClient.ProductType.INAPP)).build(),
                                {billingResult,purchaseList ->
                                    purchaseList.forEach{purchase ->
                                        if (purchase != null && purchase.isAcknowledged){
                                            ispermium = true
                                        }
                                    }
                                }
                            )
                        } catch (e:Exception){
                            ispermium = false
                        }
                    }
                    runOnUiThread{
                        try {
                            Thread.sleep(1000)
                        }catch (e:Exception){
                            e.printStackTrace()
                        }

                        if (ispermium){
                            ConnectionClass.permium = true
                            ConnectionClass.locked = false
                        }else{
                            ConnectionClass.permium = false
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }


}