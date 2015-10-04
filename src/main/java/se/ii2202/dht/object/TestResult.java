package se.ii2202.dht.object;

public class TestResult {

    public enum TestType {
        STORE, LOOKUP;
    }

    public Long external1;
    public Long external2;
    public Long internal;
    public Long total;
    public Long time;
    public int messages;
    public int ring;
    public int appid;
    public int key;
    public TestType type;


    public TestResult(TestType type, int appid, int ring, int nMessages, int key,  Long external1, Long external2, Long internal, Long total, Long time){
        this.external1 = external1;
        this.external2 = external2;
        this.internal = internal;
        this.total = total;
        this.messages = nMessages;
        this.ring = ring;
        this.type = type;
        this.appid = appid;
        this.key = key;
        this.time = time;
    }
}
