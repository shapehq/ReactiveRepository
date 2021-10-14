package com.novasa.reactiverepositoryexample

import com.novasa.reactiverepository.CachingRepository
import com.novasa.reactiverepository.Repository
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

class ItemRepository : CachingRepository<Item>("Data") {

    private var itemId: Int = 0

    init {
        invalidationDelay = 10 * 1000L
    }

    override fun refresh(): Maybe<Result<Item>> {
        return Maybe.timer(2, TimeUnit.SECONDS)
            .map { Item(++itemId) }
            .flatMap { if (it.id % 5 == 0) Maybe.error(RuntimeException("Failed repository refresh")) else Maybe.just(it) }
            .map { Result(it, Repository.Source.REMOTE) }
            .observeOn(AndroidSchedulers.mainThread())
    }
}
