package com.novasa.reactiverepository

import android.os.SystemClock
import android.widget.TextView
import androidx.annotation.CheckResult
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

interface Repository<TValue : Any> : Disposable {

    /** An identifier which will be supplied to any data output */
    val id: String

    /** The current data in this repository. */
    val data: Data<TValue>

    /**
     * Observe this repository.
     * This should receive all updates to its internal state, including [State.LOADING] and [State.EMPTY].
     */
    @CheckResult
    fun observe(): Observable<Data<TValue>>

    /**
     * Get the data. This should only update if the repository state is currently empty or failed.
     * It should only publish [State.SUCCESS] and [State.FAILED] states.
     */
    @CheckResult
    fun get(): Single<Data<TValue>>

    /**
     * Update the data. This should always start a new update.
     * It should only publish [State.SUCCESS] and [State.FAILED] states.
     */
    @CheckResult
    fun update(): Single<Data<TValue>>

    /**
     * Start periodic [update].
     */
    @CheckResult
    fun periodicUpdates(period: Long, initialDelay: Long): Observable<Data<TValue>>

    /** Push an update with the current value to observers. */
    fun push()

    /** Clear any cached data or in progress updates. */
    @CheckResult
    fun clear(): Completable

    /** Replace the current data in the repository. */
    fun set(value: TValue)

    enum class State {
        EMPTY,
        LOADING,
        SUCCESS,
        FAILED
    }

    enum class Source {
        UNDEFINED,
        REMOTE,
        PERSISTENCE
    }

    data class Data<TValue>(
        val id: String,
        val state: State,
        val value: TValue? = null,
        val error: Throwable? = null,
        val source: Source = Source.UNDEFINED,
        val timestamp: Long = System.currentTimeMillis()
    ) {

        internal companion object {
            fun <TValue> empty(id: String): Data<TValue> = Data(id, State.EMPTY)
            fun <TValue> loading(id: String): Data<TValue> = Data(id, State.LOADING)
            fun <TValue> success(id: String, value: TValue, source: Source, timestamp: Long): Data<TValue> = Data(id, State.SUCCESS, value = value, source = source, timestamp = timestamp)
            fun <TValue> failure(id: String, error: Throwable): Data<TValue> = Data(id, State.FAILED, error = error)
        }

        /** The age of this data in milliseconds, as measured by [System.currentTimeMillis]. */
        val age: Long
            get() = System.currentTimeMillis() - timestamp

        fun isSuccess() = state == State.SUCCESS
        fun isFailed() = state == State.FAILED
        fun isEmpty() = state == State.EMPTY

        fun valueOrThrow() : TValue = value ?: throw RepositoryException(error)
    }
}
