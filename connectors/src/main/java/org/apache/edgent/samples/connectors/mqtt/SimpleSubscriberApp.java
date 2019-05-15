/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.edgent.samples.connectors.mqtt;

import static org.apache.edgent.samples.connectors.mqtt.MqttClient.OPT_PUB_CNT;
import static org.apache.edgent.samples.connectors.mqtt.MqttClient.OPT_QOS;
import static org.apache.edgent.samples.connectors.mqtt.MqttClient.OPT_RETAIN;
import static org.apache.edgent.samples.connectors.mqtt.MqttClient.OPT_TOPIC;

import java.awt.List;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.edgent.connectors.mqtt.MqttConfig;
import org.apache.edgent.connectors.mqtt.MqttStreams;
import org.apache.edgent.console.server.HttpServer;
import org.apache.edgent.providers.development.DevelopmentProvider;
import org.apache.edgent.samples.connectors.MsgSupplier;
import org.apache.edgent.samples.connectors.Options;
import org.apache.edgent.samples.connectors.Util;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.TWindow;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.TopologyProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A simple MQTT subscriber topology application.
 */
public class SimpleSubscriberApp {

	private final Properties props;
	private final String topic;
	private int counter = 1;
	

	public static void main(String[] args) throws Exception {
		if (args.length != 1)
			throw new Exception("missing pathname to mqtt.properties file");
		SimpleSubscriberApp subscriber = new SimpleSubscriberApp(args[0]);
		subscriber.run();
	}

	/**
	 * @param mqttPropsPath pathname to properties file
	 */
	SimpleSubscriberApp(String mqttPropsPath) throws Exception {
		props = new Properties();
		props.load(Files.newBufferedReader(new File(mqttPropsPath).toPath()));
		topic = props.getProperty("mqtt.topic");
		
	}

	private MqttConfig createMqttConfig() {
		MqttConfig mqttConfig = MqttConfig.fromProperties(props);
		return mqttConfig;
	}

	/**
	 * Create a topology for the subscriber application and run it.
	 */
	void run() throws Exception {
		
        DevelopmentProvider tp = new DevelopmentProvider();
        
        // build the application/topology
        
        Topology t = tp.newTopology("mqttSampleSubscriber");
        
        // System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full list

        // Create the MQTT broker connector
        MqttConfig mqttConfig = createMqttConfig();
        MqttStreams mqtt = new MqttStreams(t, () -> mqttConfig);
        
        // Subscribe to the topic and create a stream of messages
        TStream<String> msgs = mqtt.subscribe(topic, 0/*qos*/);
        
        // Process the received msgs - just print them out
//        msgs.sink(tuple -> System.out.println(
//                String.format("[%s] received: %s", Util.simpleTS(), tuple)));
        
        msgs.sink(tuple -> {
			try {
				filterData(tuple);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
        
        // run the application / topology
        System.out.println("Console URL for the job: "
                + tp.getServices().getService(HttpServer.class).getConsoleUrl());
        tp.submit(t);

	}
	
	public void filterData(String msg) throws Exception {
		
		JsonObject msgpayload = null;
        JsonArray observationSensorItems = null;
        JsonArray observationsenSensorItemsNew = new JsonArray();
        JsonObject sensorobject = null;
        JsonObject newobservation = new JsonObject();
    
        msgpayload = new JsonParser().parse(msg).getAsJsonObject();
//        System.out.println("msgpayload" + msgpayload);
        observationSensorItems = msgpayload.getAsJsonObject("Observation").getAsJsonArray("Sensors");
//        System.out.println("observationSensorItems" + observationSensorItems);
        int length = observationSensorItems.size();
            
            
        for (int i = 0; i < length; i++) {
            sensorobject = (JsonObject) observationSensorItems.get(i);
            
            if ((sensorobject.get("TypeCode").getAsString().equals("Temperature")) || (sensorobject.get("TypeCode").getAsString().equals("Illuminance")) || (sensorobject.get("TypeCode").getAsString().equals("Humidity")) ) {
//                System.out.println(sensorobject + "ist in neuem JSONArray");
                observationsenSensorItemsNew.add(sensorobject);
            }
            else {
//                System.out.println(sensorobject + "nicht in neuem JSONArray");
            }

        }
        
        newobservation.add("Sensors", observationsenSensorItemsNew);
        newobservation.add("DateTime", msgpayload.get("Observation").getAsJsonObject().get("DateTime"));
        msgpayload.add("Observation", newobservation);
//        System.out.println(observationsenSensorItemsNew);

        if(msgpayload != null)
        	publishFilteredData(msgpayload.toString());
        
	}
	
	
	public void publishFilteredData(String result) throws Exception {
	   
		String topic = "Factory/ColorSorter/Sensor/Filtered";
        
        
        DevelopmentProvider tp = new DevelopmentProvider();
        
        // build the application/topology
        
        Topology t = tp.newTopology("mqttSamplePublisher");
        

        // Create the MQTT broker connector
        MqttConfig mqttConfig = createMqttConfig();
        MqttStreams mqtt = new MqttStreams(t, () -> mqttConfig);
        
        // Create a sample stream of tuples to publish
        AtomicInteger cnt = new AtomicInteger();
        TStream<String> msgs = t.strings(result);
        
        mqtt.publish(msgs, topic, 0/*qos*/, true/*retain*/);
        
        // run the application / topology
//        System.out.println("Console URL for the job: "
//                + tp.getServices().getService(HttpServer.class).getConsoleUrl());
        tp.submit(t);
     
	}
	
	public void comments() {
//		int windowtime = 30;
//		DevelopmentProvider tp = new DevelopmentProvider();
//		
//
//		// build the application/topology
//
//		Topology t = tp.newTopology("mqttSampleSubscriber");
//
//		// System.setProperty("javax.net.debug", "ssl"); // or "all"; "help" for full
//		// list
//
//		// Create the MQTT broker connector
//		MqttConfig mqttConfig = createMqttConfig();
//		MqttStreams mqtt = new MqttStreams(t, () -> mqttConfig);
//
//		// Subscribe to the topic and create a stream of messages
//		TStream<String> stream = mqtt.subscribe(topic, 0/* qos */);
//		
//		stream.sink(tupel -> filterData(tupel));
//
////		stream.filter(tup -> (tup.contains("CEL") || tup.contains("LUX") || tup.contains("PCT"))); 
////		TWindow<String, Object> window = stream.last(windowtime, TimeUnit.SECONDS, tuple -> 0  );
////		TStream<Object> batch = window.batch((tuples,key) -> { return tuples; }); 
////		// Process the received msgs - just print them out
	}
}
