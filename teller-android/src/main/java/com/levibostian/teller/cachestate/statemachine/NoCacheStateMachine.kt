package com.levibostian.teller.cachestate.statemachine

/**
 * State machine for the phase of response's lifecycle when no cache exists.
 *
 * This is always the first phase all cache response will go through. You must have a fetch successful fetch to move onto any other node in the state machine.
 */
internal class NoCacheStateMachine private constructor(val state: State, val errorDuringFetch: Throwable?) {

    var isFetching: Boolean = false
        get() = state == State.IS_FETCHING
        private set

    companion object {
        fun noCacheExists(): NoCacheStateMachine = NoCacheStateMachine(State.NO_CACHE_EXISTS, null)
    }

    fun fetching(): NoCacheStateMachine = NoCacheStateMachine(State.IS_FETCHING, null)
    fun failedFetching(error: Throwable) = NoCacheStateMachine(State.NO_CACHE_EXISTS, error)

    internal enum class State {
        NO_CACHE_EXISTS,
        IS_FETCHING
    }

    override fun toString(): String {
        return when (state) {
            State.IS_FETCHING -> "Cache response does not exist, but it is being fetched for first time."
            State.NO_CACHE_EXISTS -> {
                return if (errorDuringFetch != null) "Cache response does not exist. It just got done fetching but failed with error: $errorDuringFetch."
                else "Cache response does not exist. It is not fetching response."
            }
        }
    }

}