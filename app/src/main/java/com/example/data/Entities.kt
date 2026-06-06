package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_config")
data class VaultConfig(
    @PrimaryKey val id: Int = 1,
    val pinHash: String,
    val isSetup: Boolean = false,
    val securityQuestion: String = "",
    val securityAnswerHash: String = ""
)

@Entity(tableName = "secret_notes")
data class SecretNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "secret_contacts")
data class SecretContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "secret_credentials")
data class SecretCredential(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val siteName: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "secret_media")
data class SecretMedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val localPath: String,
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis()
)
