package se.ii2202.dht.object;

import java.util.ArrayList;

public class KeyPlacedInfo {

    public int ring = -1;
    public ArrayList<Integer> replicas;

    public KeyPlacedInfo(){
        replicas = new ArrayList<>();
    }

    public String toString(){
        return "ring: " + ring + " replicas: " + replicas.toString();
    }
}
