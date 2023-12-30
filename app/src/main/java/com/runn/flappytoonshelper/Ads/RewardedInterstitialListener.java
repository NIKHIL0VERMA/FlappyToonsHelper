package com.runn.flappytoonshelper.Ads;

public interface RewardedInterstitialListener {
    void onRewardedInterstitialLoaded();

    void onRewardedInterstitialOpened();

    void onRewardedInterstitialClosed();

    void onRewardedInterstitialFailedToLoad(int errorCode);

    void onRewardedInterstitialFailedToShow(int errorCode);

    void onRewarded(String type, int amount);

    void onRewardedClicked();

    void onRewardedAdImpression();
}
