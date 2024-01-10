package com.runn.flappytoonshelper;

import static androidx.core.app.ActivityCompat.startIntentSenderForResult;
import static androidx.core.content.ContextCompat.startActivity;
import static com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdkNotInitializedException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.runn.flappytoonshelper.Ads.Banner;
import com.runn.flappytoonshelper.Ads.BannerListener;
import com.runn.flappytoonshelper.Ads.Consent;
import com.runn.flappytoonshelper.Ads.ConsentListener;
import com.runn.flappytoonshelper.Ads.RewardedInterstitial;
import com.runn.flappytoonshelper.Ads.RewardedInterstitialListener;
import com.runn.flappytoonshelper.Ads.RewardedVideo;
import com.runn.flappytoonshelper.Ads.RewardedVideoListener;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public class Helper extends GodotPlugin {
    // For FB
    private static CallbackManager callbackManager;
    private final String TAG = "Helper";
    private final Activity activity;
    private final int REQ_ONE_TAP = 55;
    public static String token = "";
    // For Admob
    private boolean isReal = false;
    private boolean isForChildDirectedTreatment = false;
    private boolean isPersonalized = true;
    private String maxAdContentRating = "";
    private Bundle extras = null;
    private FrameLayout layout = null; // Store the layout
    private RewardedVideo rewardedVideo = null;
    private RewardedInterstitial rewardedInterstitial = null;
    private Banner banner = null;
    private Consent consent;
    // For Google
    private SignInClient onTapClient;
    private BeginSignInRequest signUpRequest;
    // For Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firestore;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseCrashlytics firebaseCrashlytics;

    public Helper(Godot godot) {
        super(godot);
        this.activity = getActivity();
    }

    @Override
    public void onMainPause() {
        super.onMainPause();
    }

    @Nullable
    @Override
    public View onMainCreate(Activity activity) {
        layout = new FrameLayout(activity);
        // Logging and sending the crashes to server
        firebaseCrashlytics = FirebaseCrashlytics.getInstance();
        firebaseCrashlytics.setCrashlyticsCollectionEnabled(true);
        if (firebaseCrashlytics.didCrashOnPreviousExecution()) {
            firebaseCrashlytics.checkForUnsentReports();
            firebaseCrashlytics.sendUnsentReports();
        } else {
            firebaseCrashlytics.deleteUnsentReports();
        }
        return layout;
    }

    @Override
    public void onMainResume() {
        super.onMainResume();
    }

    @Override
    public void onMainDestroy() {
        super.onMainDestroy();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Helper";
    }

    @NonNull
    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                "init",
                "loginWithAnonym",
                "loginWithGoogle",
                "getScore",
                "setScore",
                "logout",
                "saveToRTDB",
                "getFromRTDB",
                "boardData",
                "loginWithFB",
                "share",
                "resetConsentInformation",
                "requestConsentInfoUpdate",
                "showRewardedInterstitial",
                "loadRewardedInterstitial",
                "showRewardedVideo",
                "loadRewardedVideo",
                "sendEvent",
                "loadBanner",
                "showBanner",
                "move",
                "resize",
                "hideBanner",
                "getBannerWidth",
                "getBannerHeight",
                "isLoggedIn"
        );
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signalInfo = new ArraySet<>();

        // Rewarded ads
        signalInfo.add(new SignalInfo("on_rewarded_interstitial_ad_closed"));
        signalInfo.add(new SignalInfo("on_rewarded_interstitial_ad_failed_to_load", Integer.class));
        signalInfo.add(new SignalInfo("on_rewarded_interstitial_ad_loaded"));
        signalInfo.add(new SignalInfo("on_rewarded_interstitial_ad_opened"));
        signalInfo.add(new SignalInfo("on_rewarded_video_ad_closed"));
        signalInfo.add(new SignalInfo("on_rewarded_video_ad_failed_to_load", Integer.class));
        signalInfo.add(new SignalInfo("on_rewarded_video_ad_loaded"));
        signalInfo.add(new SignalInfo("on_rewarded_video_ad_opened"));
        signalInfo.add(new SignalInfo("on_rewarded", String.class, Integer.class));
        signalInfo.add(new SignalInfo("on_rewarded_clicked"));
        signalInfo.add(new SignalInfo("on_rewarded_impression"));

        // Banner ad
        signalInfo.add(new SignalInfo("on_admob_ad_loaded"));
        signalInfo.add(new SignalInfo("on_admob_banner_failed_to_load", Integer.class));

        // Consent info
        signalInfo.add(new SignalInfo("on_consent_info_update_success"));
        signalInfo.add(
                new SignalInfo("on_consent_info_update_failure",
                        Integer.class, String.class));
        signalInfo.add(new SignalInfo("on_app_can_request_ads", Integer.class));

        // Firebase
        signalInfo.add(new SignalInfo("on_server_update", String.class, Boolean.class)); // boolean is_error occurs
        signalInfo.add(new SignalInfo("on_completed"));
        signalInfo.add(new SignalInfo("on_get_rtdb", Dictionary.class));

        // Leaderboard
        signalInfo.add(new SignalInfo("on_boardData", Dictionary.class));
        signalInfo.add(new SignalInfo("on_getScore", Integer.class));
        return signalInfo;
    }

    /**
     * Prepare for work with AdMob
     *
     * @param isReal Tell if the environment is for real or test
     */
    @UsedByGodot
    public void init(boolean isReal, boolean isFirstTime, String Host) {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        if (!Objects.equals(Host, "")){
            firebaseDatabase.useEmulator(Host, 9000);
            firestore.useEmulator(Host, 8080);
            Log.d(TAG, "FIREBASE: got host:" + Host);
        }
        // For Admob
        this.initWithContentRating(isReal, false, true, "");
        // For Login
        activity.runOnUiThread(() -> {
            try {
                // Getting the instances
                callbackManager = CallbackManager.Factory.create();
                onTapClient = Identity.getSignInClient(activity.getApplicationContext());
                signUpRequest = BeginSignInRequest.builder()
                        .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId("1017278365273-qs4v9kr64not3aot1242ekajic088hv0.apps.googleusercontent.com")
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                        .build();
                // Configuring if not login or first time opened app
                if (isFirstTime || !isLoggedIn()) {
                    // Fb login Manager
                    FacebookSdk.setApplicationId("352763776016419");
                    LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            handleFacebookAccessToken(loginResult.getAccessToken());
                            emitSignal("on_server_update", "Authenticated from Facebook...", false);
                        }

                        @Override
                        public void onCancel() {
                            sendEvent("FB", "Fb login cancelled by the user");
                            emitSignal("on_server_update", "Cancelled Facebook Auth", true);
                        }

                        @Override
                        public void onError(@NonNull FacebookException e) {
                            sendEvent("FB", String.valueOf(e));
                            sendCrashes("Fb login", String.valueOf(e), "Fb error");
                            emitSignal("on_server_update", "Facebook error", true);
                        }
                    });
                    Log.d(TAG, "Init completed");
                }
            } catch (FacebookSdkNotInitializedException e) {
                sendCrashes("Fb SDK", String.valueOf(e), "Fb error");
            } catch (Exception e) {
                firebaseCrashlytics.recordException(e);
            }
        });
    }

    private void handleFacebookAccessToken(@NonNull AccessToken accessToken) {
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        emitSignal("on_server_update", "Server Error please try again...", true);
                    } else {
                        firebaseUser = firebaseAuth.getCurrentUser();
                        cfgServerDB(firebaseUser.getDisplayName(),
                                firebaseUser.getPhotoUrl().getPath(),
                                token);
                    }
                });
    }

    private void handleGoogleAccessToken(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener((Executor) this, task -> {
                    if (!task.isSuccessful()) {
                        emitSignal("on_server_update", "Server Error please try again...", true);
                    } else {
                        firebaseUser = firebaseAuth.getCurrentUser();
                        cfgServerDB(firebaseUser.getDisplayName(),
                                firebaseUser.getPhotoUrl().getPath(),
                                token);
                    }
                });
    }

    @UsedByGodot
    public void loginWithAnonym(){
        emitSignal("on_server_update", "Flying toward server", false);
        firebaseAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()){
                    emitSignal("on_server_update", "Server Error please try again...", true);
                }else{
                    firebaseUser = firebaseAuth.getCurrentUser();
                    cfgServerDB(randomIdentifier(),
                            "https://firebasestorage.googleapis.com/v0/b/flappy-toons.appspot.com/o/icon.png?alt=media&token=76a10d02-db85-41e6-9f06-76ea28b304a0",
                            token);
                }
            }
        });
    }

    @UsedByGodot
    public void loginWithGoogle(){
        onTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(activity, new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult beginSignInResult) {
                        try{
                            startIntentSenderForResult(
                                    activity,
                                    beginSignInResult.getPendingIntent().getIntentSender(),
                                    REQ_ONE_TAP,
                                    null,
                                    0,
                                    0,
                                    0,
                                    null);
                        }catch (IntentSender.SendIntentException e){
                            sendEvent("One Tap Google", e.toString());
                            emitSignal("on_server_update", "Google login failed", true);
                        }
                    }
                })
                .addOnFailureListener(activity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        sendEvent("One Tap Google", e.toString());
                        emitSignal("on_server_update", "Google login error", true);
                    }
                });
    }

    private void cfgServerDB(String name, String photoURL, String msgToken){
        // For leaderboard
        Map<String, Object> player = new HashMap<>();
        player.put("name", name);
        player.put("photo", photoURL);
        player.put("score", 0);
        firestore.collection("players")
                .document(firebaseUser.getUid())
                .set(player);

        // For identity and msg token
        Map<String, String> map = new HashMap<>();
        map.put(name, token);
        firestore.collection("msgToken")
                .document(firebaseUser.getUid())
                .set(map);
        emitSignal("on_completed");
    }


    @UsedByGodot
    public void getScore(){
        firestore.collection("players")
                .document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            int score = Objects.requireNonNull(snapshot.getLong("score")).intValue();
                            emitSignal("on_getScore", score);
                        }
                    }
                });
    }

    @UsedByGodot
    public void setScore(int score){
        firestore.collection("players")
                .document(firebaseUser.getUid())
                .update("score", score);
    }

    @UsedByGodot
    public void logout(){
        firebaseAuth.signOut();
        emitSignal("on_server_update", "Logged out", false);
    }

    @NonNull
    private String randomIdentifier() {
        StringBuilder builder = new StringBuilder();
        builder.append("FT-");
        int desiredLength = 6;
        builder.append(UUID.randomUUID()
                .toString()
                .substring(0, desiredLength));
        return builder.toString();
    }

    /**
     * Getting the data from RTDB
     * use to restore unlocked players backgrounds pipes for user
     * Use this to secure user data
     * **/
    @UsedByGodot
    public void getFromRTDB(String key){
        Dictionary db = new Dictionary();
        firebaseDatabase.getReference().child(firebaseUser.getUid()).child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap:
                        snapshot.getChildren()) {
                    db.put(snap.getKey(), snap.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                sendCrashes(String.valueOf(error.getMessage()), String.valueOf(error.getDetails()), String.valueOf(error.getCode()));
                sendEvent("Error getting " + key, error.toString());
            }
        });
        emitSignal("on_get_rtdb", db);
    }

    /**
     * Saving the data to RTDB
     * Use it to store unlocked things
     * Backup and user management
     */
    @UsedByGodot
    public void saveToRTDB(String key, Dictionary db){
        if (firebaseAuth != null) {
            firebaseDatabase.getReference().child(firebaseUser.getUid()).child(key).setValue(db);
        }
    }

    /**
     * Getting the user's data to populate the leaderboard section
     *
     * @return the dictionary to godot with needed data
     */
    @UsedByGodot
    public void boardData(){
        Dictionary toReturn = new Dictionary();
        Query query = firestore.collection("players")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
    @Override
    public void onComplete(@NonNull Task<QuerySnapshot> task) {
        if (task.isSuccessful()) {
            // Loop through the documents in the snapshot
            int i = 0;
            for (QueryDocumentSnapshot document : task.getResult()) {
                // Get the fields of the document
                String name = document.getString("name");
                String photo = document.getString("photo");
                int score = Objects.requireNonNull(document.getLong("score")).intValue();
                Dictionary pl = new Dictionary();
                pl.put("name",name);
                pl.put("score", score);
                pl.put("photo", photo);
                toReturn.put(String.valueOf(i++), pl);
            }
        emitSignal("on_boardData", toReturn);
        } else {
            // Handle the error
            firebaseCrashlytics.recordException(Objects.requireNonNull(task.getException()));
            sendCrashes("loading Players error", String.valueOf(task.getException()), "Firestore error");
            emitSignal("on_server_update", "Server Error please try again...", true);
        }
    }
});
}

    @UsedByGodot
    public void share(int score){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareBody = "Check out this game: Flappy Toons" + ". My score is: " + score + ". You can download it from: https://play.google.com/store/apps/details?id=com.runn.flappytoons";
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(this.activity.getApplicationContext(), shareIntent, null);
    }

    @UsedByGodot
    public void loginWithFB(){
        List<String> perm = new ArrayList<String>();
        perm.add("user_friends");
        perm.add("email");
        perm.add("public_profile");
        LoginManager.getInstance().logInWithReadPermissions(activity, perm);
    }

    @UsedByGodot
    public boolean isLoggedIn(){
        return (firebaseUser != null);
    }

    @UsedByGodot
    public void sendEvent(String key, String msg) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(activity.getApplicationContext());
        Bundle bundle = new Bundle();
        bundle.putString(key, msg);
        if(isLoggedIn()) {
            bundle.putString("userID", firebaseUser.getUid());
        }
        analytics.logEvent("Event_Send", bundle);
    }

    public void sendCrashes(String key, String value, String msg) {
        firebaseCrashlytics = FirebaseCrashlytics.getInstance();
        firebaseCrashlytics.setCustomKey(key, value);
        if(isLoggedIn()) {
            firebaseCrashlytics.setUserId(firebaseUser.getUid());
        }
        firebaseCrashlytics.log(msg);
    }

    /**
     * passing the activity result to callbackManager of the FB
     * @param requestCode the requested code for identity
     * @param resultCode the result code of request from intent
     * @param data the data of the result
     */
    @Override
    public void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ONE_TAP){
            try {
                SignInCredential credential = onTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                handleGoogleAccessToken(idToken);
            } catch (ApiException e){
                sendEvent("One Tap Google", e.toString());
                emitSignal("on_server_update", "Google login failed", true);
            }
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onMainActivityResult(requestCode, resultCode, data);
    }

    /**
     * Init with content rating additional options
     *
     * @param isReal                      Tell if the environment is for real or test
     * @param isForChildDirectedTreatment Target audience is children.
     * @param isPersonalized              If ads should be personalized or not.
     *                                    GDPR compliance within the European Economic Area requires that you
     *                                    disable ad personalization if the user does not wish to opt into
     *                                    ad personalization.
     * @param maxAdContentRating          must be "G", "PG", "T" or "MA"
     */
    public void initWithContentRating(
            boolean isReal,
            boolean isForChildDirectedTreatment,
            boolean isPersonalized,
            String maxAdContentRating) {

        this.isReal = isReal;
        this.isForChildDirectedTreatment = isForChildDirectedTreatment;
        this.isPersonalized = isPersonalized;
        this.maxAdContentRating = maxAdContentRating;

        this.setRequestConfigurations();

        if (!isPersonalized) {
            // https://developers.google.com/admob/android/eu-consent#forward_consent_to_the_google_mobile_ads_sdk
            if (extras == null) {
                extras = new Bundle();
            }
            extras.putString("npa", "1");
        }

        Log.d(TAG, "AdMob: init with content rating options");
    }


    private void setRequestConfigurations() {
        if (!this.isReal) {
            List<String> testDeviceIds = Arrays.asList(AdRequest.DEVICE_ID_EMULATOR, getAdMobDeviceId());
            RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration()
                    .toBuilder()
                    .setTestDeviceIds(testDeviceIds)
                    .build();
            MobileAds.setRequestConfiguration(requestConfiguration);
        }

        if (this.isForChildDirectedTreatment) {
            RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration()
                    .toBuilder()
                    .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                    .build();
            MobileAds.setRequestConfiguration(requestConfiguration);
        }

        // StringEquality false positive
        //noinspection StringEquality
        if (this.maxAdContentRating != null && this.maxAdContentRating != "") {
            RequestConfiguration requestConfiguration = MobileAds.getRequestConfiguration()
                    .toBuilder()
                    .setMaxAdContentRating(this.maxAdContentRating)
                    .build();
            MobileAds.setRequestConfiguration(requestConfiguration);
        }
    }


    /**
     * Returns AdRequest object constructed considering the extras.
     *
     * @return AdRequest object
     */
    private AdRequest getAdRequest() {
        AdRequest.Builder adBuilder = new AdRequest.Builder();
        AdRequest adRequest;
        if (!this.isForChildDirectedTreatment && extras != null) {
            adBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        }

        adRequest = adBuilder.build();
        return adRequest;
    }

    /* Rewarded Video
     * ********************************************************************** */

    /**
     * Load a Rewarded Video
     *
     * @param id AdMod Rewarded video ID
     */
    @UsedByGodot
    public void loadRewardedVideo(final String id) {
        activity.runOnUiThread(() -> {
            rewardedVideo = new RewardedVideo(activity, new RewardedVideoListener() {
                @Override
                public void onRewardedVideoLoaded() {
                    emitSignal("on_rewarded_video_ad_loaded");
                }

                @Override
                public void onRewardedVideoFailedToLoad(int errorCode) {
                    emitSignal("on_rewarded_video_ad_failed_to_load", errorCode);
                }

                @Override
                public void onRewardedVideoOpened() {
                    emitSignal("on_rewarded_video_ad_opened");
                }

                @Override
                public void onRewardedVideoClosed() {
                    emitSignal("on_rewarded_video_ad_closed");
                }

                @Override
                public void onRewarded(String type, int amount) {
                    emitSignal("on_rewarded", type, amount);
                }

                @Override
                public void onRewardedClicked() {
                    emitSignal("on_rewarded_clicked");
                }

                @Override
                public void onRewardedAdImpression() {
                    emitSignal("on_rewarded_impression");
                }
            });
            rewardedVideo.load(id, getAdRequest());
        });
    }

    /**
     * Show a Rewarded Video
     */
    @UsedByGodot
    public void showRewardedVideo() {
        activity.runOnUiThread(() -> {
            if (rewardedVideo == null) {
                return;
            }
            rewardedVideo.show();
        });
    }

    /* Rewarded Interstitial
     * ********************************************************************** */

    /**
     * Load a Rewarded Interstitial
     *
     * @param id AdMod Rewarded interstitial ID
     */
    @UsedByGodot
    public void loadRewardedInterstitial(final String id) {
        activity.runOnUiThread(() -> {
            rewardedInterstitial = new RewardedInterstitial(activity, new RewardedInterstitialListener() {
                @Override
                public void onRewardedInterstitialLoaded() {
                    emitSignal("on_rewarded_interstitial_ad_loaded");
                }

                @Override
                public void onRewardedInterstitialOpened() {
                    emitSignal("on_rewarded_interstitial_ad_opened");
                }

                @Override
                public void onRewardedInterstitialClosed() {
                    emitSignal("on_rewarded_interstitial_ad_closed");
                }

                @Override
                public void onRewardedInterstitialFailedToLoad(int errorCode) {
                    emitSignal("on_rewarded_interstitial_ad_failed_to_load", errorCode);
                }

                @Override
                public void onRewardedInterstitialFailedToShow(int errorCode) {
                    Log.d(TAG, String.valueOf(errorCode));
                }

                @Override
                public void onRewarded(String type, int amount) {
                    emitSignal("on_rewarded", type, amount);
                }

                @Override
                public void onRewardedClicked() {
                    emitSignal("on_rewarded_clicked");
                }

                @Override
                public void onRewardedAdImpression() {
                    emitSignal("on_rewarded_impression");
                }
            });
            rewardedInterstitial.load(id, getAdRequest());
        });
    }

    /**
     * Show a Rewarded Interstitial
     */
    @UsedByGodot
    public void showRewardedInterstitial() {
        activity.runOnUiThread(() -> {
            if (rewardedInterstitial == null) {
                return;
            }
            rewardedInterstitial.show();
        });
    }

    /* ConsentInformation
     * ********************************************************************** */
    @UsedByGodot
    public void requestConsentInfoUpdate(final boolean testingConsent) {

        activity.runOnUiThread(() -> consent = new Consent(activity,
                testingConsent,
                testingConsent ? getAdMobDeviceId() : "",
                new ConsentListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        emitSignal("on_consent_info_update_success");
                    }

                    @Override
                    public void onConsentInfoUpdateFailure(int errorCode, String errorMessage) {
                        emitSignal("on_consent_info_update_failure", errorCode, errorMessage);
                    }

                    @Override
                    public void onAppCanRequestAds(int consentStatus) {
                        emitSignal("on_app_can_request_ads", consentStatus);
                    }
                }));
    }

    /**
     * Reset the consent information
     * It is required to have consent to show ads
     * GDPR
     * */
    @UsedByGodot
    public void resetConsentInformation() {
        Log.d(TAG, "Removing consent ");
        if (consent != null) {
            consent.resetConsentInformation();
        }
    }

    /* Banner
     * ********************************************************************** */

    /**
     * Load a banner
     *
     * @param id      AdMod Banner ID
     * @param isOnTop To made the banner top or bottom
     */
    @UsedByGodot
    public void loadBanner(final String id, final boolean isOnTop, final String bannerSize) {
        activity.runOnUiThread(() -> {
            if (banner != null) banner.remove();
            banner = new Banner(id, getAdRequest(), activity, new BannerListener() {
                @Override
                public void onBannerLoaded() {
                    emitSignal("on_admob_ad_loaded");
                }

                @Override
                public void onBannerFailedToLoad(int errorCode) {
                    emitSignal("on_admob_banner_failed_to_load", errorCode);
                }
            }, isOnTop, layout, bannerSize);
        });
    }

    /**
     * Show the banner
     */
    @UsedByGodot
    public void showBanner() {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.show();
            }
        });
    }

    /**
     * Resize the banner
     * @param isOnTop To made the banner top or bottom
     */
    @UsedByGodot
    public void move(final boolean isOnTop) {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.move(isOnTop);
            }
        });
    }

    /**
     * Resize the banner
     */
    @UsedByGodot
    public void resize() {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.resize();
            }
        });
    }


    /**
     * Hide the banner
     */
    @UsedByGodot
    public void hideBanner() {
        activity.runOnUiThread(() -> {
            if (banner != null) {
                banner.hide();
            }
        });
    }

    /**
     * Get the banner width
     *
     * @return int Banner width
     */
    @UsedByGodot
    public int getBannerWidth() {
        if (banner != null) {
            return banner.getWidth();
        }
        return 0;
    }

    /**
     * Get the banner height
     *
     * @return int Banner height
     */
    @UsedByGodot
    public int getBannerHeight() {
        if (banner != null) {
            return banner.getHeight();
        }
        return 0;
    }

    /**
     * Generate MD5 for the deviceID
     *
     * @param s The string to generate de MD5
     * @return String The MD5 generated
     */
    private String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            //Logger.logStackTrace(TAG,e);
        }
        return "";
    }

    /**
     * Get the Device ID for AdMob
     *
     * @return String Device ID
     */
    @NonNull
    private String getAdMobDeviceId() {
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = md5(android_id).toUpperCase(Locale.ENGLISH);
        return deviceId;
    }
}
