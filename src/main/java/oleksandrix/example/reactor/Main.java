package oleksandrix.example.reactor;

import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Flux.fromIterable(Arrays.asList(1, 2, 2, 3));

        Flux
                .<String>generate(sink -> sink.next("hell"))
                .delayElements(Duration.ofMillis(500))
                .take(2);

        Flux<Integer> producer = Flux.generate(
                () -> 2345,
                (state, sink) -> {
                    if (state > 2366) {
                        sink.complete();
                    } else {
                        sink.next(state);
                    }

                    return state + 3;
                }
        );

        Flux.create(fluxSink -> {
            producer.subscribe(new BaseSubscriber<Integer>() {
                @Override
                protected void hookOnNext(Integer value) {
                    fluxSink.next(value);
                }

                @Override
                protected void hookOnComplete() {
                    fluxSink.complete();
                }
            });
            fluxSink.onRequest(r -> fluxSink.next("DB returns: " + producer.blockFirst()));
        });

        Flux<String> second = Flux.just("hell", "oleksandrix").repeat();

        Flux<String> sumFlux = Flux.just("hell", "java", "oleksandrix", "java")
                .zipWith(second, (f, s) -> String.format("%s %s", f, s));

        Flux<String> stringFlux = sumFlux.delayElements(Duration.ofMillis(1300))
                .timeout(Duration.ofSeconds(1))
                .onErrorResume(throwable ->
                        Flux.interval(Duration.ofMillis(300))
                                .map(String::valueOf)
                )
                .skip(2)
                .take(3);

        stringFlux.subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("Finished")
        );

        Thread.sleep(30000);
    }
}
