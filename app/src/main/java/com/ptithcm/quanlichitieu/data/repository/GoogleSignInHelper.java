package com.ptithcm.quanlichitieu.data.repository;

import android.app.Activity;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;

import java.util.concurrent.Executors;

/**
 * Encapsulates Google Sign-In flow using the Credential Manager API.
 * Handles the platform-level credential retrieval and extracts the Google ID token.
 *
 * Single Responsibility: Only manages Google credential retrieval.
 * The extracted ID token is passed to {@link AuthRepository} for backend verification.
 */
public class GoogleSignInHelper {

    private static final String TAG = "GoogleSignInHelper";

    private final CredentialManager credentialManager;
    private final String webClientId;

    public interface GoogleSignInCallback {
        void onSuccess(@NonNull String idToken, @NonNull String displayName, @NonNull String email);
        void onError(@NonNull String message);
    }

    public GoogleSignInHelper(@NonNull Activity activity, @NonNull String webClientId) {
        this.credentialManager = CredentialManager.create(activity);
        this.webClientId = webClientId;
    }

    /**
     * Launches the Google Sign-In flow via Credential Manager.
     * On success, returns the Google ID token, display name, and email via callback.
     *
     * @param activity The host Activity (required by Credential Manager for UI).
     * @param callback Receives the ID token on success or an error message on failure.
     */
    public void signIn(@NonNull Activity activity, @NonNull GoogleSignInCallback callback) {
        Log.d(TAG, "signIn: Starting Google Sign-In flow");

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result, callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "signIn: Credential Manager error", e);
                        callback.onError(e.getMessage() != null
                                ? e.getMessage()
                                : "Google Sign-In failed");
                    }
                }
        );
    }

    /**
     * Clears the credential state (used during logout).
     */
    public void signOut(@NonNull Activity activity) {
        Log.d(TAG, "signOut: Clearing credential state");
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        Log.d(TAG, "signOut: Credential state cleared");
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "signOut: Failed to clear credential state", e);
                    }
                }
        );
    }

    private void handleSignInResult(@NonNull GetCredentialResponse response,
                                    @NonNull GoogleSignInCallback callback) {
        Credential credential = response.getCredential();

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {
                try {
                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(customCredential.getData());

                    String idToken = googleCredential.getIdToken();
                    String displayName = googleCredential.getDisplayName() != null
                            ? googleCredential.getDisplayName()
                            : "";
                    String email = googleCredential.getId();

                    Log.d(TAG, "handleSignInResult: Successfully extracted Google ID token"
                            + ", displayName=" + displayName
                            + ", email=" + email);

                    callback.onSuccess(idToken, displayName, email);
                } catch (Exception e) {
                    Log.e(TAG, "handleSignInResult: Failed to parse GoogleIdTokenCredential", e);
                    callback.onError("Failed to parse Google credential");
                }
            } else {
                Log.e(TAG, "handleSignInResult: Unexpected credential type: "
                        + customCredential.getType());
                callback.onError("Unexpected credential type");
            }
        } else {
            Log.e(TAG, "handleSignInResult: Credential is not a CustomCredential");
            callback.onError("Unexpected credential format");
        }
    }
}
