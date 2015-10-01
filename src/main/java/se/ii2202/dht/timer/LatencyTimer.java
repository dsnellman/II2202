package se.ii2202.dht.timer;

import se.ii2202.dht.appmsg.AppMessage;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class LatencyTimer extends Timeout {

    public AppMessage<Object> msg;
    public int type;

    public LatencyTimer(ScheduleTimeout request, AppMessage<Object> msg, int type) {
        super(request);
        this.msg = msg;
        this.type = type;
    }

}