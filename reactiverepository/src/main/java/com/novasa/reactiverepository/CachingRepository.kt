package com.novasa.reactiverepository

import com.novasa.reactiverepository.Repository.Data
import io.reactivex.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

abstract class CachingRepository<TValue : Any>(final override val id: String) : Repository<TValue> {

    data class Result<TValue>(
        val value: TValue,
        val source: Repository.Source = Repository.Source.UNDEFINED,
        val timestamp: Long = System.currentTimeMillis()
    )

    final override val data: Data<TValue>
        get() = subject.value!! // This is never null

    private val subject = BehaviorSubject.createDefault<Data<TValue>>(Data.empty(id))
    private var updateDisposable: Disposable? = null

    var invalidationDelay = 0L
    private var invalidateDisposable: Disposable? = null

    private val disposables = CompositeDisposable()

    override fun observe(): Observable<Data<TValue>> = subject

    override fun get(): Single<Data<TValue>> = when {
        data.isSuccess() || updateDisposable != null -> nextValue()
        else -> update()
    }

    override fun update(): Single<Data<TValue>> {
        if (isDisposed) {
            throw IllegalStateException("Tried to update disposed repository")
        }

        updateDisposable?.dispose()
        updateDisposable = refreshInternal()
            .doFinally { updateDisposable = null }
            .subscribe()

        return nextValue()
    }

    override fun periodicUpdates(period: Long, initialDelay: Long): Observable<Data<TValue>> {
        if (isDisposed) {
            throw IllegalStateException("Tried to update disposed repository")
        }

        return Observable.interval(initialDelay, period, TimeUnit.MILLISECONDS)
            .flatMapSingle { refreshInternal() }
            .doOnSubscribe { d -> disposables.add(d) }
    }

    private fun refreshInternal(): Single<Data<TValue>> = refresh()
        .map { Data.success(id, it.value, it.source, it.timestamp) }
        .switchIfEmpty(Single.fromCallable { Data.empty(id) })
        .onErrorReturn { Data.failure(id, it) }
        .doOnSubscribe { d -> disposables.add(d) }
        .doOnSubscribe { subject.onNext(Data.loading(id)) }
        .doOnSuccess { subject.onNext(it) }
        .doOnSuccess { invalidateDelayed(invalidationDelay) }
        .doOnDispose { if (data.state == Repository.State.LOADING) emitEmpty() }

    private fun nextValue(): Single<Data<TValue>> = subject.filter { it.isSuccess() || it.isFailed() }
        .flatMap { if (it.isFailed()) Observable.error(it.error) else Observable.just(it) }
        .firstOrError()

    private fun emitEmpty() {
        if (!isDisposed) {
            subject.onNext(Data.empty(id))
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

    override fun push() {
        if (!isDisposed) {
            subject.onNext(data)
        }
    }

    override fun clear(): Completable = Completable.fromAction {
        disposables.clear()
        if (data.state != Repository.State.EMPTY) {
            emitEmpty()
        }
    }

    override fun set(value: TValue) {
        subject.onNext(Data.success(id, value, Repository.Source.UNDEFINED, System.currentTimeMillis()))
    }

    protected abstract fun refresh(): Maybe<Result<TValue>>

    override fun isDisposed(): Boolean = disposables.isDisposed

    override fun dispose() {
        subject.onComplete()
        disposables.dispose()
    }
}
