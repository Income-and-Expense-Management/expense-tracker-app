package com.ptithcm.quanlichitieu.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ptithcm.quanlichitieu.data.repository.AuthService;
import com.ptithcm.quanlichitieu.data.repository.MockAuthService;

public class LoginViewModel extends ViewModel {

    public enum State { IDLE, LOADING, SUCCESS, ERROR }

    public static class LoginState {
        private final State state;
        private final String data;

        private LoginState(State state, String data) {
            this.state = state;
            this.data = data;
        }

        public static LoginState idle() { return new LoginState(State.IDLE, null); }
        public static LoginState loading() { return new LoginState(State.LOADING, null); }
        public static LoginState success(String username) { return new LoginState(State.SUCCESS, username); }
        public static LoginState error(String message) { return new LoginState(State.ERROR, message); }

        public State getState() { return state; }
        public String getData() { return data; }
    }

    private final AuthService authService;
    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>(LoginState.idle());

    public LoginViewModel() {
        this.authService = MockAuthService.getInstance();
    }

    public LiveData<LoginState> getLoginState() {
        return loginState;
    }

    public void login(String email, String password) {
        loginState.setValue(LoginState.loading());
        authService.login(email, password, new AuthService.LoginCallback() {
            @Override
            public void onSuccess(String username) {
                loginState.postValue(LoginState.success(username));
            }

            @Override
            public void onError(String message) {
                loginState.postValue(LoginState.error(message));
            }
        });
    }
}
