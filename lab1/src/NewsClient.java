import java.io.*;
import java.net.*;
import java.util.Scanner;

public class NewsClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Scanner scanner = new Scanner(System.in);
            System.out.println("Connected to News Server.");

            while (true) {
                System.out.print("Enter request (help for requests): ");
                String request = scanner.nextLine();
                out.writeObject(request);

                String response = (String) in.readObject();
                System.out.println(response);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Server disconnected.");
        }
    }
}
