package com.example.snake.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    companion object {
        private const val TAG = "AuthManager"
        const val GOOGLE_WEB_CLIENT_ID = "156729676614-m9nphcsvftcg3phlea8clarotuso55r7.apps.googleusercontent.com"
        const val APP_SHA1 = "96:D2:0F:9F:13:ED:F0:7E:5B:63:12:3B:E7:73:0C:2A:FF:BA:47:6B"
        const val APP_PACKAGE = "com.aistudio.snakegame.xkrpqv"
        const val FIREBASE_PROJECT_ID = "neon-snake-468d3"
    }

    private val auth: FirebaseAuth by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp initialization exception: ${e.message}")
        }
        FirebaseAuth.getInstance()
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val userFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        trySend(auth.currentUser)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    private fun findActivity(ctx: Context): Activity? {
        var current = ctx
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    /**
     * Signs in with Google:
     * 1. Attempts CredentialManager with GetSignInWithGoogleOption.
     * 2. If CredentialManager encounters NoCredentialException or configuration error,
     *    automatically falls back to Firebase OAuthProvider (Chrome Custom Tab / Web flow).
     */
    suspend fun signInWithGoogle(activityContext: Context? = null): Result<FirebaseUser> {
        val callingContext = activityContext ?: context
        val activity = findActivity(callingContext)
        val targetContext: Context = activity ?: callingContext

        // Step 1: Attempt Credential Manager
        try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = GOOGLE_WEB_CLIENT_ID)
                .build()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = targetContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                if (user != null) {
                    return Result.success(user)
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Google Sign-In cancelled by user")
            return Result.failure(Exception("تم إلغاء عملية تسجيل الدخول."))
        } catch (e: Exception) {
            Log.w(TAG, "CredentialManager flow failed: ${e.message}. Attempting Firebase OAuth fallback...")
        }

        // Step 2: Fallback to Firebase OAuth Provider if Activity is available
        if (activity != null) {
            try {
                val provider = OAuthProvider.newBuilder("google.com")
                val pending = auth.pendingAuthResult
                val authResult = if (pending != null) {
                    pending.await()
                } else {
                    auth.startActivityForSignInWithProvider(activity, provider.build()).await()
                }
                val user = authResult.user
                if (user != null) {
                    return Result.success(user)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase OAuth fallback failed: ${e.message}", e)
                val isCancelled = e.message?.contains("canceled", ignoreCase = true) == true ||
                                  e.message?.contains("cancelled", ignoreCase = true) == true
                if (isCancelled) {
                    return Result.failure(Exception("تم إلغاء عملية تسجيل الدخول."))
                }
            }
        }

        return Result.failure(
            Exception("تعذر تسجيل الدخول عبر Google. يمكنك استخدام الدخول السحابي السريع أو إضافة بصمة SHA-1 في Firebase Console.")
        )
    }

    /**
     * Quick Cloud Sign-In:
     * Signs in anonymously with Firebase Auth, creating a persistent Firebase UID
     * with full Cloud Firestore read/write sync capabilities.
     */
    suspend fun signInQuickCloud(playerName: String = "Neon Runner"): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                try {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(playerName)
                        .build()
                    user.updateProfile(profileUpdate).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set display name: ${e.message}")
                }
                Result.success(user)
            } else {
                Result.failure(Exception("تعذر إنشاء حساب سحابي."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Quick cloud sign-in error: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "فشل تسجيل الدخول السحابي السريع."))
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
