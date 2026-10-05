package com.zubora.taijuki.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Google Drive backup — not usable until you complete a one-time setup in Google Cloud
 * Console, because Drive access requires an OAuth client tied to your own project:
 *
 *   1. console.cloud.google.com → create/select a project → enable the "Google Drive API"
 *      (APIs & Services > Library).
 *   2. APIs & Services > OAuth consent screen: configure it, and while the app is in
 *      "Testing" mode, add your own Google account under "Test users" (otherwise sign-in
 *      is rejected with an access-denied screen).
 *   3. APIs & Services > Credentials > Create Credentials > OAuth client ID:
 *        - Application type: Android
 *        - Package name: com.zubora.taijuki
 *        - SHA-1: from `./gradlew signingReport` (debug cert for local testing).
 *      No client ID or secret needs to be pasted into this app — Android-type OAuth
 *      clients are matched automatically by package name + signing certificate.
 *   4. Reinstall the app. No code changes needed after step 3.
 *
 * Uses the drive.file scope, so the signed-in account only grants access to files this
 * app creates — not the user's whole Drive.
 */
object DriveBackup {
    private val scope = Scope(DriveScopes.DRIVE_FILE)
    private const val FOLDER_NAME = "ズボラ体重記"

    fun signInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // GoogleSignInAccount.getAccount() needs this to return a non-null Account
            .requestScopes(scope)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** Returns a previously-granted account, or null if the user still needs to (re)sign in. */
    fun lastSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)
            ?.takeIf { GoogleSignIn.hasPermissions(it, scope) && it.account != null }

    /** Uploads [csv] as a new timestamped file. Blocking network call — invoke from Dispatchers.IO. */
    fun upload(context: Context, account: GoogleSignInAccount, csv: String) {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE)).apply {
            selectedAccount = account.account
        }
        val drive = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("ズボラ体重記")
            .build()

        val folderId = findOrCreateFolder(drive)
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
        val metadata = DriveFile().setName("zubora_taijuki_backup_$stamp.csv").setParents(listOf(folderId))
        val content = ByteArrayContent("text/csv", csv.toByteArray())
        drive.files().create(metadata, content).execute()
    }

    /**
     * drive.file scope only sees files/folders this app created, so a folder the user made
     * by hand in the Drive UI is invisible to this query — this reuses (or creates once) a
     * folder the app owns instead.
     */
    private fun findOrCreateFolder(drive: Drive): String {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$FOLDER_NAME' and trashed=false"
        val existing = drive.files().list().setQ(query).setSpaces("drive").execute().files.firstOrNull()
        if (existing != null) return existing.id

        val folderMetadata = DriveFile().setName(FOLDER_NAME).setMimeType("application/vnd.google-apps.folder")
        return drive.files().create(folderMetadata).execute().id
    }
}
