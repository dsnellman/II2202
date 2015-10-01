package se.ii2202.dht.timer;

import se.ii2202.dht.appmsg.LookUp;
import se.ii2202.dht.msg.ClosestFingerResponse;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class InnerLatencyTimerLookUp extends Timeout

    {

        public LookUp lookup;
        public int msgId;

        public InnerLatencyTimerLookUp(ScheduleTimeout request,int msgId, LookUp lookup) {
        super(request);
        this.lookup = lookup;
        this.msgId = msgId;
    }
}
