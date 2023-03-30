/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Examples;

import com.rutgers.Core.Listener;
import com.rutgers.Core.Message;
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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;


/**
 * This is and example of the use of the R-Pulsar API.
 * This example shows how to build a R-Pulsar Publisher.
 * @param keys
 * @return
 */
public class FilePublisher { 
    
    static int numRecords = 10000;
    static int recordSize = 200;
    static int iterations = 20;
    static boolean running = false;
    static Thread thread = null;
    static PulsarProducer producer = null;
    static String outputFile = "/data/data";

    private static String readFile() {
        try{
            String text = new String(Files.readAllBytes(Paths.get(outputFile)), StandardCharsets.UTF_8);
            return text;
        }catch (java.io.IOException e){
            System.out.printf("Error %s\n", e);
            return "NULL";
        }
    }

    
    public static class Push implements Runnable {
        Message.ARMessage msg = null;
        ARMessage.Header.Profile profile = null;
        ARMessage.Header header = null;
        String[] sentences = null;
        
        Push(Message.ARMessage msg) {
            this.msg = msg;
        }
        
        @Override
        public void run() {
        	// Creation of a record
            //Using some compression techniques to reduce network overhead
            for(int i = 0; i < iterations; i ++) {
                try {
                	// Pushing a record to the RP
                	String payload = readFile();
                	Message.ARMessage push_msg = Message.ARMessage.newBuilder().setAction(Message.ARMessage.Action.STORE_QUEUE).setTopic(msg.getTopic()).addPayload(payload).build();
                	System.out.println("Sending: Hello World!!");
                    producer.stream(push_msg, msg.getHeader().getPeerId());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnknownHostException | InterruptedException ex) {
                    Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    

    public static void start(String propsFile) throws UnknownHostException, ClassNotFoundException {
        try {
            // TODO code application logic here           
            InputStream props = new FileInputStream(propsFile);//Resources.getResource("producer.prop").openStream();
            Properties properties = new Properties();
            properties.load(props);
            
            producer = new PulsarProducer(properties);
            producer.init();
            
            producer.replayListener(new Listener(){
                @Override
                public void replay(MessageListener ml, Message.ARMessage o) {
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
            ARMessage.Header.Profile profile = ARMessage.Header.Profile.newBuilder().addSingle("function").addSingle("env").build();
            ARMessage.Header header = ARMessage.Header.newBuilder().setLatitude(0.00).setLongitude(0.00).setType(ARMessage.RPType.AR_PRODUCER).setProfile(profile).setPeerId(producer.getPeerID()).build();
            ARMessage msg = ARMessage.newBuilder().setHeader(header).setAction(ARMessage.Action.NOTIFY_INTEREST).build();
            //Send the message to the RP
            producer.post(msg, profile);
            
        } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void main(String[] arg) throws UnknownHostException, ClassNotFoundException {
        start(arg[0]);

    }
}
