package com.novasa.reactiverepository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable

interface Repository<TKey, TData> : Disposable {

    val key: TKey
    val value: Data<TKey, TData>

    fun observe(): Observable<Data<TKey, TData>>

    fun get(): Single<Data<TKey, TData>>

    fun update(): Single<Data<TKey, TData>>

    fun clear(): Completable

    fun set(data: TData)

    enum class Status {
        EMPTY,
        LOADING,
        SUCCESS,
        FAILED
    }

    data class Data<TKey, TData>(
        val key: TKey,
        val status: Status,
        val value: TData? = null,
        val error: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) {

        internal companion object {
            fun <TKey, TData> empty(key: TKey): Data<TKey, TData> = Data(key, Status.EMPTY)
            fun <TKey, TData> loading(key: TKey): Data<TKey, TData> = Data(key, Status.LOADING)
            fun <TKey, TData> success(key: TKey, data: TData): Data<TKey, TData> = Data(key, Status.SUCCESS, value = data)
            fun <TKey, TData> failure(key: TKey, error: Throwable): Data<TKey, TData> = Data(key, Status.FAILED, error = error)
        }

        fun isSuccess() = status == Status.SUCCESS
        fun isFailed() = status == Status.FAILED
    }
}
