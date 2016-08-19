package com.nishitadutta.brainapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.nishitadutta.brainapp.Global.Constants;
import com.nishitadutta.brainapp.R;
import com.nishitadutta.brainapp.Utils.SharedPreferenceUtils;

import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Nishita on 18-08-2016.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mFireBaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 1;
    GoogleApiClient mGoogleApiClient;

    @BindView(R.id.btn_facebook_login)
    LoginButton btnFacebookLogin;

    @BindView(R.id.btn_google_login)
    com.google.android.gms.common.SignInButton btnGoogleLogin;

    //Facebook
    static LoginManager loginManager;
    CallbackManager callbackManager;

    @OnClick(R.id.btn_google_login)
    public void googleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Constants.FIREBASE_WEB_CLIENT_ID)
                .requestEmail()
                .build();

        //get firebase auth
        mFireBaseAuth = FirebaseAuth.getInstance();


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    // User signed in. Now, You can get UserId by calling firebaseUser.getUid() and save it in preference
                } else {
                    // User signed out. Now, You can clear the preference.
                }
            }
        };


        //Facebook button callback register
        loginManager = LoginManager.getInstance();
        callbackManager = CallbackManager.Factory.create();
        btnFacebookLogin=(LoginButton)findViewById(R.id.btn_facebook_login);
        btnFacebookLogin.setReadPermissions("email", "public_profile");
        btnFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e("facebook login", "success");
                GraphRequest graphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        String name = object.optString("name");
                        String facebookUserId = object.optString("id");
                        if (!TextUtils.isEmpty(name)) {
                            SharedPreferenceUtils.getInstance(LoginActivity.this).setValue(Constants.USERNAME, name);
                        }
                        SharedPreferenceUtils.getInstance(LoginActivity.this).setValue(Constants.FACEBOOK_ID, facebookUserId);
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();

                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Facebook Sign in failed. Please try again", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFireBaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            // Handle authentication failure
                        } else {
                            // Do whatever you want on successful authentication
                        }
                    }
                });
    }

    private void handleFacebookAccessToken(final AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mFireBaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {
                            // Handle authentication failure
                        } else {
                            // Do whatever you want on successful authentication
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);



        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                Log.d("google sign in", "successful");
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Log.e("google sign in", "failed");
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        mFireBaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mFireBaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }


}