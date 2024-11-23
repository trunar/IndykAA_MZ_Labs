package org.example.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
    private static final int defaultPort = 8888;

    public static void main(String[] args) {
        int port = defaultPort;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + defaultPort);
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Proxy server is running on http://localhost:" + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting proxy server: " + e.getMessage());
        }
    }
}
