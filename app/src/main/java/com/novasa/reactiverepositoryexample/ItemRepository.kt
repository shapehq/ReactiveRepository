package com.novasa.reactiverepositoryexample

import com.novasa.reactiverepository.CachingRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class ItemRepository : CachingRepository<String, Item>("Data") {

    private var id: Int = 0

    init {
        invalidationDelay = 1000
    }

    override fun refresh(): Single<Item> {
        return Single.timer(2, TimeUnit.SECONDS)
            .map { Item(++id) }
            .observeOn(AndroidSchedulers.mainThread())
    }
}
