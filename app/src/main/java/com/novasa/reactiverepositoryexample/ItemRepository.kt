package com.novasa.reactiverepositoryexample

import com.novasa.reactiverepository.CachingRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

class ItemRepository : CachingRepository<String, Item>("Data") {

    private var id: Int = 0

    init {
        invalidationDelay = 10 * 1000
    }

    override fun refresh(): Single<Item> {
        return Single.timer(2, TimeUnit.SECONDS)
            .map { Item(++id) }
            .flatMap { if (it.id % 5 == 0) Single.error(RuntimeException("Failed repository refresh")) else Single.just(it) }
            .observeOn(AndroidSchedulers.mainThread())
    }
}
