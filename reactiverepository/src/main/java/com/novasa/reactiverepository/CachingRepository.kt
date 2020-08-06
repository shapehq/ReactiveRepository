package com.novasa.reactiverepository

import com.novasa.reactiverepository.Repository.Data
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

abstract class CachingRepository<TKey, TValue>(final override val key: TKey) : Repository<TKey, TValue> {

    final override val data: Data<TKey, TValue>
        get() = subject.value!! // This is never null

    private val subject = BehaviorSubject.createDefault<Data<TKey, TValue>>(Data.empty(key))
    private var updateDisposable: Disposable? = null

    var invalidationDelay = 0L
    private var invalidateDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    override fun observe(): Observable<Data<TKey, TValue>> = subject

    override fun get(): Single<Data<TKey, TValue>> = when {
        data.isSuccess() || updateDisposable != null -> nextValue()
        else -> update()
    }

    override fun update(): Single<Data<TKey, TValue>> {
        if (isDisposed) {
            throw IllegalStateException("Tried to update disposed repository")
        }

        updateDisposable?.dispose()
        updateDisposable = refreshInternal()
            .doFinally { updateDisposable = null }
            .subscribe()

        return nextValue()
    }

    override fun periodicUpdates(period: Long, initialDelay: Long): Observable<Data<TKey, TValue>> {
        if (isDisposed) {
            throw IllegalStateException("Tried to update disposed repository")
        }

        return Observable.interval(initialDelay, period, TimeUnit.MILLISECONDS)
            .flatMapSingle { refreshInternal() }
            .doOnSubscribe { d -> disposables.add(d) }
    }

    private fun refreshInternal(): Single<Data<TKey, TValue>> = refresh()
        .map { Data.success(key, it) }
        .onErrorReturn { Data.failure(key, it) }
        .doOnSubscribe { d -> disposables.add(d) }
        .doOnSubscribe { subject.onNext(Data.loading(key)) }
        .doOnSuccess { subject.onNext(it) }
        .doOnSuccess { invalidateDelayed(invalidationDelay) }
        .doOnDispose { if (data.state == Repository.State.LOADING) emitEmpty() }

    private fun nextValue(): Single<Data<TKey, TValue>> = subject.filter { it.isSuccess() || it.isFailed() }
        .flatMap { if (it.isFailed()) Observable.error(it.error) else Observable.just(it) }
        .firstOrError()

    private fun emitEmpty() {
        if (!isDisposed) {
            subject.onNext(Data.empty(key))
        }
    }

    fun invalidateDelayed(delay: Long) {
        invalidateDisposable?.dispose()
        invalidateDisposable = null

        if (delay > 0) {
            invalidateDisposable = Completable.timer(delay, TimeUnit.MILLISECONDS)
                .andThen(clear())
                .doOnSubscribe { d -> disposables.add(d) }
                .subscribe()
        }
    }

    override fun clear(): Completable = Completable.fromAction {
        disposables.clear()
        if (data.state != Repository.State.EMPTY) {
            emitEmpty()
        }
    }

    override fun set(value: TValue) {
        subject.onNext(Data.success(key, value))
    }

    protected abstract fun refresh(): Single<TValue>

    override fun isDisposed(): Boolean = disposables.isDisposed

    override fun dispose() {
        subject.onComplete()
        disposables.dispose()
    }
}
