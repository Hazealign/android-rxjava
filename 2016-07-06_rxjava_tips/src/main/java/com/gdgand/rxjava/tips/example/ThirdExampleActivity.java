package com.gdgand.rxjava.tips.example;

import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.gdgand.rxjava.tips.MainActivity;
import com.gdgand.rxjava.tips.R;
import com.gdgand.rxjava.tips.example.more.WeakSubscriberDecorator;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotificationListener;
import rx.plugins.RxJavaPlugins;
import rx.schedulers.Schedulers;

public class ThirdExampleActivity extends AppCompatActivity {
    @BindView(R.id.search_bar) EditText searchBar;
    @BindView(R.id.results) RecyclerView results;

    static {
        String TAG = "Rx-Debug Example";
        RxJavaPlugins.getInstance().registerObservableExecutionHook(new DebugHook(new DebugNotificationListener() {
            public Object onNext(DebugNotification n) {
                Log.v(TAG, "onNext on " + n);
                return super.onNext(n);
            }


            public Object start(DebugNotification n) {
                Log.v(TAG, "start on " + n);
                return super.start(n);
            }


            public void complete(Object context) {
                Log.v(TAG, "complete on " + context);
            }

            public void error(Object context, Throwable e) {
                Log.e(TAG, "error on " + context);
            }
        }));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);
        ButterKnife.bind(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        results.setLayoutManager(layoutManager);
        results.setHasFixedSize(true);

        final SearchRecyclerAdapter adapter = new SearchRecyclerAdapter(Collections.emptyList());
        results.setAdapter(adapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LruCache<String, SearchResult> cache = new LruCache<>(5 * 1024 * 1024); // 5MiB

        final GitHubInteractor interactor = new GitHubInteractor(retrofit, cache);

        RxUserBus.sub().subscribe(new WeakSubscriberDecorator<>(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {
                Toast.makeText(ThirdExampleActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        }));

        RxTextView.textChanges(searchBar)
                .observeOn(Schedulers.io())
                .filter(charSequence -> charSequence.length() > 0)
                .switchMap((CharSequence seq) -> interactor.searchUsers(seq.toString()))
                .flatMap((SearchResult searchResult) ->
                        Observable.from(searchResult.getItems()).limit(20).toList())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new WeakSubscriberDecorator<>(new Subscriber<List<SearchItem>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(List<SearchItem> searchItems) {
                        adapter.refreshResults(searchItems);
                    }
                }));
    }
}