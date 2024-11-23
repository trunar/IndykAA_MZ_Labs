package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HTTPHandler implements Runnable {
    private final Socket clientSocket;
    private final String rootDir;

    public HTTPHandler(Socket clientSocket, String rootDir) {
        this.clientSocket = clientSocket;
        this.rootDir = rootDir;
    }

    @Override
    public void run() {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String requestLine = reader.readLine();
            if (requestLine != null) {
                System.out.println("Request: " + requestLine);

                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 3) {
                    sendErrorResponse(output, 400, "Bad Request");
                    return;
                }

                String method = requestParts[0];
                String url = requestParts[1];

                String filePath = url.equals("/") ? "/index.html" : url;
                Path file = Paths.get(rootDir, filePath);

                if (method.equals("GET")) {
                    handleGetRequest(file, output);
                } else if (method.equals("HEAD")) {
                    handleHeadRequest(file, output);
                } else if (method.equals("POST")) {
                    handlePostRequest(reader, output);
                } else {
                    sendErrorResponse(output, 405, "Method Not Allowed");
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleGetRequest(Path file, OutputStream output) throws IOException {
        if (Files.exists(file)) {
            byte[] fileContent = Files.readAllBytes(file);
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + getContentType(file) + "\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n\r\n";
            output.write(response.getBytes());
            output.write(fileContent);
        } else {
            sendErrorResponse(output, 404, "Not Found");
        }
    }

    private void handleHeadRequest(Path file, OutputStream output) throws IOException {
        if (Files.exists(file)) {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + getContentType(file) + "\r\n" +
                    "Content-Length: " + Files.size(file) + "\r\n\r\n";
            output.write(response.getBytes());
        } else {
            sendErrorResponse(output, 404, "Not Found");
        }
    }

    private void handlePostRequest(BufferedReader reader, OutputStream output) throws IOException {
        String line;
        int contentLength = 0;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
            System.out.println("Header: " + line);
        }

        if (contentLength > 0) {
            char[] body = new char[contentLength];
            int bytesRead = reader.read(body, 0, contentLength);
            if (bytesRead > 0) {
                String postData = new String(body);
                System.out.println("POST data: " + postData);

                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 13\r\n" +
                        "\r\n" +
                        "POST received";
                output.write(response.getBytes());
            }
        } else {
            String response = "HTTP/1.1 400 Bad Request\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: 11\r\n" +
                    "\r\n" +
                    "Bad Request";
            output.write(response.getBytes());
        }
    }

    private void sendErrorResponse(OutputStream output, int statusCode, String statusMessage) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + statusMessage.length() + "\r\n\r\n" +
                statusCode + " " + statusMessage;
        output.write(response.getBytes());
    }

    private String getContentType(Path file) {
        String fileName = file.toString();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }
}