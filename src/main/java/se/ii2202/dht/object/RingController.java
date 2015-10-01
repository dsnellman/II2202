package se.ii2202.dht.object;

public class RingController {

    public Long external1;
    public Long external2;
    public Long internal;
    public Long totalTime;

    public RingController(Long external1, Long external2, Long internal, Long totalTime) {
        this.external1 = external1;
        this.external2 = external2;
        this.internal = internal;
        this.totalTime = totalTime;
    }
}
