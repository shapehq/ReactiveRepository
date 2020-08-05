package com.novasa.reactiverepository

import android.os.SystemClock
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

interface Repository<TKey, TData> : Disposable {

    /** A unique identifier. */
    val key: TKey

    /** The current value in this repository. */
    val value: Data<TKey, TData>

    /**
     * Observe this repository.
     * This should receive all updates to its internal state, including [State.LOADING] and [State.EMPTY].
     */
    fun observe(): Observable<Data<TKey, TData>>

    /**
     * Get the data. This should only update if the repository state is currently empty or failed.
     * It should only publish [State.SUCCESS] and [State.FAILED] states.
     */
    fun get(): Single<Data<TKey, TData>>

    /**
     * Update the data. This should always start a new update.
     * It should only publish [State.SUCCESS] and [State.FAILED] states.
     */
    fun update(): Single<Data<TKey, TData>>

    /** Clear any cached data or in progress updates. */
    fun clear(): Completable

    /** Replace the current data in the repository. */
    fun set(data: TData)

    enum class State {
        EMPTY,
        LOADING,
        SUCCESS,
        FAILED
    }

    data class Data<TKey, TData>(
        val key: TKey,
        val state: State,
        val value: TData? = null,
        val error: Throwable? = null,
        val timestamp: Long = SystemClock.elapsedRealtime()
    ) {

        internal companion object {
            fun <TKey, TData> empty(key: TKey): Data<TKey, TData> = Data(key, State.EMPTY)
            fun <TKey, TData> loading(key: TKey): Data<TKey, TData> = Data(key, State.LOADING)
            fun <TKey, TData> success(key: TKey, data: TData): Data<TKey, TData> = Data(key, State.SUCCESS, value = data)
            fun <TKey, TData> failure(key: TKey, error: Throwable): Data<TKey, TData> = Data(key, State.FAILED, error = error)
        }

        /** The age of this data in milliseconds, as measured by [SystemClock.elapsedRealtime]. */
        val age: Long
            get() = SystemClock.elapsedRealtime() - timestamp

        fun isSuccess() = state == State.SUCCESS
        fun isFailed() = state == State.FAILED
    }
}
