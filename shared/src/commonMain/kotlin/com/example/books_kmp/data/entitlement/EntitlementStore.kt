package com.example.books_kmp.data.entitlement

import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.auth.AuthRepository
import com.example.books_kmp.domain.auth.AuthSessionState
import com.example.books_kmp.domain.entitlement.EntitlementError
import com.example.books_kmp.domain.entitlement.EntitlementRepository
import com.example.books_kmp.domain.entitlement.EntitlementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EntitlementStore(
    private val repository: EntitlementRepository,
    private val authRepository: AuthRepository,
    scope: CoroutineScope,
) : EntitlementState {
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val currentUserId = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            authRepository.sessionStatus.collect { status ->
                when (status) {
                    is AuthSessionState.Authenticated -> {
                        currentUserId.value = status.userId
                        _isPremium.value =
                            when (val result = repository.fetchIsPremium(status.userId)) {
                                is Result.Success -> result.data
                                is Result.Failure -> false // conservative; see decisions.md
                            }
                    }
                    else -> {
                        currentUserId.value = null
                        _isPremium.value = false
                    }
                }
            }
        }
    }

    override suspend fun setPremiumStatus(premium: Boolean): Result<Unit, EntitlementError> {
        val userId = currentUserId.value ?: return Result.Failure(EntitlementError.NotAuthenticated)
        return when (val result = repository.setPremiumStatus(userId, premium)) {
            is Result.Success -> {
                _isPremium.value = premium
                result
            }
            is Result.Failure -> result
        }
    }
}
