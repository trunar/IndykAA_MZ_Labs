package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {
    private static final int PORT = 8080;
    private static final String rootDir = "public";

    public static void main(String[] args) {
        System.out.println("Starting HTTP Server...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on http://localhost:" + PORT);
            System.out.println("Serving files from: " + rootDir);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());
                new Thread(new HTTPHandler(clientSocket, rootDir)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}