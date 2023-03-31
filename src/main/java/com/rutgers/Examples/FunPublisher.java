/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Examples;

import com.rutgers.Core.Listener;
import com.rutgers.Core.Message.ARMessage;
import com.rutgers.Core.MessageListener;
import com.rutgers.Core.PulsarProducer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class FunPublisher {
    
    static int numRecords = 10000;
    static int recordSize = 200;
    static int iterations = 20;
    static boolean running = false;
    static Thread thread = null;
    static PulsarProducer producer = null;

    public abstract String readPayload();

    
    public class Push implements Runnable {
        ARMessage msg = null;
        ARMessage.Header.Profile profile = null;
        ARMessage.Header header = null;
        String[] sentences = null;

        Push(ARMessage msg) {
            this.msg = msg;
        }
        
        @Override
        public void run() {
        	// Creation of a record
            //Using some compression techniques to reduce network overhead
            for(int i = 0; i < iterations; i ++) {
                try {
                	// Pushing a record to the RP
                	String payload = readPayload();
                	ARMessage push_msg = ARMessage.newBuilder().setAction(ARMessage.Action.STORE_QUEUE).setTopic(msg.getTopic()).addPayload(payload).build();
                	System.out.printf("Sending: %s\n", payload);
                    producer.stream(push_msg, msg.getHeader().getPeerId());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnknownHostException | InterruptedException ex) {
                    Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static ARMessage.Header.Profile getProfile(Properties conf){
        //Message.ARMessage.Header.Profile profile = ARMessage.Header.Profile.newBuilder().addSingle("function").addSingle("env").build();

        String prefix = "profile.out";
        ARMessage.Header.Profile.Builder builder = ARMessage.Header.Profile.newBuilder();

        for (String key : conf.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String prof = conf.getProperty(key);
                builder = builder.addSingle(prof);
                System.out.printf("Adding profile %s\n", prof);
            }
        }
        return builder.build();
    }

    public void start(String propsFile, String confFile) throws UnknownHostException, ClassNotFoundException {
        try {
            // TODO code application logic here           
            InputStream props = new FileInputStream(propsFile);//Resources.getResource("producer.prop").openStream();
            Properties properties = new Properties();
            properties.load(props);

            InputStream conf = new FileInputStream(confFile);// Resources.getResource("consumer.prop").openStream();
            // Create a java util properties object
            Properties confProperties = new Properties();
            // Load the consumer properties into memory
            confProperties.load(conf);
            
            producer = new PulsarProducer(properties);
            producer.init();
            
            producer.replayListener(new Listener(){
                @Override
                public void replay(MessageListener ml, ARMessage o) {
                    switch(o.getAction()) {
                    	//Start streaming the sensor data
                        case NOTIFY_START:
                            running = true;
                            //Start a new thread with Push class
                            thread = new Thread(new Push(o));
                            thread.start();
                            break;
                        case NOTIFY_STOP:
                            try {
                                running = false;
                                thread.join();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                    }
                }
            });
            
            //Create sensor profile
            ARMessage.Header.Profile profile = getProfile(confProperties);
            ARMessage.Header header = ARMessage.Header.newBuilder().setLatitude(0.00).setLongitude(0.00).setType(ARMessage.RPType.AR_PRODUCER).setProfile(profile).setPeerId(producer.getPeerID()).build();
            ARMessage msg = ARMessage.newBuilder().setHeader(header).setAction(ARMessage.Action.NOTIFY_INTEREST).build();
            //Send the message to the RP
            producer.post(msg, profile);
            
        } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
