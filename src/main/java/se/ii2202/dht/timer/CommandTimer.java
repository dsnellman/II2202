package se.ii2202.dht.timer;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class CommandTimer extends Timeout {


    public CommandTimer(ScheduleTimeout request) {
        super(request);
    }
}
