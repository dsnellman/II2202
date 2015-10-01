package se.ii2202.dht.timer;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class PeriodicPingTimer extends Timeout {

    public PeriodicPingTimer(SchedulePeriodicTimeout request) {
        super(request);
    }
}
