package com.novasa.reactiverepository

import android.os.SystemClock
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

interface Repository<TKey, TValue> : Disposable {

    /** A unique identifier. */
    val key: TKey

    /** The current data in this repository. */
    val data: Data<TKey, TValue>

    /**
     * Observe this repository.
     * This should receive all updates to its internal state, including [State.LOADING] and [State.EMPTY].
     */
    fun observe(): Observable<Data<TKey, TValue>>

    /**
     * Get the data. This should only update if the repository state is currently empty or failed.
     * It should only publish [State.SUCCESS] and [State.FAILED] states.
     */
    fun get(): Single<Data<TKey, TValue>>

    /**
     * Update the data. This should always start a new update.
     * It should only publish [State.SUCCESS] and [State.FAILED] states.
     */
    fun update(): Single<Data<TKey, TValue>>

    /** Clear any cached data or in progress updates. */
    fun clear(): Completable

    /** Replace the current data in the repository. */
    fun set(value: TValue)

    enum class State {
        EMPTY,
        LOADING,
        SUCCESS,
        FAILED
    }

    data class Data<TKey, TValue>(
        val key: TKey,
        val state: State,
        val value: TValue? = null,
        val error: Throwable? = null,
        val timestamp: Long = SystemClock.elapsedRealtime()
    ) {

        internal companion object {
            fun <TKey, TValue> empty(key: TKey): Data<TKey, TValue> = Data(key, State.EMPTY)
            fun <TKey, TValue> loading(key: TKey): Data<TKey, TValue> = Data(key, State.LOADING)
            fun <TKey, TValue> success(key: TKey, value: TValue): Data<TKey, TValue> = Data(key, State.SUCCESS, value = value)
            fun <TKey, TValue> failure(key: TKey, error: Throwable): Data<TKey, TValue> = Data(key, State.FAILED, error = error)
        }

        /** The age of this data in milliseconds, as measured by [SystemClock.elapsedRealtime]. */
        val age: Long
            get() = SystemClock.elapsedRealtime() - timestamp

        fun isSuccess() = state == State.SUCCESS
        fun isFailed() = state == State.FAILED
    }
}
