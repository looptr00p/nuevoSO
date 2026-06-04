package com.nuevoso.launcher.data.credentials

sealed class ApiKeyReadResult {
    data class Available(val apiKey: String) : ApiKeyReadResult()
    data object Missing : ApiKeyReadResult()
    data class Failure(val code: CredentialFailureCode) : ApiKeyReadResult()
}

sealed class CredentialOperationResult {
    data object Success : CredentialOperationResult()
    data class Failure(val code: CredentialFailureCode) : CredentialOperationResult()
}

enum class CredentialFailureCode {
    SECURE_STORAGE_UNAVAILABLE,
    READ_FAILED,
    WRITE_FAILED,
    DELETE_FAILED,
}

interface CredentialRepository {
    suspend fun readApiKey(providerId: String): ApiKeyReadResult
    suspend fun saveApiKey(providerId: String, apiKey: String): CredentialOperationResult
    suspend fun clearApiKey(providerId: String): CredentialOperationResult
}
