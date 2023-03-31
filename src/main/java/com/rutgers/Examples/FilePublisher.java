/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rutgers.Examples;
import java.io.*;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import static java.lang.Thread.sleep;


public class FilePublisher extends FunPublisher {
    static String outputFile = "/data/data";


    public static String readFile(File file, Long fileLength, String text) throws IOException {
        String line = null;

        BufferedReader in = new BufferedReader(new java.io.FileReader(file));
        in.skip(fileLength);
        while((line = in.readLine()) != null)
        {
            text += line;
        }
        in.close();
        return text;
    }

    public static String readStream() throws IOException {
        File file = new File(outputFile);
        String text = "";
        if(file.exists() && file.canRead()){
            long fileLength = file.length();
            readFile(file,0L, text);
            while(true){
                if(fileLength<file.length()){
                    readFile(file,fileLength, text);
                    fileLength=file.length();
                }
            }
        }
        return text;
    }

    public  String readPayload(){
        try{

            //String text = new String(Files.readAllBytes(Paths.get(outputFile)), StandardCharsets.UTF_8);
            String text = new String(Files.readAllBytes(Paths.get(outputFile)));
            System.out.printf("Reading payload from %s, data: %s\n", outputFile, text);
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
