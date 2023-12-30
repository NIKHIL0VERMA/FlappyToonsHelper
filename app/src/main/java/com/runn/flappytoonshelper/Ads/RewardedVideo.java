package com.runn.flappytoonshelper.Ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class RewardedVideo {
    private final String TAG = "Helper";

    private RewardedAd rewardedAd = null;
    private final Activity activity;
    private final RewardedVideoListener defaultRewardedVideoListener;

    public RewardedVideo(Activity activity, final RewardedVideoListener defaultRewardedVideoListener) {
        this.activity = activity;
        this.defaultRewardedVideoListener = defaultRewardedVideoListener;
        MobileAds.initialize(activity);
    }

    public boolean isLoaded() {
        return rewardedAd != null;
    }

    public void load(final String id, AdRequest adRequest) {

        RewardedAd.load(activity, id, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                super.onAdLoaded(rewardedAd);
                setAd(rewardedAd);
                Log.d(TAG, "AdMob: onAdLoaded: rewarded video");
                defaultRewardedVideoListener.onRewardedVideoLoaded();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                // safety
                setAd(null);
                Log.d(TAG, "AdMob: onAdFailedToLoad. errorCode: " + loadAdError.getCode());
                defaultRewardedVideoListener.onRewardedVideoFailedToLoad(loadAdError.getCode());
            }
        });
    }

    public void show() {
        if (rewardedAd != null) {
            rewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "AdMob: "
                        + String.format(" onRewarded! currency: %s amount: %d", rewardItem.getType(), rewardItem.getAmount()));
                defaultRewardedVideoListener.onRewarded(rewardItem.getType(), rewardItem.getAmount());
            });
        }
    }

    private void setAd(RewardedAd rewardedAd) {
        // Avoid memory leaks.
        if (this.rewardedAd != null)
            this.rewardedAd.setFullScreenContentCallback(null);
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    Log.d(TAG, "AdMob: onAdClicked");
                    defaultRewardedVideoListener.onRewardedClicked();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    // TODO: Test if new video ads are loaded
//                    setAd(null);
//                    RewardedAd.load(activity, id, adRequest, rewardedAdLoadCallback);
                    Log.d(TAG, "AdMob: onAdDismissedFullScreenContent");
                    defaultRewardedVideoListener.onRewardedVideoClosed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    super.onAdFailedToShowFullScreenContent(adError);
                    Log.d(TAG, "AdMob: onAdFailedToShowFullScreenContent");
                    defaultRewardedVideoListener.onRewardedVideoFailedToLoad(adError.getCode());
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    Log.d(TAG, "AdMob: onAdImpression");
                    defaultRewardedVideoListener.onRewardedAdImpression();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent();
                    Log.d(TAG, "AdMob: onAdShowedFullScreenContent");
                    defaultRewardedVideoListener.onRewardedVideoOpened();
                }
            });
        }
        this.rewardedAd = rewardedAd;
    }
}