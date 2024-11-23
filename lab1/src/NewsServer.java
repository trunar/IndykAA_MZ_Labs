import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NewsServer {
    private static final int PORT = 12345;
    private static Map<String, Map<String, String>> newsDatabase = new ConcurrentHashMap<>();
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static int clientIdCounter = 1;
    private static int newsIdCounter = 0;

    public static void main(String[] args) {
        System.out.println("News Server started...");

        initializeDatabase();

        new Thread(NewsServer::listenForCommands).start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                String clientId = "cl" + clientIdCounter++;
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);

                clients.put(clientId, clientHandler);

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disconnectClientById(String clientId) {
        ClientHandler clientHandler = clients.get(clientId);
        if (clientHandler != null) {
            clientHandler.disconnect();
            clients.remove(clientId);
        } else {
            System.out.println("Client with ID " + clientId + " not found.");
        }
    }

    private static void listenForCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            if (command.startsWith("kick")) {
                String[] parts = command.split(" ");
                if (parts.length > 1) {
                    String clientId = parts[1];
                    disconnectClientById(clientId);
                } else {
                    System.out.println("Please provide the client ID.");
                }
            }
        }
    }

    private static void initializeDatabase() {
        addTheme("theme1");
        addNews("theme1", "NewsName1", "Newstext1 newstext newstext newstext newstext newstext newstext newstext newstext newstext\n" +
                                                        "newstext newstext newstext newstext newstext newstext newstext newstext newstext");
        addNews("theme1", "NewsName2", "Newstext2 newstext newstext newstext newstext newstext newstext newstext newstext newstext\n" +
                "newstext newstext newstext newstext newstext newstext newstext newstext newstext");

        addTheme("theme2");
        addNews("theme2", "NewsName3", "Newstext3 newstext newstext newstext newstext newstext newstext newstext newstext newstext\n" +
                "newstext newstext newstext newstext newstext newstext newstext newstext newstext");
        addNews("theme2", "NewsName4", "Newstext4 newstext newstext newstext newstext newstext newstext newstext newstext newstext\n" +
                "newstext newstext newstext newstext newstext newstext newstext newstext newstext");
    }

    private static void addTheme(String theme) {
        newsDatabase.putIfAbsent(theme, new ConcurrentHashMap<>());
    }

    private static void addNews(String theme, String name, String text) {
        String newsId = generateNewsId();
        newsDatabase.computeIfAbsent(theme, k -> new ConcurrentHashMap<>()).put(newsId, name + ": " + text);
    }

    private static synchronized String generateNewsId() {
        return String.valueOf(newsIdCounter++);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientId;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("Client " + clientId + " connected from " + socket.getInetAddress());

                while (true) {
                    String request = (String) in.readObject();
                    String response = handleRequest(request);
                    out.writeObject(response);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client " + clientId + " disconnected.");
            }
        }

        private String handleRequest(String request) {
            String[] parts = request.split(" ", 2);

            if (request.equals("exit")) {
                try {
                    socket.close();
                    return "Disconnected from server.";
                } catch (IOException e) {
                    return "Error while disconnecting.";
                }
            }

            if (request.equals("help")) {
                return """
                        
                        Available requests:
                        help - get list of available requests
                        exit - disconnect
                        themes - get list of all themes
                        newsbytheme [Theme] - get list of news by theme
                        newstext [ID] - get text of news by ID
                        addtheme [Theme] - add new theme
                        addnews [Theme] [Name] [Text] - add news by entering theme, name and text
                        """;
            }

            if (request.equals("themes")) {
                return String.join("\n", newsDatabase.keySet());
            }

            if (parts[0].equals("newsbytheme") && parts.length > 1) {
                String theme = parts[1].trim();
                Map<String, String> newsMap = newsDatabase.get(theme);

                if (newsMap == null) {
                    return "No news for this theme";
                }

                StringBuilder response = new StringBuilder();
                for (Map.Entry<String, String> entry : newsMap.entrySet()) {
                    response.append("id").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                return response.toString();
            }

            if (parts[0].equals("newstext") && parts.length > 1) {
                String id = parts[1].trim();
                return getNewsTextById(id);
            }

            if (parts[0].equals("addtheme") && parts.length > 1) {
                String theme = parts[1].trim();
                addTheme(theme);
                return "Theme added: " + theme;
            }

            if (parts[0].equals("addnews") && parts.length > 1) {
                String[] newsParts = parts[1].split(" ", 3);
                if (newsParts.length < 3) {
                    return "Error: Provide theme, name, and text for the news.";
                }
                String theme = newsParts[0].trim();
                String name = newsParts[1].trim();
                String text = newsParts[2].trim();
                addNews(theme, name, text);
                return "News added under theme: " + theme;
            }

            return "No requests with such name: " + request;
        }

        private String getNewsTextById(String id) {
            for (Map.Entry<String, Map<String, String>> entry : newsDatabase.entrySet()) {
                if (entry.getValue().containsKey(id)) {
                    return entry.getValue().get(id);
                }
            }
            return "News not found for ID: " + id;
        }

        public void disconnect() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
