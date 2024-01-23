package com.runn.flappytoonshelper;

import static androidx.core.app.ActivityCompat.startIntentSenderForResult;
import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdkNotInitializedException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
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
import com.google.firebase.messaging.FirebaseMessaging;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @NonNull
    @Override
    public String getPluginName() {
        return "Helper";
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signalInfo = new ArraySet<>();
        // Firebase
        signalInfo.add(new SignalInfo("on_server_update", String.class, Boolean.class)); // boolean is_error occurs
        signalInfo.add(new SignalInfo("on_completed"));
        signalInfo.add(new SignalInfo("on_get_rtdb", Dictionary.class));

        // Leaderboard
        signalInfo.add(new SignalInfo("on_boardData", Dictionary.class));
        signalInfo.add(new SignalInfo("on_getScore", Integer.class));
        return signalInfo;
    }

    @UsedByGodot
    public void init(boolean isFirstTime, String Host) {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        // Logging and sending the crashes to server
        firebaseCrashlytics = FirebaseCrashlytics.getInstance();
        firebaseCrashlytics.setCrashlyticsCollectionEnabled(true);
        if (firebaseCrashlytics.didCrashOnPreviousExecution()) {
            firebaseCrashlytics.checkForUnsentReports();
            firebaseCrashlytics.sendUnsentReports();
        } else {
            firebaseCrashlytics.deleteUnsentReports();
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        sendCrashes("FCM", "Fetching FCM registration token failed", Objects.requireNonNull(task.getException()).toString());
                        return;
                    }
                    token = task.getResult();
                    Log.d(TAG, token);
                });

        if (!Objects.equals(Host, "")){
            firebaseDatabase.useEmulator(Host, 9000);
            firestore.useEmulator(Host, 8080);
            Log.d(TAG, "FIREBASE: got host:" + Host);
        }
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
                    LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<>() {
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
                        cfgServerDB(Objects.requireNonNull(firebaseUser).getDisplayName(),
                                Objects.requireNonNull(firebaseUser.getPhotoUrl()).getPath(),
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
                        cfgServerDB(Objects.requireNonNull(firebaseUser).getDisplayName(),
                                Objects.requireNonNull(firebaseUser.getPhotoUrl()).getPath(),
                                token);
                    }
                });
    }

    @UsedByGodot
    public void loginWithAnonym(){
        emitSignal("on_server_update", "Flying toward server", false);
        firebaseAuth.signInAnonymously().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                emitSignal("on_server_update", "Server Error please try again...", true);
            } else {
                firebaseUser = firebaseAuth.getCurrentUser();
                cfgServerDB(randomIdentifier(),
                        "https://firebasestorage.googleapis.com/v0/b/flappy-toons.appspot.com/o/icon.png?alt=media&token=76a10d02-db85-41e6-9f06-76ea28b304a0",
                        token);
            }
        });
    }

    @UsedByGodot
    public void loginWithGoogle(){
        onTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(activity, beginSignInResult -> {
                    try {
                        startIntentSenderForResult(
                                activity,
                                beginSignInResult.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP,
                                null,
                                0,
                                0,
                                0,
                                null);
                    } catch (IntentSender.SendIntentException e) {
                        sendEvent("One Tap Google", e.toString());
                        emitSignal("on_server_update", "Google login failed", true);
                    }
                })
                .addOnFailureListener(activity, e -> {
                    sendEvent("One Tap Google", e.toString());
                    emitSignal("on_server_update", "Google login error", true);
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
        return "FT-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6);
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
                sendCrashes(error.getMessage(), error.getDetails(), String.valueOf(error.getCode()));
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
     *  */
    @UsedByGodot
    public void boardData(){
        Dictionary toReturn = new Dictionary();
        Query query = firestore.collection("players")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Loop through the documents in the snapshot
                int i = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    // Get the fields of the document
                    String name = document.getString("name");
                    String photo = document.getString("photo");
                    int score = Objects.requireNonNull(document.getLong("score")).intValue();
                    Dictionary pl = new Dictionary();
                    pl.put("name", name);
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
        });
}

    @UsedByGodot
    public void share(int score){
        activity.runOnUiThread(() -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            String shareBody = "Check out this game: Flappy Toons" + ". My score is: " + score + ". You can download it from: https://play.google.com/store/apps/details?id=com.runn.flappytoons";
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(activity, shareIntent, null);
        });
    }

    @UsedByGodot
    public void loginWithFB(){
        List<String> perm = new ArrayList<>();
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
}
