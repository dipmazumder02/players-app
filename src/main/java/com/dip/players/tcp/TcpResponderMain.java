package com.dip.players.tcp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * TcpResponderMain
 * Responsibility: accept a single TCP client, echo back each message concatenated with its own send counter.
 * On receiving BYE, reply with BYE-ACK and exit.
 */
public class TcpResponderMain {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        int sentCounter = 0;
        try (ServerSocket server = new ServerSocket(port)) {
            log("Responder listening on port " + port);
            try (Socket socket = server.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = in.readLine()) != null) {
                    if ("BYE".equals(line)) {
                        log("Received BYE; replying BYE-ACK and exiting.");
                        out.write("BYE-ACK\n");
                        out.flush();
                        break;
                    }
                    sentCounter++;
                    String reply = line + " #" + sentCounter;
                    log("Replying: " + reply);
                    out.write(reply);
                    out.write("\n");
                    out.flush();
                }
            }
        }
        log("Responder exiting.");
    }

    private static void log(String s) {
        System.out.println("[RESPONDER] " + s);
    }
}
