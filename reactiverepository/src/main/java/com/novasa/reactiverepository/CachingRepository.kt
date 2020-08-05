package com.novasa.reactiverepository

import com.novasa.reactiverepository.Repository.Data
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

abstract class CachingRepository<TKey, TData>(final override val key: TKey) : Repository<TKey, TData> {

    final override val value: Data<TKey, TData>
        get() = subject.value!! // This is never null

    private val subject = BehaviorSubject.createDefault<Data<TKey, TData>>(Data.empty(key))
    private var updateDisposable: Disposable? = null

    var invalidationDelay = 0L
    private var invalidateDisposable: Disposable? = null

    override fun observe(): Observable<Data<TKey, TData>> = subject

    override fun get(): Single<Data<TKey, TData>> = when {
        value.isSuccess() ||
        updateDisposable != null -> nextValue()
        else -> update()
    }

    override fun update(): Single<Data<TKey, TData>> {
        if (disposed) {
            throw IllegalStateException("Tried to update disposed repository")
        }

        updateDisposable?.dispose()

        updateDisposable = refresh()
            .map { Data.success(key, it) }
            .onErrorReturn { Data.failure(key, it) }
            .doOnSubscribe { subject.onNext(Data.loading(key)) }
            .doOnSuccess { subject.onNext(it) }
            .doOnSuccess { invalidateDelayed(invalidationDelay) }
            .doFinally { updateDisposable = null }
            .subscribe()

        return nextValue()
    }

    private fun nextValue(): Single<Data<TKey, TData>> = subject.filter { it.isSuccess() || it.isFailed() }.firstOrError()

    fun invalidateDelayed(delay: Long) {
        invalidateDisposable?.dispose()
        invalidateDisposable = null

        if (delay > 0) {
            invalidateDisposable = Completable.timer(delay, TimeUnit.MILLISECONDS)
                .andThen(clear())
                .subscribe()
        }
    }

    override fun clear(): Completable = Completable.fromAction {
        updateDisposable?.dispose()
        updateDisposable = null

        invalidateDisposable?.dispose()
        invalidateDisposable = null

        if (value.state != Repository.State.EMPTY) {
            subject.onNext(Data.empty(key))
        }
    }

    override fun set(data: TData) {
        subject.onNext(Data.success(key, data))
    }

    protected abstract fun refresh(): Single<TData>

    private var disposed = false

    override fun isDisposed(): Boolean = disposed

    override fun dispose() {
        updateDisposable?.dispose()
        updateDisposable = null

        invalidateDisposable?.dispose()
        invalidateDisposable = null

        subject.onComplete()
        disposed = true
    }
}
