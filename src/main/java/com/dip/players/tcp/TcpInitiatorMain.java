package com.dip.players.tcp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * TcpInitiatorMain
 * Responsibility: initiator process connecting to a responder over TCP, performing 10 round-trips,
 * then sending BYE and exiting gracefully.
 */
public class TcpInitiatorMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java ... TcpInitiatorMain <host> <port>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int sentCounter = 0;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String payload = "hello";
            for (int i = 0; i < 10; i++) {
                sentCounter++;
                String msg = payload + " #" + sentCounter;
                log("Sending: " + msg);
                out.write(msg);
                out.write("\n");
                out.flush();

                String reply = in.readLine();
                log("Received: " + reply);
                payload = reply; // continue conversation using latest payload content
            }

            // send termination
            log("Sending BYE");
            out.write("BYE\n");
            out.flush();
            String ack = in.readLine();
            log("Received: " + ack);
            log("Initiator exiting.");
        }
    }

    private static void log(String s) {
        System.out.println("[INITIATOR] " + s);
    }
}
