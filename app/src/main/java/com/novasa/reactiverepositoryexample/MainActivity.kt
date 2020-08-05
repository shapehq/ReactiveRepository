package com.novasa.reactiverepositoryexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.novasa.reactiverepository.Repository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var repo: Repository<String, Item>

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = ItemRepository()

        disposables += repo.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { print("observe", it) }

        get.setOnClickListener {
            print("GET")

            disposables += repo.get()
                .subscribe { v ->
                    print("get", v)
                }
        }

        update.setOnClickListener {
            print("UPDATE")
            disposables += repo.update()
                .subscribe { v ->
                    print("update", v)
                }
        }

        current.setOnClickListener {
            print("CURRENT")
            print("current", repo.value)
        }

        clear.setOnClickListener {
            print("CLEAR")
            disposables += repo.clear().subscribe()
        }
    }

    @SuppressLint("SetTextI18n")
    fun print(source: String, data: Repository.Data<String, Item>) {
        val text = "$source: $data, age: ${data.age}"
        print(text)
    }

    fun print(text: String) {

        Log.d("Repo", text)

        if (log.text.isEmpty()) {
            log.text = text

        } else {
            log.text = "${log.text}\n$text"
        }

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}
