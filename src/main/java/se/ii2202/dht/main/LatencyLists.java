package se.ii2202.dht.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LatencyLists {


    public List<Double> list1 = new ArrayList<>();



    public LatencyLists(){

        InputStream stream = Application.class.getResourceAsStream("/latencies.txt");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
        {
            String line;
            while((line = br.readLine()) != null){
                   list1.add(Double.parseDouble(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }




}
