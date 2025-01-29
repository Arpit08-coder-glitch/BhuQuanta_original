package com.arpit.project;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth.AuthStateListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity {
    FirebaseAuth auth;
    EditText emailBox, passwordBox, namebox, phoneBox;
    Button loginBtn, signupBtn, googleBtn;

    FirebaseFirestore database;
    ProgressDialog progressDialog;
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 9001;

    private AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getSupportActionBar().hide();

        database = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        progressDialog = new ProgressDialog(SignupActivity.this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("We are creating your account");

        namebox = findViewById(R.id.namebox);
        emailBox = findViewById(R.id.emailBox);
        passwordBox = findViewById(R.id.passwordBox);
        phoneBox = findViewById(R.id.phoneBox);

        loginBtn = findViewById(R.id.loginBtn);
        signupBtn = findViewById(R.id.createBtn);
        googleBtn = findViewById(R.id.google_btn);

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                String email = emailBox.getText().toString();
                String pass = passwordBox.getText().toString();
                String name = namebox.getText().toString();
                String phone = phoneBox.getText().toString();

                User user = new User();
                user.setEmail(email);
                user.setPass(pass);
                user.setName(name);
                user.setPhone(phone); // Set phone number

                auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            sendVerificationEmail(user);
                        } else {
                            Toast.makeText(SignupActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void sendVerificationEmail(User user) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        addUserToFirestore(firebaseUser.getUid(), user);
                                        Toast.makeText(SignupActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Log.e(TAG, "Failed to send verification email.", task.getException());
                                        Toast.makeText(SignupActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            }
        });

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });

        // Initialize AuthStateListener
        authStateListener = new AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null && user.isEmailVerified()) {
                    String userId = user.getUid();
                    User newUser = new User();
                    newUser.setEmail(user.getEmail());
                    newUser.setName(namebox.getText().toString());
                    newUser.setPhone(phoneBox.getText().toString());
                    newUser.setPass(passwordBox.getText().toString());
                    FirebaseUser User = auth.getCurrentUser();
                    addUserToFirestore(User);
                }
            }

            private void addUserToFirestore(FirebaseUser user) {
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }
    }

    private void googleSignIn() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuth(account.getIdToken());
                } else {
                    Toast.makeText(this, "Failed to get Google account", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                checkIfUserExistsInFirestore(user);
                            } else {
                                Toast.makeText(SignupActivity.this, "FirebaseUser is null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkIfUserExistsInFirestore(FirebaseUser firebaseUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = firebaseUser.getUid();
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // User already exists, proceed to main activity

                    finish();
                } else {
                    // User does not exist, prompt for additional details
                    collectAdditionalDetails(firebaseUser);
                }
            } else {
                Toast.makeText(SignupActivity.this, "Failed to check if user exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void collectAdditionalDetails(FirebaseUser firebaseUser) {
        // Prompt user to enter additional details (e.g., phone number, password)
        // You can use a dialog or a new activity for this
        // For simplicity, using existing fields here

        String name = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String phone = phoneBox.getText().toString();
        String password = passwordBox.getText().toString();

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPass(password);

        addUserToFirestore(firebaseUser.getUid(), user);
    }

    private void addUserToFirestore(String userId, User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    // User added successfully

                    finish();
                })
                .addOnFailureListener(e -> {
                    // Failed to add user
                    Toast.makeText(SignupActivity.this, "Failed to add user to Firestore", Toast.LENGTH_SHORT).show();
                });
    }
}
