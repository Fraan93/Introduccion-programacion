package com.wallcraft4k.app.data.remote

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.wallcraft4k.app.data.model.Wallpaper
import com.wallcraft4k.app.data.model.WallpaperSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Shared community backend on Firebase. Images live in Cloud Storage under
 * `wallpapers/{id}.jpg`; their metadata lives in the Firestore collection
 * `wallpapers`. Everyone reads the same feed, so uploads are visible to all users.
 *
 * This class is only instantiated when Firebase is configured (see [isAvailable]),
 * which happens automatically once a valid `google-services.json` is added.
 */
class FirebaseWallpaperSource {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val collection get() = db.collection(COLLECTION)

    /** Live feed of community wallpapers, newest first. */
    fun communityFeed(): Flow<List<Wallpaper>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    val fullUrl = doc.getString("fullUrl") ?: return@mapNotNull null
                    Wallpaper(
                        id = doc.id,
                        title = doc.getString("title") ?: "Sin título",
                        author = doc.getString("author") ?: "Anónimo",
                        category = doc.getString("category") ?: "Comunidad",
                        thumbUrl = doc.getString("thumbUrl") ?: fullUrl,
                        fullUrl = fullUrl,
                        source = WallpaperSource.UPLOAD
                    )
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Uploads an image to Storage and registers its metadata in Firestore. */
    suspend fun upload(
        source: Uri,
        title: String,
        author: String,
        category: String
    ): Wallpaper {
        val id = "up_${UUID.randomUUID()}"
        val ref = storage.reference.child("$STORAGE_DIR/$id.jpg")
        ref.putFile(source).await()
        val url = ref.downloadUrl.await().toString()

        val cleanTitle = title.ifBlank { "Sin título" }
        val cleanAuthor = author.ifBlank { "Anónimo" }
        val cleanCategory = category.ifBlank { "Comunidad" }

        val data = hashMapOf(
            "title" to cleanTitle,
            "author" to cleanAuthor,
            "category" to cleanCategory,
            "thumbUrl" to url,
            "fullUrl" to url,
            "createdAt" to FieldValue.serverTimestamp()
        )
        collection.document(id).set(data).await()

        return Wallpaper(
            id = id,
            title = cleanTitle,
            author = cleanAuthor,
            category = cleanCategory,
            thumbUrl = url,
            fullUrl = url,
            source = WallpaperSource.UPLOAD
        )
    }

    /** Removes a community wallpaper (subject to your Firestore/Storage rules). */
    suspend fun delete(id: String) {
        runCatching { storage.reference.child("$STORAGE_DIR/$id.jpg").delete().await() }
        runCatching { collection.document(id).delete().await() }
    }

    companion object {
        private const val COLLECTION = "wallpapers"
        private const val STORAGE_DIR = "wallpapers"

        /**
         * True when a default [FirebaseApp] has been initialised — i.e. a valid
         * `google-services.json` is present. When false, the app runs fully local.
         */
        fun isAvailable(): Boolean = try {
            FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
}
