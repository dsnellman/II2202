package se.ii2202.dht.timer;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class CommandTimer extends Timeout {

    public int type;

    public CommandTimer(ScheduleTimeout request, int type) {
        super(request);
        this.type = type;
    }
}
