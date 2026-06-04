package com.nuevoso.launcher.test

import com.nuevoso.launcher.data.credentials.ApiKeyReadResult
import com.nuevoso.launcher.data.credentials.CredentialFailureCode
import com.nuevoso.launcher.data.credentials.CredentialOperationResult
import com.nuevoso.launcher.data.credentials.CredentialRepository

class FakeCredentialRepository : CredentialRepository {
    val apiKeys = mutableMapOf<String, String>()
    val writtenKeys = mutableListOf<Pair<String, String>>()
    var readFailureCode: CredentialFailureCode? = null
    var writeFailureCode: CredentialFailureCode? = null
    var clearFailureCode: CredentialFailureCode? = null

    override suspend fun readApiKey(providerId: String): ApiKeyReadResult {
        readFailureCode?.let { return ApiKeyReadResult.Failure(it) }
        val key = apiKeys[providerId]
        return if (key.isNullOrBlank()) ApiKeyReadResult.Missing else ApiKeyReadResult.Available(key)
    }

    override suspend fun saveApiKey(providerId: String, apiKey: String): CredentialOperationResult {
        writeFailureCode?.let { return CredentialOperationResult.Failure(it) }
        writtenKeys += providerId to apiKey
        if (apiKey.isBlank()) {
            apiKeys.remove(providerId)
        } else {
            apiKeys[providerId] = apiKey
        }
        return CredentialOperationResult.Success
    }

    override suspend fun clearApiKey(providerId: String): CredentialOperationResult {
        clearFailureCode?.let { return CredentialOperationResult.Failure(it) }
        apiKeys.remove(providerId)
        return CredentialOperationResult.Success
    }
}
