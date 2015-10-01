package se.ii2202.dht.main;

import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.run.LauncherComp;

public class Executor {

    public static void main(String[] args){

        SimulationScenario scenario = Scenario.start();
        scenario.setSeed(1234);
        scenario.simulate(LauncherComp.class);

    }

}
