package com.utilitylab.focussense.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAccountManager(private val activity: Activity) {
    private val credentialManager = CredentialManager.create(activity)

    data class GoogleProfile(
        val uniqueId: String,
        val email: String,
        val displayName: String,
        val photoUri: String?
    )

    suspend fun signIn(serverClientId: String): GoogleProfile {
        require(!serverClientId.startsWith("REPLACE_WITH")) {
            "Configura google_server_client_id prima di usare il login Google"
        }

        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result = credentialManager.getCredential(activity, request)
        val custom = result.credential as? CustomCredential
            ?: error("Credenziale Google non ricevuta")

        if (custom.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Tipo credenziale non supportato: ${custom.type}")
        }

        val google = GoogleIdTokenCredential.createFrom(custom.data)
        return GoogleProfile(
            uniqueId = google.id,
            email = google.id,
            displayName = google.displayName ?: google.givenName ?: google.id,
            photoUri = google.profilePictureUri?.toString()
        )
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
