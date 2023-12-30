package com.runn.flappytoonshelper.Ads;

public interface RewardedVideoListener {
    void onRewardedVideoLoaded();

    void onRewardedVideoFailedToLoad(int errorCode);

    void onRewardedVideoOpened();

    void onRewardedVideoClosed();

    void onRewarded(String type, int amount);

    /* Removed in GMS Ads SDK version 19 or 20.
    void onRewardedVideoStarted();
    void onRewardedVideoCompleted();
    */
    // new
    void onRewardedClicked();

    void onRewardedAdImpression();
}
