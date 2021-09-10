package com.novasa.reactiverepositoryexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.novasa.reactiverepository.Repository
import com.novasa.reactiverepositoryexample.databinding.ActivityMainBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign

class MainActivity : AppCompatActivity() {

    private lateinit var repo: Repository<String, Item>

    private val disposables = CompositeDisposable()

    private var periodicDisposable: Disposable? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        repo = ItemRepository()

        disposables += repo.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { print("observe", it) }

        binding.apply {
            get.setOnClickListener {
                print("GET")

                disposables += repo.get()
                    .subscribe({ print("get", it) }, { print("get", it) })
            }

            update.setOnClickListener {
                print("UPDATE")
                disposables += repo.update()
                    .subscribe({ print("update", it) }, { print("update", it) })
            }

            current.setOnClickListener {
                print("CURRENT")
                print("current", repo.data)
            }

            push.setOnClickListener {
                print("PUSH")
                repo.push()
            }

            clear.setOnClickListener {
                print("CLEAR")
                disposables += repo.clear().subscribe()
            }

            periodic.setOnClickListener {
                print("PERIODIC")

                periodicDisposable?.let {
                    it.dispose()
                    periodicDisposable = null
                } ?: run {
                    disposables += repo.periodicUpdates(5000, 2000)
                        .doOnSubscribe { d -> periodicDisposable = d }
                        .doFinally { periodicDisposable = null }
                        .retry()
                        .subscribe({ print("periodic", it) }, { print("periodic", it) })
                }
            }
        }
    }

    private fun print(source: String, data: Repository.Data<String, Item>) {
        val text = "$source: $data, age: ${data.age}"
        print(text)
    }

    private fun print(source: String, error: Throwable) {
        val text = "$source failed: $error"
        print(text)
    }

    @SuppressLint("SetTextI18n")
    private fun print(text: String) {

        Log.d("Repo", text)

        binding.apply {
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
}
