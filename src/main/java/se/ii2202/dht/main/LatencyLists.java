package se.ii2202.dht.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ii2202.dht.object.LatencyContainer;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LatencyLists {

    private static final Logger log = LoggerFactory.getLogger(LatencyLists.class);


    /*public List<Long> FranCali = new ArrayList<>();
    public List<Long> FranSdny = new ArrayList<>();
    public List<Long> FranSplo = new ArrayList<>();
    public List<Long> FranToky = new ArrayList<>();
    public List<Long> CaliFran = new ArrayList<>();
    public List<Long> CaliSdny = new ArrayList<>();
    public List<Long> CaliSplo = new ArrayList<>();
    public List<Long> CaliToky = new ArrayList<>();
    public List<Long> SdnyFran = new ArrayList<>();
    public List<Long> SdnyCali = new ArrayList<>();
    public List<Long> SdnySplo = new ArrayList<>();
    public List<Long> SdnyToky = new ArrayList<>();
    public List<Long> SploFran = new ArrayList<>();
    public List<Long> SploCali = new ArrayList<>();
    public List<Long> SploSdny = new ArrayList<>();
    public List<Long> SploToky = new ArrayList<>();
    public List<Long> TokyFran = new ArrayList<>();
    public List<Long> TokyCali = new ArrayList<>();
    public List<Long> TokySdny = new ArrayList<>();
    public List<Long> TokySplo = new ArrayList<>();*/


    public ArrayList<LatencyContainer> latencies = new ArrayList<>();

    public LatencyLists(int lines){

        log.info("Start reading files..");



        File file = new File("./src/main/resources/latencies/");

        if(file != null){

            for(File f : file.listFiles()){
                int counter = 0;
                String name = f.getName();
                String[] cities = name.substring(0,9).split("-");

                LatencyContainer latency = new LatencyContainer();
                latency.from = cities[0];
                latency.to = cities[1];

                try (BufferedReader br = new BufferedReader(new FileReader(f)))
                {
                    String line;
                    while((line = br.readLine()) != null){
                        Double d = Double.parseDouble(line);
                        latency.latencies.add(d.intValue());
                        counter++;
                        if(counter == lines)
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                latencies.add(latency);

                log.info("{} , {}, size {}", new Object[]{cities[0], cities[1], latency.latencies.size()});
            }

        }

        log.info("Done reading files.. {}", new Object[]{latencies.size()});

    }

    public ArrayList<LatencyContainer> getLatencies(){
        return latencies;
    }




}
