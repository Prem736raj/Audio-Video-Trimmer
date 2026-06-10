package com.example.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import com.video.trimmer.audio.cutter.R
import com.video.trimmer.audio.cutter.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdComponent() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Using Production ID exclusively for Play Store release
    val adUnitId = "ca-app-pub-8204679574020840/7965479877"

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                    if (BuildConfig.DEBUG) {
                        android.widget.Toast.makeText(context, "Native Ad Failed: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (nativeAd != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val adView = LayoutInflater.from(ctx)
                        .inflate(R.layout.ad_unified, null) as NativeAdView

                    // Locate view elements
                    adView.headlineView = adView.findViewById<TextView>(R.id.ad_headline)
                    adView.bodyView = adView.findViewById<TextView>(R.id.ad_body)
                    adView.callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
                    adView.iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
                    adView.advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
                    adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)

                    adView
                },
                update = { adView ->
                    val ad = nativeAd ?: return@AndroidView

                    // Headline
                    (adView.headlineView as? TextView)?.text = ad.headline

                    // Body
                    if (ad.body == null) {
                        adView.bodyView?.visibility = android.view.View.GONE
                    } else {
                        adView.bodyView?.visibility = android.view.View.VISIBLE
                        (adView.bodyView as? TextView)?.text = ad.body
                    }

                    // Call to Action
                    if (ad.callToAction == null) {
                        adView.callToActionView?.visibility = android.view.View.GONE
                    } else {
                        adView.callToActionView?.visibility = android.view.View.VISIBLE
                        (adView.callToActionView as? Button)?.text = ad.callToAction
                    }

                    // Icon
                    if (ad.icon == null) {
                        adView.iconView?.visibility = android.view.View.GONE
                    } else {
                        (adView.iconView as? ImageView)?.setImageDrawable(ad.icon?.drawable)
                        adView.iconView?.visibility = android.view.View.VISIBLE
                    }

                    // Advertiser
                    if (ad.advertiser == null) {
                        adView.advertiserView?.visibility = android.view.View.GONE
                    } else {
                        (adView.advertiserView as? TextView)?.text = ad.advertiser
                        adView.advertiserView?.visibility = android.view.View.VISIBLE
                    }

                    adView.setNativeAd(ad)
                }
            )
        }
    }
}
