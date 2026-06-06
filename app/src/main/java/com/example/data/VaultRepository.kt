package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val db: VaultDatabase) {
    val vaultConfig: Flow<VaultConfig?> = db.vaultDao().getConfig()
    val allNotes: Flow<List<SecretNote>> = db.noteDao().getAllNotes()
    val allContacts: Flow<List<SecretContact>> = db.contactDao().getAllContacts()
    val allCredentials: Flow<List<SecretCredential>> = db.credentialDao().getAllCredentials()
    val allMedia: Flow<List<SecretMedia>> = db.mediaDao().getAllMedia()

    suspend fun getConfigDirect(): VaultConfig? = db.vaultDao().getConfigDirect()
    suspend fun saveConfig(config: VaultConfig) = db.vaultDao().saveConfig(config)

    suspend fun saveNote(note: SecretNote) = db.noteDao().saveNote(note)
    suspend fun deleteNoteById(id: Int) = db.noteDao().deleteNoteById(id)

    suspend fun saveContact(contact: SecretContact) = db.contactDao().saveContact(contact)
    suspend fun deleteContactById(id: Int) = db.contactDao().deleteContactById(id)

    suspend fun saveCredential(credential: SecretCredential) = db.credentialDao().saveCredential(credential)
    suspend fun deleteCredentialById(id: Int) = db.credentialDao().deleteCredentialById(id)

    suspend fun saveMedia(media: SecretMedia) = db.mediaDao().saveMedia(media)
    suspend fun deleteMediaById(id: Int) = db.mediaDao().deleteMediaById(id)
}
