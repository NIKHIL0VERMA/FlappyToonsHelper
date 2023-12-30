package com.runn.flappytoonshelper.Ads;

public interface ConsentListener {
    void onConsentInfoUpdateSuccess();

    void onConsentInfoUpdateFailure(int errorCode, String msg);

    void onAppCanRequestAds(int consentStatus);
}
