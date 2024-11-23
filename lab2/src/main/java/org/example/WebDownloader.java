package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Set;
import java.util.HashSet;

public class WebDownloader {
    private static String webServerName = "https://example.com";
    private static boolean nestingEnabled = false;
    private static final String outputDir = "output";
    private static Set<String> visitedUrls = new HashSet<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Web Server name: " + webServerName);
        System.out.println("Nesting enabled: " + nestingEnabled);
        System.out.println("Type /help for list of commands.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            String[] commandParts = input.split(" ", 2);
            String command = commandParts[0];
            String argument = commandParts.length > 1 ? commandParts[1] : "";

            switch (command) {
                case "/help":
                    printHelp();
                    break;
                case "/setname":
                    if (!argument.isEmpty()) {
                        webServerName = argument;
                        System.out.println("Web server name set to: " + webServerName);
                    } else {
                        System.out.println("Usage: /setname [web server link]");
                    }
                    break;
                case "/setnesting":
                    if ("true".equalsIgnoreCase(argument)) {
                        nestingEnabled = true;
                        System.out.println("Nesting enabled: true (1 level deep)");
                    } else if ("false".equalsIgnoreCase(argument)) {
                        nestingEnabled = false;
                        System.out.println("Nesting enabled: false (only main page)");
                    } else {
                        System.out.println("Usage: /setnesting [true/false]");
                    }
                    break;
                case "/run":
                    try {
                        visitedUrls.clear();
                        runDownload(webServerName, 0);
                    } catch (IOException e) {
                        System.out.println("Error: Unable to connect to the server. Change the web server and try again.");
                    }
                    break;
                case "/clean":
                    cleanOutputDirectory();
                    break;
                case "/exit":
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown command. Type /help for list of commands.");
                    break;
            }
        }
    }

    private static void printHelp() {
        System.out.println("/setname [web server link] - set the name of the web server");
        System.out.println("/setnesting [true/false] - set whether to include nested pages (true: 1 level deep, false: only main page)");
        System.out.println("/run - copying the website's links to the output folder");
        System.out.println("/clean - clear the output folder");
        System.out.println("/exit - exit the application");
    }

    private static void runDownload(String url, int currentLevel) throws IOException {
        if (visitedUrls.contains(url)) {
            System.out.println("Already visited: " + url);
            return;
        }

        visitedUrls.add(url);

        System.out.println("Connecting to " + url + " ...");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            HttpEntity entity = httpClient.execute(request).getEntity();
            if (entity == null) {
                throw new IOException("No content from server.");
            }
            String htmlContent = EntityUtils.toString(entity);

            Document document = Jsoup.parse(htmlContent);

            Set<String> links = new HashSet<>();

            for (Element link : document.select("a[href]")) {
                String href = link.absUrl("href");
                if (!href.isEmpty()) {
                    links.add(href);
                }
            }
            for (Element script : document.select("script[src]")) {
                String src = script.absUrl("src");
                if (!src.isEmpty()) {
                    links.add(src);
                }
            }
            for (Element link : document.select("link[rel=stylesheet], link[rel=icon]")) {
                String href = link.absUrl("href");
                if (!href.isEmpty()) {
                    links.add(href);
                }
            }
            for (Element media : document.select("[src]")) {
                String src = media.absUrl("src");
                if (!src.isEmpty()) {
                    links.add(src);
                }
            }

            String folderName = (currentLevel == 0) ? "main" : "nesting";
            File levelDir = new File(outputDir + "/" + folderName);
            if (!levelDir.exists()) {
                levelDir.mkdir();
            }

            String outputFileName = url.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
            File outputFile = new File(levelDir + "/" + outputFileName);

            try (FileWriter writer = new FileWriter(outputFile)) {
                for (String link : links) {
                    writer.write(link + System.lineSeparator());
                }
            }

            System.out.println("Links copied to " + outputFile.getAbsolutePath());

            if (nestingEnabled && currentLevel == 0) {
                for (String link : links) {
                    runDownload(link, currentLevel + 1);
                }
            }
        }
    }

    private static void cleanOutputDirectory() {
        Path outputPath = Paths.get(outputDir);
        if (Files.exists(outputPath)) {
            try {
                if (Files.list(outputPath).findAny().isEmpty()) {
                    System.out.println("Output folder is already empty.");
                    return;
                }
                Files.walk(outputPath)
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .forEach(File::delete);

                visitedUrls.clear();

                System.out.println("Output folder cleaned.");
            } catch (IOException e) {
                System.out.println("Error while cleaning the output folder: " + e.getMessage());
            }
        } else {
            System.out.println("Output folder does not exist. Nothing to clean.");
        }
    }
}