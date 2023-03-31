/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Examples;

import com.rutgers.Core.Listener;
import com.rutgers.Core.Message;
import com.rutgers.Core.MessageListener;
import com.rutgers.Core.PulsarConsumer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;


public class FunConsumer {
    static boolean running = false;
    static Thread thread = null;
    static PulsarConsumer consumer = null;
    static String outputFile = "/data/data";

    public static void invokeFunction(String functionUrl, String payload) {
        String userAgent = "Mozilla/5.0";
        System.out.printf("Invoking function at %s\n", functionUrl);
        try {
            String result = sendPOST(userAgent, functionUrl, payload);
            System.out.printf("RESULT: %s\n", result);
            writeResult(result);
        } catch (IOException e) {

            e.printStackTrace();
        }

    }

    private static String sendPOST(String userAgent, String endpoint, String payload) throws IOException {
        URL obj = new URL(endpoint);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", userAgent);

        // For POST only - START
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(payload.getBytes());
        os.flush();
        os.close();
        // For POST only - END

        int responseCode = con.getResponseCode();
        System.out.println("POST Response Code :: " + responseCode);

        if (responseCode != HttpURLConnection.HTTP_SERVER_ERROR) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // print result
            return response.toString();
        }
        throw new IOException("Error handling request");
    }
    private static void writeResult(String str){
        try{
            System.out.printf("Writing on %s, data: %s\n", outputFile, str);
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(str);       
            writer.close();
        }
        catch (IOException e){
            System.out.println("Unable to write the result");
        }
        
    }

    public static class Consum implements Runnable {
        Message.ARMessage msg;
        Message.ARMessage.Header.Profile profile;
        Message.ARMessage.Header header;
        String functionUrl;

        Consum(Message.ARMessage msg, String functionUrl) {
            this.msg = msg;
            this.functionUrl = functionUrl;
            String profile_value = "SmokeDetector";
            profile = Message.ARMessage.Header.Profile.newBuilder().addSingle(profile_value).build();
            header = Message.ARMessage.Header.newBuilder().setLatitude(0.00).setLongitude(0.00)
                    .setType(Message.ARMessage.RPType.AR_CONSUMER).setProfile(profile).setPeerId(consumer.getPeerID())
                    .build();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Message.ARMessage consum_msg = Message.ARMessage.newBuilder().setHeader(header)
                            .setAction(Message.ARMessage.Action.REQUEST).setTopic(msg.getTopic()).build();
                    // Get the message that was send by the sensor
                    Message.ARMessage poll = consumer.poll(consum_msg, msg.getHeader().getPeerId());

                    String payload = poll.getPayload(0);
                    System.out.println("Received: " + StringEscapeUtils.unescapeJava(payload));
                    invokeFunction(functionUrl, payload);

                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnknownHostException ex) {
                    Logger.getLogger(Consumer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static Message.ARMessage.Header.Profile getProfile(Properties conf){
        /*Message.ARMessage.Header.Profile profile = Message.ARMessage.Header.Profile.newBuilder()
                .addSingle("temperature").addSingle("fahrenheit").build();
        // Create a header and set our physical location
        */
        String prefix = "profile.in";
        Message.ARMessage.Header.Profile.Builder builder = Message.ARMessage.Header.Profile.newBuilder();

        for (String key : conf.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String prof = conf.getProperty(key);
                builder = builder.addSingle(prof);
                System.out.printf("Adding profile %s\n", prof);
            }
        }
        return builder.build();
    }
    public static void start(String propsFile, String confFile) throws UnknownHostException, ClassNotFoundException {
        try {
            Thread.sleep(10000);
            // Load the consumer properties into an InputStream
            InputStream props = new FileInputStream(propsFile);// Resources.getResource("consumer.prop").openStream();
            // Create a java util properties object
            Properties properties = new Properties();
            // Load the consumer properties into memory
            properties.load(props);

            InputStream conf = new FileInputStream(confFile);// Resources.getResource("consumer.prop").openStream();
            // Create a java util properties object
            Properties confProperties = new Properties();
            // Load the consumer properties into memory
            confProperties.load(conf);


            // Create a new R-Pulsar consumer object
            consumer = new PulsarConsumer(properties);
            // Init the R-Pulsar consumer
            consumer.init();

            // Create a listener for incoming AR messages
            consumer.replayListener(new Listener() {
                @Override
                public void replay(MessageListener ml, Message.ARMessage o) {
                    switch (o.getAction()) {
                        // When we receive a notify_start we need to start processing the data that we
                        // will receive
                        case NOTIFY_START:
                            running = true;
                            // Start a new thread with the Consum class
                            thread = new Thread(new Consum(o, confProperties.getProperty("function")));
                            thread.start();
                            break;
                        // We will not receive any more data from the sensors
                        case NOTIFY_STOP:
                            try {
                                running = false;
                                thread.join();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                        case REQUEST_RESPONSE:

                            break;
                    }
                }
            });
            Message.ARMessage.Header.Profile profile = getProfile(confProperties);
            Message.ARMessage.Header header = Message.ARMessage.Header.newBuilder().setLatitude(0.00).setLongitude(0.00)
                    .setType(Message.ARMessage.RPType.AR_CONSUMER).setProfile(profile).setPeerId(consumer.getPeerID())
                    .build();

            // Create an AR Message and tell the system to notify us if there is a profile
            // that matches this criteria
            Message.ARMessage msg = Message.ARMessage.newBuilder().setHeader(header)
                    .setAction(Message.ARMessage.Action.NOTIFY_DATA).build();
            // Use the consumer object to send the message
            consumer.post(msg);

        } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(HelloWorldPublisher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void main(String[] arg) throws UnknownHostException, ClassNotFoundException {
        start(arg[0], arg[1]);
    }
}
