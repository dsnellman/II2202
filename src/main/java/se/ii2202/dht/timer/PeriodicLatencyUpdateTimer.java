package se.ii2202.dht.timer;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class PeriodicLatencyUpdateTimer extends Timeout {

    public PeriodicLatencyUpdateTimer(SchedulePeriodicTimeout request) {
        super(request);
    }
}
