package com.simplemobiletools.calendar.pro.extensions

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

fun uploadFile(file: File, callback: (downloadUrl: String?) -> Unit) {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val fileUri = Uri.fromFile(file)
    val fileRef: StorageReference = storageRef.child("uploads/${file.name}")

    val uploadTask = fileRef.putFile(fileUri)
    uploadTask.addOnSuccessListener {
        // File uploaded successfully
        fileRef.downloadUrl.addOnSuccessListener { uri ->
            // Get the download URL and store it in Firestore
            saveFileUrlToFirestore(uri.toString())
            callback(uri.toString())
        }.addOnFailureListener {
            it.printStackTrace()
            callback(null)
        }
    }.addOnFailureListener {
        // Handle unsuccessful uploads
        it.printStackTrace()
        callback(null)
    }
}

fun saveFileUrlToFirestore(downloadUrl: String) {
    val db = FirebaseFirestore.getInstance()
    val fileData = hashMapOf(
        "fileUrl" to downloadUrl,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("files").add(fileData)
        .addOnSuccessListener {
            // File URL successfully saved to Firestore
        }
        .addOnFailureListener {
            // Handle the error
            it.printStackTrace()
        }
}
