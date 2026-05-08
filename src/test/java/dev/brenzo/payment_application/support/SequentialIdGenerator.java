package dev.brenzo.payment_application.support;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SequentialIdGenerator implements Supplier<UUID> {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public UUID get() {
        long n = counter.incrementAndGet();
        return new UUID(0L, n);
    }
}
