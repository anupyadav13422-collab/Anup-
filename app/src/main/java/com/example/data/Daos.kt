package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<VaultConfig?>

    @Query("SELECT * FROM vault_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): VaultConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: VaultConfig)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM secret_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<SecretNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: SecretNote)

    @Delete
    suspend fun deleteNote(note: SecretNote)

    @Query("DELETE FROM secret_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM secret_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<SecretContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContact(contact: SecretContact)

    @Delete
    suspend fun deleteContact(contact: SecretContact)

    @Query("DELETE FROM secret_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)
}

@Dao
interface CredentialDao {
    @Query("SELECT * FROM secret_credentials ORDER BY siteName ASC")
    fun getAllCredentials(): Flow<List<SecretCredential>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCredential(credential: SecretCredential)

    @Delete
    suspend fun deleteCredential(credential: SecretCredential)

    @Query("DELETE FROM secret_credentials WHERE id = :id")
    suspend fun deleteCredentialById(id: Int)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM secret_media ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<SecretMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMedia(media: SecretMedia)

    @Delete
    suspend fun deleteMedia(media: SecretMedia)

    @Query("DELETE FROM secret_media WHERE id = :id")
    suspend fun deleteMediaById(id: Int)
}
