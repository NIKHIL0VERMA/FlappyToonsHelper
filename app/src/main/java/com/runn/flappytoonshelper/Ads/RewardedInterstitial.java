package com.runn.flappytoonshelper.Ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

public class RewardedInterstitial {
    private final String TAG = "Helper";
    
    private RewardedInterstitialAd rewardedAd = null;
    private final Activity activity;
    private final RewardedInterstitialListener defaultRewardedInterstitialListener;

    public RewardedInterstitial(Activity activity, final RewardedInterstitialListener defaultRewardedInterstitialListener) {
        this.activity = activity;
        this.defaultRewardedInterstitialListener = defaultRewardedInterstitialListener;
        MobileAds.initialize(activity);
    }

    public boolean isLoaded() {
        return rewardedAd != null;
    }

    public void load(final String id, AdRequest adRequest) {

        RewardedInterstitialAd.load(activity, id, adRequest, new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedAd) {
                super.onAdLoaded(rewardedAd);
                setAd(rewardedAd);
                Log.d(TAG, "AdMob: onAdLoaded: rewarded interstitial");
                defaultRewardedInterstitialListener.onRewardedInterstitialLoaded();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                // safety
                setAd(null);
                Log.d(TAG, "AdMob: onAdFailedToLoad. errorCode: " + loadAdError.getCode());
                defaultRewardedInterstitialListener.onRewardedInterstitialFailedToLoad(loadAdError.getCode());
            }
        });
    }

    public void show() {
        if (rewardedAd != null) {
            rewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "AdMob: "
                        + String.format(" onRewarded! currency: %s amount: %d", rewardItem.getType(), rewardItem.getAmount()));
                defaultRewardedInterstitialListener.onRewarded(rewardItem.getType(), rewardItem.getAmount());
            });
        }
    }

    private void setAd(RewardedInterstitialAd rewardedAd) {
        // Avoid memory leaks.
        if (this.rewardedAd != null)
            this.rewardedAd.setFullScreenContentCallback(null);
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    Log.d(TAG, "AdMob: onAdClicked");
                    defaultRewardedInterstitialListener.onRewardedClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    // TODO: Test if new video ads are loaded
//                    setAd(null);
//                    RewardedAd.load(activity, id, adRequest, rewardedAdLoadCallback);
                    Log.d(TAG, "AdMob: onAdDismissedFullScreenContent");
                    defaultRewardedInterstitialListener.onRewardedInterstitialClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    Log.d(TAG, "AdMob: onAdFailedToShowFullScreenContent");
                    defaultRewardedInterstitialListener.onRewardedInterstitialFailedToShow(adError.getCode());
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "AdMob: onAdImpression");
                    defaultRewardedInterstitialListener.onRewardedAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();
                    Log.d(TAG, "AdMob: onAdShowedFullScreenContent");
                    defaultRewardedInterstitialListener.onRewardedInterstitialOpened();
                }
            });
        }
        this.rewardedAd = rewardedAd;
    }
}
