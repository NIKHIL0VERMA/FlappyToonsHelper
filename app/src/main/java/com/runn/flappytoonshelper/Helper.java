package com.runn.flappytoonshelper;

import static android.app.Activity.RESULT_OK;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
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

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Nikhil Verma.
 * RUNN is owner of the com.runn.flappytoons under Project Flappy Toons.
 * Copyright (c) 2020 RUNN.
 * Don't use the project or it's code without any legal permission.
 * For getting permission to use any part of code.
 * You may contact on nikhil2003verma@gmail.com
 **/

public class Helper extends GodotPlugin {
    private final String TAG = "Helper";
    private final Godot godot;
    public static String token = "";

    private GoogleSignInClient googleSignInClient;
    private boolean valid = false; // Used it to mark the try with google client
    private final ActivityResultLauncher<Intent> activityResultLauncher = getGodot().registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK && valid){
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try{
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    handleGoogleAccessToken(signInAccount.getIdToken());
                } catch (ApiException e) {
                    firebaseCrashlytics.recordException(e);
                    sendEvent("GOOGLE", "Google login api exception");
                    emitSignal("on_server_update", "Google Auth error", true);
                }
            }
        }
    });

    // For Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firestore;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseCrashlytics firebaseCrashlytics;

    private boolean isNewUser = false;

    public Helper(Godot godot) {
        super(godot);
        this.godot = godot;
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
                "share",
                "sendEvent",
                "isLoggedIn"
        );
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signalInfo = new ArraySet<>();
        // Firebase
        signalInfo.add(new SignalInfo("on_server_update", String.class, Boolean.class)); // boolean is_error occurs
        signalInfo.add(new SignalInfo("on_completed", String.class, Boolean.class));
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
        godot.runOnUiThread(() -> {
            try {
                // Configuring if not login or first time opened app
                if (isFirstTime || !isLoggedIn()) {

                    // Hashing to prevent replay Attack
                    String rawNonce = UUID.randomUUID().toString();
                    byte[] bytes = rawNonce.getBytes();
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] digest = md.digest(bytes);
                    String hashedNonce = Arrays.toString(digest);
                    GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("1017278365273-qs4v9kr64not3aot1242ekajic088hv0.apps.googleusercontent.com")
                            .requestEmail()
                            .build();
                    googleSignInClient = GoogleSignIn.getClient(godot.requireActivity(), googleSignInOptions);
                }
            } catch (Exception e) {
                firebaseCrashlytics.recordException(e);
            }
        });
    }

    private void handleGoogleAccessToken(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(godot.requireActivity(), task -> {
                    if (!task.isSuccessful()) {
                        emitSignal("on_server_update", "Server Error please try again...", true);
                    } else {
                        isNewUser = Objects.requireNonNull(task.getResult().getAdditionalUserInfo()).isNewUser();
                        firebaseUser = firebaseAuth.getCurrentUser();
                        cfgServerDB(Objects.requireNonNull(firebaseUser).getDisplayName(),
                                "google/" + Objects.requireNonNull(firebaseUser.getPhotoUrl()).getPath(),
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
                isNewUser = Objects.requireNonNull(task.getResult().getAdditionalUserInfo()).isNewUser();
                firebaseUser = firebaseAuth.getCurrentUser();
                cfgServerDB(randomIdentifier(),
                        "",
                        token);
            }
        });
    }

    @UsedByGodot
    public void loginWithGoogle(){
        valid = true;
        Intent intent = googleSignInClient.getSignInIntent();
        activityResultLauncher.launch(intent);
        emitSignal("on_server_update", "Sending request to google", false);
    }

    private void cfgServerDB(String name, String photoURL, String msgToken){
        // For leaderboard
        Map<String, Object> player = new HashMap<>();
        player.put("name", name);
        player.put("photo", photoURL);
        if (isNewUser) {
            player.put("score", 0);
            firestore.collection("players")
                    .document(firebaseUser.getUid())
                    .set(player);
        }else{
            firestore.collection("players")
                    .document(firebaseUser.getUid())
                    .update(player);
        }

        // For identity and msg token
        Map<String, String> map = new HashMap<>();
        map.put(name, token);
        firestore.collection("msgToken")
                .document(firebaseUser.getUid())
                .set(map);
        emitSignal("on_completed", name, isNewUser);
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
        if (isLoggedIn() && !firebaseUser.isAnonymous()) {
            Dictionary db = new Dictionary();
            firebaseDatabase.getReference().child(firebaseUser.getUid()).child(key).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot snap :
                            snapshot.getChildren()) {
                            db.put(snap.getKey(), snap.getValue());
                    }
                    emitSignal("on_get_rtdb", db);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    sendCrashes(error.getMessage(), error.getDetails(), String.valueOf(error.getCode()));
                    sendEvent("Error getting " + key, error.toString());
                }
            });
        }
    }

    /**
     * Saving the data to RTDB
     * Use it to store unlocked things
     * Backup and user management
     */
    @UsedByGodot
    public void saveToRTDB(String key, Dictionary db){
        if (isLoggedIn() && !firebaseUser.isAnonymous()) {
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
                    assert photo != null;
                    if (photo.contains("google/")){
                        photo = photo.replace("google/", "https://lh3.googleusercontent.com");
                    }
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
    public void share(String msg){
        godot.runOnUiThread(() -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(godot.requireActivity(), shareIntent, null);
        });
    }

    @UsedByGodot
    public boolean isLoggedIn(){
        return (firebaseUser != null);
    }

    @UsedByGodot
    public void sendEvent(String key, String msg) {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(godot.requireContext());
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
     * @param requestCode the requested code for identity
     * @param resultCode the result code of request from intent
     * @param data the data of the result
     */
    @Override
    public void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        super.onMainActivityResult(requestCode, resultCode, data);
    }
}
