package se.ii2202.dht.main;

import java.util.ArrayList;
import java.util.Arrays;


public class RunProperties {

    public final int runTime = 1400; //In seconds

    public final int M = 14; //Number of bits in identifier
    public final int nNode = 500; //Number of nodes in each ring
    public final int nRings = 5; //Number of rings
    public final int nApps = 540; //Number of applications

    public final int replications = 5; //Not lower than 1


    //Time for handles to process work
    public final boolean randomTime = false;
    public final int constantProccessTime = 5;
    public final int maxProcessMsgTime = 10;
    public final int minProcessMsgTime = 5;


    //Operations for each app
    public int NUMBER_OF_ADDS = 100;
    public int NUMBER_OF_LOOKUPS = 100;
    public int PERIODIC_PING_TIMEOUT = 2000;

    public int DELAY_BETWEEN_OP = 1000;
    public int DELAY_BETWEEN_ADD_LOOKUP = 100000;

    //Strategy for choosing ring
    public boolean testStrategy_nFirst = true;
    public int n = 1;
    public boolean testStrategy_algorithm1 = false;

    public boolean randomChoose = false;
    public boolean bestChoose = false;
    public boolean worstChoose = true;
    public boolean algorithmChoose = false;

    public boolean lookUpToAll = true;
    public int nLookUp = 1; //Not bigger than replications

    public ArrayList<String> RingCities = new ArrayList<>(Arrays.asList("FRAN", "CALI", "SDNY", "SPLO", "TKYO"));
    public ArrayList<String> allCities = new ArrayList<>(Arrays.asList("FRAN", "CALI", "SDNY", "SPLO", "TKYO", "VGNI", "SNGP", "ORGN", "IRLD"));



    public RunProperties(){}
}
