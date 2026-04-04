package com.ptithcm.quanlichitieu.ui.login;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage;
import com.ptithcm.quanlichitieu.data.local.token.TokenStorage;
import com.ptithcm.quanlichitieu.data.repository.AuthRepository;
import com.ptithcm.quanlichitieu.data.repository.AuthRepositoryImpl;
import com.ptithcm.quanlichitieu.data.repository.GoogleSignInHelper;

/**
 * ViewModel managing all authentication state (Login, Register, Logout).
 * Exposes read-only LiveData for the UI to observe; keeps MutableLiveData private.
 *
 * Uses AndroidViewModel because it needs Application context to construct
 * the AuthRepositoryImpl (Volley + EncryptedSharedPreferences).
 *
 * SOLID:
 * - Single Responsibility: Only orchestrates auth UI state.
 * - Dependency Inversion: Depends on AuthRepository interface.
 * - Open/Closed: Swap the concrete repository without modifying this class.
 */
public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";

    // ---- State wrapper --------------------------------------------------------

    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    public static class AuthState {
        private final Status status;
        private final String data; // username on SUCCESS, error message on ERROR

        private AuthState(Status status, String data) {
            this.status = status;
            this.data = data;
        }

        public static AuthState idle()                 { return new AuthState(Status.IDLE, null); }
        public static AuthState loading()              { return new AuthState(Status.LOADING, null); }
        public static AuthState success(String data)   { return new AuthState(Status.SUCCESS, data); }
        public static AuthState error(String message)  { return new AuthState(Status.ERROR, message); }

        public Status getStatus() { return status; }
        public String getData()   { return data; }
    }

    // ---- Fields ---------------------------------------------------------------

    private final AuthRepository authRepository;

    private final MutableLiveData<AuthState> loginState       = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<AuthState> registerState    = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<AuthState> googleAuthState  = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<AuthState> logoutState      = new MutableLiveData<>(AuthState.idle());
    private final MutableLiveData<Boolean>   sessionExpired   = new MutableLiveData<>(false);

    // ---- Constructor ----------------------------------------------------------

    public AuthViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "AuthViewModel created");

        TokenStorage tokenStorage = EncryptedTokenStorage.getInstance(application);
        AuthRepositoryImpl repositoryImpl = new AuthRepositoryImpl(application, tokenStorage);

        // Wire 401 handler so the UI can react to forced logouts
        repositoryImpl.setSessionExpiredListener(() -> {
            Log.w(TAG, "Session expired (401) — notifying observers");
            sessionExpired.postValue(true);
        });

        this.authRepository = repositoryImpl;
    }

    // ---- Public LiveData accessors --------------------------------------------

    public LiveData<AuthState> getLoginState()      { return loginState; }
    public LiveData<AuthState> getRegisterState()  { return registerState; }
    public LiveData<AuthState> getGoogleAuthState() { return googleAuthState; }
    public LiveData<AuthState> getLogoutState()    { return logoutState; }
    public LiveData<Boolean>   getSessionExpired() { return sessionExpired; }

    public boolean isLoggedIn()      { return authRepository.isLoggedIn(); }
    public String getUserFullName()  { return authRepository.getUserFullName(); }
    public String getUserEmail()     { return authRepository.getUserEmail(); }

    // ---- Actions --------------------------------------------------------------

    public void login(String email, String password) {
        Log.d(TAG, "login: email=" + email);
        loginState.setValue(AuthState.loading());

        authRepository.login(email, password, new AuthRepository.AuthCallback<String>() {
            @Override
            public void onSuccess(String fullName) {
                Log.d(TAG, "login: SUCCESS — fullName=" + fullName);
                loginState.postValue(AuthState.success(fullName));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "login: ERROR — " + message);
                loginState.postValue(AuthState.error(message));
            }
        });
    }

    public void register(String name, String email, String password) {
        Log.d(TAG, "register: name=" + name + ", email=" + email);
        registerState.setValue(AuthState.loading());

        authRepository.register(name, email, password, new AuthRepository.AuthCallback<String>() {
            @Override
            public void onSuccess(String fullName) {
                Log.d(TAG, "register: SUCCESS — fullName=" + fullName);
                registerState.postValue(AuthState.success(fullName));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "register: ERROR — " + message);
                registerState.postValue(AuthState.error(message));
            }
        });
    }

    /**
     * Initiates the Google Sign-In flow using Credential Manager API.
     * Step 1: Retrieves Google ID token from Credential Manager.
     * Step 2: Sends the ID token to the backend for verification and JWT exchange.
     *
     * @param activity The host Activity (required by Credential Manager for UI prompts).
     */
    public void signInWithGoogle(@NonNull Activity activity) {
        Log.d(TAG, "signInWithGoogle: initiated");
        googleAuthState.setValue(AuthState.loading());

        String webClientId = activity.getString(R.string.google_web_client_id);
        Log.d(TAG, "signInWithGoogle: initiated" + webClientId);
        GoogleSignInHelper helper = new GoogleSignInHelper(activity, webClientId);

        helper.signIn(activity, new GoogleSignInHelper.GoogleSignInCallback() {
            @Override
            public void onSuccess(@NonNull String idToken, @NonNull String displayName,
                                  @NonNull String email) {
                Log.d(TAG, "signInWithGoogle: Got ID token, sending to backend");
                authRepository.loginWithGoogle(idToken, displayName, email,
                        new AuthRepository.AuthCallback<String>() {
                            @Override
                            public void onSuccess(String fullName) {
                                Log.d(TAG, "signInWithGoogle: SUCCESS — fullName=" + fullName);
                                googleAuthState.postValue(AuthState.success(fullName));
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "signInWithGoogle: Backend ERROR — " + message);
                                googleAuthState.postValue(AuthState.error(message));
                            }
                        });
            }

            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "signInWithGoogle: Credential Manager ERROR — " + message);
                googleAuthState.postValue(AuthState.error(message));
            }
        });
    }

    public void logout() {
        Log.d(TAG, "logout: initiated");
        logoutState.setValue(AuthState.loading());

        authRepository.logout(new AuthRepository.AuthCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "logout: SUCCESS");
                logoutState.postValue(AuthState.success(null));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "logout: ERROR — " + message);
                logoutState.postValue(AuthState.error(message));
            }
        });
    }
}
