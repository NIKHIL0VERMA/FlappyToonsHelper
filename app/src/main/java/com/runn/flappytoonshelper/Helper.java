package com.runn.flappytoonshelper;

import static androidx.core.app.ActivityCompat.startIntentSenderForResult;
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
import com.google.firebase.messaging.FirebaseMessaging;
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
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class Helper extends GodotPlugin {
    // For FB
    private static CallbackManager callbackManager;
    // Using this for creating random user name
    final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
    final java.util.Random rand = new java.util.Random();
    final Set<String> identifiers = new HashSet<String>();
    private final String TAG = "Helper";
    private final Activity activity;
    private final int REQ_ONE_TAP = 55;
    public String token = "";
    // For Admob
    private boolean isReal = false;
    private boolean isForChildDirectedTreatment = false;
    private boolean isPersonalized = true;
    private String maxAdContentRating = "";
    private Bundle extras = null;
    private FrameLayout layout = null; // Store the layout
    private RewardedVideo rewardedVideo = null;
    private RewardedInterstitial rewardedInterstitial = null;
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

    // Getting the country using ip-api for future purpose
    public static JSONObject getJSONObjectFromURL() throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        URL url = new URL("http://ip-api.com/json");
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */ );
        urlConnection.setConnectTimeout(15000 /* milliseconds */ );
        urlConnection.setDoOutput(true);
        urlConnection.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        String jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);
        return new JSONObject(jsonString);
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
    public void onMainPause() {
        super.onMainPause();
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
                "resetConsentInformation",
                "requestConsentInfoUpdate",
                "showRewardedInterstitial",
                "loadRewardedInterstitial",
                "showRewardedVideo",
                "loadRewardedVideo",
                "sendCrashes",
                "sendEvent",
                "isLoggedIn"
        );
    }

