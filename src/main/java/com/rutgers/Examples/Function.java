package com.rutgers.Examples;

import java.net.UnknownHostException;

public class Function {

    public static void start(String prodProps, String consProps) throws UnknownHostException, ClassNotFoundException {
        SmokeConsumerFaas.start(consProps);
        FilePublisher.start(prodProps);
    }
    public static void main(String[] arg) throws UnknownHostException, ClassNotFoundException {
        String prodProps = arg[0];
        String consProps = arg[1];
        start(prodProps, consProps);
    }
}
