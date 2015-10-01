package se.ii2202.dht.timer;

import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;

public class StabilizerTimer extends Timeout {

    public StabilizerTimer(SchedulePeriodicTimeout request) {
        super(request);
    }

}
