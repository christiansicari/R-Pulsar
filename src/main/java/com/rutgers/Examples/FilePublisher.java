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



public class FilePublisher extends FunPublisher {


    public  String readPayload(){
        try{
            String text = new String(Files.readAllBytes(Paths.get(outputFile)), StandardCharsets.UTF_8);
            return text;
        }catch (java.io.IOException e){
            System.out.printf("Error %s\n", e);
            return "NULL";
        }
    }

    public static void main(String[] arg) throws UnknownHostException, ClassNotFoundException {
        FilePublisher obj = new FilePublisher();
        obj.start(arg[0], arg[1]);

    }
}
