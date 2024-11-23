package org.example.proxy;

import java.io.*;
import java.net.Socket;
import java.net.URL;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
                InputStream clientInput = clientSocket.getInputStream();
                OutputStream clientOutput = clientSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput));
                PrintWriter writer = new PrintWriter(clientOutput, true)
        ) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) {
                sendBadRequestResponse(writer);
                return;
            }

            String method = requestParts[0];
            String url = requestParts[1];

            if (!url.startsWith("http")) {
                sendBadRequestResponse(writer);
                return;
            }

            Logger.log("Method: " + method + " | URL: " + url);

            if (method.equals("GET") || method.equals("HEAD") || method.equals("POST")) {
                if (method.equals("GET") && CacheManager.hasCached(url)) {
                    Logger.log("Cache hit for: " + url);
                    byte[] cachedResponse = CacheManager.getCachedResponse(url);
                    clientOutput.write(cachedResponse);
                    return;
                }

                forwardRequestToServer(method, url, reader, clientOutput);

            } else {
                sendMethodNotAllowedResponse(writer);
            }
        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void forwardRequestToServer(String method, String url, BufferedReader clientReader, OutputStream clientOutput) {
        try {
            URL targetUrl = new URL(url);
            Socket serverSocket = new Socket(targetUrl.getHost(), 80);

            try (
                    OutputStream serverOutput = serverSocket.getOutputStream();
                    InputStream serverInput = serverSocket.getInputStream()
            ) {
                PrintWriter serverWriter = new PrintWriter(serverOutput, true);
                serverWriter.println(method + " " + targetUrl.getFile() + " HTTP/1.1");
                serverWriter.println("Host: " + targetUrl.getHost());
                serverWriter.println("Connection: close");

                if (method.equals("POST")) {
                    String postData = readPostData(clientReader);
                    serverWriter.println("Content-Length: " + postData.length());
                    serverWriter.println();
                    serverWriter.println(postData);
                }

                serverWriter.println();
                serverWriter.flush();

                if (method.equals("POST")) {
                    sendPostResponse(clientOutput);
                }

                byte[] buffer = new byte[8192];
                int bytesRead;
                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                while ((bytesRead = serverInput.read(buffer)) != -1) {
                    responseBuffer.write(buffer, 0, bytesRead);
                }

                if (method.equals("HEAD")) {
                    String headers = responseBuffer.toString();
                    clientOutput.write(headers.getBytes());
                } else {
                    if (method.equals("GET")) {
                        CacheManager.cacheResponse(url, responseBuffer.toByteArray());
                        Logger.log("Response cached for: " + url);
                    }
                    clientOutput.write(responseBuffer.toByteArray());
                }
            } finally {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error forwarding request: " + e.getMessage());
        }
    }

    private String readPostData(BufferedReader clientReader) throws IOException {
        StringBuilder postData = new StringBuilder();
        String line;
        while ((line = clientReader.readLine()) != null && !line.isEmpty()) {
            postData.append(line).append("\n");
        }
        return postData.toString();
    }

    private void sendBadRequestResponse(PrintWriter writer) {
        writer.println("HTTP/1.1 400 Bad Request");
        writer.println("Content-Type: text/plain");
        writer.println();
        writer.println("Bad Request");
    }

    private void sendMethodNotAllowedResponse(PrintWriter writer) {
        writer.println("HTTP/1.1 405 Method Not Allowed");
        writer.println("Content-Type: text/plain");
        writer.println();
        writer.println("Method Not Allowed");
    }

    private void sendPostResponse(OutputStream clientOutput) {
        PrintWriter writer = new PrintWriter(clientOutput, true);
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/plain");
        writer.println();
        writer.println("POST request successfully received by proxy server");
    }
}