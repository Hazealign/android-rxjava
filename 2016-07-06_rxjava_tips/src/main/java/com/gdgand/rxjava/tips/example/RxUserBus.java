package com.gdgand.rxjava.tips.example;

// import com.fernandocejas.frodo.annotation.RxLogObservable;

import rx.Observable;
import rx.subjects.PublishSubject;

public final class RxUserBus {
    private static PublishSubject<String> bus = PublishSubject.create();

    private RxUserBus() {}

    static Observable<String> sub() {
        return bus.asObservable();
    }

    static void pub(String user) {
        bus.onNext(user);
    }
}