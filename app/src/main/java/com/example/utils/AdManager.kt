package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.video.trimmer.audio.cutter.BuildConfig

object AdManager {

    private const val TAG = "AdManager"

    // Using Production IDs exclusively for Play Store release
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-8204679574020840/4582478497"
    private val REWARDED_AD_UNIT_ID = "ca-app-pub-8204679574020840/5830271122"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    var isInterstitialReady = false
        private set

    /**
     * Loads the Interstitial Ad in the background.
     */
    fun loadInterstitialAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    interstitialAd = null
                    isInterstitialReady = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad was loaded.")
                    interstitialAd = ad
                    isInterstitialReady = true
                }
            }
        )
    }

    /**
     * Shows the Interstitial Ad if ready, otherwise calls the callback immediately.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null && isInterstitialReady) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad was dismissed.")
                    interstitialAd = null
                    isInterstitialReady = false
                    onAdDismissed()
                    // Pre-load the next one
                    loadInterstitialAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Interstitial Ad failed to show.")
                    interstitialAd = null
                    isInterstitialReady = false
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            onAdDismissed()
            // Try loading again
            loadInterstitialAd(activity)
        }
    }

    /**
     * Loads the Rewarded Ad in the background.
     */
    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    rewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad was loaded.")
                    rewardedAd = ad
                }
            }
        )
    }

    /**
     * Shows the Rewarded Ad if ready. Triggers onRewardEarned only if the user finishes the ad.
     * Triggers onAdDismissed in all cases so the UI can unlock.
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onAdDismissed: () -> Unit) {
        if (rewardedAd != null) {
            var earnedReward = false
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad was dismissed.")
                    rewardedAd = null
                    if (earnedReward) {
                        onRewardEarned()
                    }
                    onAdDismissed()
                    // Pre-load next
                    loadRewardedAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Rewarded Ad failed to show.")
                    rewardedAd = null
                    onAdDismissed()
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned the reward.")
                earnedReward = true
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            // If ad fails to load, we just grant the reward (export) anyway so user isn't blocked by bad internet
            onRewardEarned()
            onAdDismissed()
            loadRewardedAd(activity)
        }
    }
}
