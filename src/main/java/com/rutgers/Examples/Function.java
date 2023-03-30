package com.rutgers.Examples;

import java.net.UnknownHostException;

public class Function {

    public static void start(String prodProps, String consProps, String confProps) throws UnknownHostException, ClassNotFoundException {
        FunConsumer.start(consProps, confProps);
        new FilePublisher().start(prodProps, confProps);
    }
    public static void main(String[] arg) throws UnknownHostException, ClassNotFoundException {
        String prodProps = arg[0];
        String consProps = arg[1];
        String confProps = arg[2];
        start(prodProps, consProps, confProps);
    }
}
