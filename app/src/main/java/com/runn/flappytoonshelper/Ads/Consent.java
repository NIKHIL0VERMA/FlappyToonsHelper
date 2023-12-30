package com.runn.flappytoonshelper.Ads;


import android.app.Activity;
import android.util.Log;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class Consent {
    
    private final String TAG = "Helper";
    private final ConsentInformation consentInformation;
    private ConsentForm consentForm;

    private final Activity activity;
    private final ConsentListener defaultConsentListener;

    public Consent(Activity activity,
               final boolean testingConsent,
               final String testingDeviceId,
               ConsentListener defaultConsentListener) {
        this.activity = activity;
        this.defaultConsentListener = defaultConsentListener;

        consentInformation = UserMessagingPlatform.getConsentInformation(this.activity);

        // Set tag for under age of consent. false means users are not under
        // age.
        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        Log.d(TAG, "Consent status: " +
                consentInformation.getConsentStatus());

        if(testingConsent) {

            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this.activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(testingDeviceId)
                    .build();

            params = new ConsentRequestParameters
                    .Builder()
                    .setConsentDebugSettings(debugSettings)
                    .build();

            Log.d(TAG, "Consent status: " +
                    consentInformation.getConsentStatus());
        }

        consentInformation.requestConsentInfoUpdate(
                this.activity,
                params,
                () -> {
                    // The consent information state was updated.
                    // You are now ready to check if a form is available.
                    Log.d(TAG, "AdMob: onConsentInfoUpdateSuccess");

                    defaultConsentListener.onConsentInfoUpdateSuccess();

                    if (consentInformation.isConsentFormAvailable()) {
                        Log.d(TAG, "AdMob: Consent information available.");
                        loadForm();
                    }else{
                        Log.d(TAG, "AdMob: No consent information.");
                        Log.d(TAG, "Consent status: " +
                                String.valueOf(consentInformation.getConsentStatus()));

                        if(ConsentInformation.ConsentStatus.NOT_REQUIRED ==
                                consentInformation.getConsentStatus()){
                            defaultConsentListener.onAppCanRequestAds(consentInformation.getConsentStatus());
                        }
                    }
                },
                formError -> {
                    // Handle the error.
                    Log.d(TAG, "AdMob: onConsentInfoUpdateFailure: "
                            + formError.getErrorCode()
                            + " - "
                            + formError.getMessage()
                    );
                    defaultConsentListener.onConsentInfoUpdateFailure(formError.getErrorCode(),
                            formError.getMessage());
                });
    }

    public void resetConsentInformation(){
        if(consentInformation != null) {
            consentInformation.reset();
        }
    }

    public void loadForm() {
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(
                this.activity,
                consentForm -> {
                    Consent.this.consentForm = consentForm;
                    Log.d(TAG, "AdMob: onConsentFormLoadSuccess");
                    if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(
                                Consent.this.activity,
                                formError -> {
                                    if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                                        // App can start requesting ads.
                                        Log.d(TAG, "AdMob: App can start requesting ads.");
                                        defaultConsentListener.onAppCanRequestAds(consentInformation.getConsentStatus());
                                    }

                                    // Handle dismissal by reloading form.
                                    loadForm();
                                });
                    }else if(consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED){
                        Log.d(TAG, "AdMob: App can start requesting ads.");
                        defaultConsentListener.onAppCanRequestAds(consentInformation.getConsentStatus());
                    }
                },
                formError -> {
                    // Handle the error.
                    Log.d(TAG, "AdMob: onConsentInfoUpdateFailure: "
                            + formError.getErrorCode()
                            + " - "
                            + formError.getMessage()
                    );
                }
        );
    }
}