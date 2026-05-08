package dev.brenzo.payment_application.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class FixedClock extends Clock {

    private Instant now;

    public FixedClock(Instant initial) {
        this.now = initial;
    }

    public void set(Instant now) {
        this.now = now;
    }

    public void advanceSeconds(long seconds) {
        this.now = this.now.plusSeconds(seconds);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
