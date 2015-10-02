package se.ii2202.dht.timer;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class PeriodicClockTimer extends Timeout {

    public PeriodicClockTimer(SchedulePeriodicTimeout request) {
        super(request);
    }
}
