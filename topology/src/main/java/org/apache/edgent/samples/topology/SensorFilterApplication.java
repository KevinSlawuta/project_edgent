package org.apache.edgent.samples.topology;

import java.util.concurrent.TimeUnit;

import org.apache.edgent.function.Supplier;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;

public class SensorFilterApplication {

	public static void main(String[] args) {
	
		Sensor sensor = new Sensor (); 
		DirectProvider dp = new DirectProvider(); 
		Topology topology = dp.newTopology();
		TStream<String> tempReadings = topology.poll(null, 0, null);
	   
	        dp.submit(topology);
	    }
}

