package com.levibostian.teller.cachestate.online.statemachine

import com.levibostian.teller.repository.OnlineRepositoryCache


/**
 * Immutable state machine for the phase of response's lifecycle when cached response exists.
 */
internal class CacheStateMachine<CACHE: OnlineRepositoryCache> private constructor(val state: State, val cache: CACHE?) {

    companion object {
        fun <CACHE: OnlineRepositoryCache> cacheEmpty(): CacheStateMachine<CACHE> = CacheStateMachine(State.CACHE_EMPTY, null)
        fun <CACHE: OnlineRepositoryCache> cacheExists(cache: CACHE): CacheStateMachine<CACHE> = CacheStateMachine(State.CACHE_NOT_EMPTY, cache)
    }

    internal enum class State {
        CACHE_EMPTY,
        CACHE_NOT_EMPTY
    }

    override fun toString(): String {
        return when (state) {
            State.CACHE_EMPTY -> "Cache response exists and is empty."
            State.CACHE_NOT_EMPTY -> "Cache response exists and is not empty (cache value: ${cache.toString()})."
        }
    }

}