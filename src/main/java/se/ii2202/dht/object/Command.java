package se.ii2202.dht.object;

public class Command {

    public enum TYPE {
        SLEEP, ADD, LOOKUP;
    }

    public int key;
    public int value;
    public TYPE type;

    public Command(TYPE type, int key, int value){
        this.key = key;
        this.value = value;
        this.type = type;

    }

    public Command(TYPE type, int x){
        if(type == TYPE.SLEEP){
            this.value = x;
        }
        else if(type == TYPE.LOOKUP){
            this.key = x;
        }
        this.type = type;

    }

}
