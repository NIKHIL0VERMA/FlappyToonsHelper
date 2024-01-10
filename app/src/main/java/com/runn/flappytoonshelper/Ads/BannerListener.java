package com.runn.flappytoonshelper.Ads;

public interface BannerListener {
    void onBannerLoaded();

    void onBannerFailedToLoad(int errorCode);
}