//    TODO: manage the consent and check at every app opening

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
        // Consent info
        signalInfo.add(new SignalInfo("on_consent_info_update_success"));
        signalInfo.add(
                new SignalInfo("on_consent_info_update_failure",
                        Integer.class, String.class));
        signalInfo.add(new SignalInfo("on_app_can_request_ads", Integer.class));

        // Firebase
        signalInfo.add(new SignalInfo("on_server_update", String.class));
        signalInfo.add(new SignalInfo("on_completed"));

        // Leaderboard
        signalInfo.add(new SignalInfo("on_boardData", Dictionary.class));
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
                    // Current user's token
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "FCM getting token failed");
                            sendEvent("Firebase messaging", "Getting the token for current user failed");
                        } else {
                            token = task.getResult();
                        }
                    });
                    // Fb login Manager
                    FacebookSdk.setApplicationId("352763776016419");
                    LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            handleFacebookAccessToken(loginResult.getAccessToken());
                            emitSignal("on_server_update", "Authenticated from Facebook...");
                        }

                        @Override
                        public void onCancel() {
                            sendEvent("FB", "Fb login cancelled by the user");
                            emitSignal("on_server_update", "Cancelled Facebook Auth");
                        }

                        @Override
                        public void onError(@NonNull FacebookException e) {
                            sendEvent("FB", String.valueOf(e));
                            sendCrashes("Fb login", String.valueOf(e), "Fb error");
                            emitSignal("on_server_update", "Facebook error");
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
                        emitSignal("on_server_update", "Server Error please try again...");
                    } else {
                        cfgServerDB(firebaseUser.getDisplayName(),
                                firebaseUser.getPhotoUrl().getPath(),
                                getToken());
                    }
                });
    }

    private void handleGoogleAccessToken(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener((Executor) this, task -> {
                    if (!task.isSuccessful()) {
                        emitSignal("on_server_update", "Server Error please try again...");
                    } else {
                        cfgServerDB(firebaseUser.getDisplayName(),
                                firebaseUser.getPhotoUrl().getPath(),
                                getToken());
                    }
                });
    }

    @UsedByGodot
    public void loginWithAnonym(){
        emitSignal("on_server_update", "Flying toward server");
        firebaseAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()){
                    emitSignal("on_server_update", "Server Error please try again...");
                }else{
                    cfgServerDB(randomIdentifier(),
                            "https://firebasestorage.googleapis.com/v0/b/flappy-toons.appspot.com/o/icon.png?alt=media&token=76a10d02-db85-41e6-9f06-76ea28b304a0",
                            getToken());
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
                            emitSignal("on_server_update", "Google login failed");
                        }
                    }
                })
                .addOnFailureListener(activity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        sendEvent("One Tap Google", e.toString());
                        emitSignal("on_server_update", "Google login error");
                    }
                });
    }

    private void cfgServerDB(String name, String photoURL, String msgToken){
        // For leaderboard
        Player player = new Player();
        player.setName(name);
        player.setPhoto(photoURL);
        player.setScore(0);
        try {
            player.setCountry(getCountryCode());
        }catch (IOException exception){
            sendEvent("IP-API", exception.toString());
        }catch (JSONException jsonException){
            sendEvent("JSON Parser", jsonException.toString());
        }
        firestore.collection("players")
                .document(firebaseUser.getUid())
                .set(player);

        // For identity and msg token
        Map<String, String> map = new HashMap<>();
        map.put(player.getName(), getToken());
        firestore.collection("msgToken")
                .document(firebaseUser.getUid())
                .set(map);
        emitSignal("on_completed");
    }

    @UsedByGodot
    public int getScore(){
        AtomicInteger i = new AtomicInteger();
        firestore.collection("players")
                .document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            i.set((int) snapshot.get("score"));
                        }
                    }
                });
        return i.get();
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
        emitSignal("on_server_update", "Logged out");
    }

    private String randomIdentifier() {
        StringBuilder builder = new StringBuilder();
        builder.append("FT-");
        while(builder.toString().length() == 0) {
            int length = rand.nextInt(3)+3;
            for(int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
            }
            if(identifiers.contains(builder.toString())) {
                builder = new StringBuilder();
            }
        }
        return builder.toString();
    }

    /**
     * Saving the data to RTDB
     * Use it to store unlocked things
     * Backup and user management
     */
    @UsedByGodot
    public void saveToRTDB(String key, String[] values){
        if (firebaseAuth != null) {
            firebaseDatabase.getReference().child(firebaseUser.getUid()).child(key).setValue(Collections.singletonList(values));
        }
    }

    @UsedByGodot
    public void savePlayers(String[] unlocked){
        if (firebaseAuth != null) {
            firebaseDatabase.getReference().child(firebaseUser.getUid()).child("unlocked").setValue(Collections.singletonList(unlocked));
        }
    }

    /**
     * Getting the data from RTDB
     * use to restore unlocked players backgrounds pipes for user
     * Use this to secure user data
     * **/
    @UsedByGodot
    public String[] getFromRTDB(String key){
        List<String> mylist = new ArrayList<String>();
        firebaseDatabase.getReference().child(firebaseUser.getUid()).child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap:
                        snapshot.getChildren()) {
                    mylist.add(snap.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                sendCrashes(String.valueOf(error.getMessage()), String.valueOf(error.getDetails()), String.valueOf(error.getCode()));
                sendEvent("Error getting " + key, error.toString());
            }
        });
        return mylist.toArray(new String[0]);
    }

    /**
     * Getting the user's data to populate the leaderboard section
     *
     * @return the dictionary to godot with needed data
     */
    @UsedByGodot
    public void boardData(){
        Dictionary toReturn = new Dictionary();
        firestore.collection("players")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    firebaseCrashlytics.recordException(error);
                    if (error != null){
                        sendCrashes("loading Players error", String.valueOf(error), "Firestore error");
                        emitSignal("on_server_update", "Server Error please try again...");
                        return;
                    }
                    List<Player> players = value.toObjects(Player.class);
                    int indx = 0;
                    for (Player p: players) {
                        toReturn.put("name"+indx,p.getName());
                        toReturn.put("country"+indx, p.getCountry());
                        toReturn.put("score"+indx, p.getScore());
                        toReturn.put("photo"+indx, p.getPhoto());
                        Log.d(TAG, p.getName());
                        Log.d(TAG, toReturn.toString());
                        indx++;
                    }
                });
        emitSignal("on_boardData", toReturn);
        Log.d(TAG, toReturn.get_keys().toString());
        Log.d(TAG, toReturn.get_values().toString());
    }

    private String getCountryCode() throws JSONException, IOException {
        JSONObject jsonObject = getJSONObjectFromURL();
        return (String) jsonObject.get("country");
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

    @UsedByGodot
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
                emitSignal("on_server_update", "Google login failed");
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
                    emitSignal("on_rewarded_interstitial_ad_failed_to_show", errorCode);
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
    private String getAdMobDeviceId() {
        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceId = md5(android_id).toUpperCase(Locale.US);
        return deviceId;
    }

    /**
     * Get the FCM token
     * @return token stored by init else generate new token
     * */
    private String getToken() {
        return (!Objects.equals(token, "")) ? token: FirebaseMessaging.getInstance().getToken().getResult();
    }
}
