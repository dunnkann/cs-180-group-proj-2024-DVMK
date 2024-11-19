import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.*;

public class UserManager implements UserManagerInterface, Runnable {
    private static final String USER_DATA_FILE = "users.txt"; // Store user data
    private AtomicInteger userIdCounter;
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String message;

    public UserManager(Socket client) {
        try {
            this.clientSocket = client;
            // Create the users.txt file if it does not exist
            new File(USER_DATA_FILE).createNewFile();
            
            // Initialize userIdCounter with the highest existing user ID + 1
            userIdCounter = new AtomicInteger(getHighestUserId() + 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream());

            while((message = reader.readLine()) != null) {
                if (message.equals("Register")) {
                    boolean good = this.registerUser(reader.readLine(), reader.readLine());
                    writer.write(Boolean.toString(good));
                    writer.println();
                    writer.flush();
                } else if (message.equals("Login")) {
                    boolean good = this.loginUser(reader.readLine(), reader.readLine());
                    writer.write(Boolean.toString(good));
                    writer.println();
                    writer.flush();
                }
            }
        } catch(IOException e) {

        }
    }
    public boolean registerUser(String username, String password) throws IOException {
        if (usernameExists(username)) {
            System.out.println("Username already exists.");
            return false;
        }
        if (usernameHasForbiddenCharacter(username) || usernameHasForbiddenCharacter(password)) {
            System.out.println("Username or password cannot have a comma or colon");
            return false;
        } 
        User newUser = new User(username, password, userIdCounter.get()); // Retrieve integer value from AtomicInteger
        userIdCounter.incrementAndGet(); // Increment userId after creating the user

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_DATA_FILE, true))) {
            writer.write(newUser.getUsername() + "," + newUser.getPassword() + "," + newUser.getUserId());
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Error writing user data: " + e.getMessage());
            return false;
        }
    }
    public boolean usernameHasForbiddenCharacter(String username) {
        if (username.contains(",") || username.contains(":")) {
            return true;
        }
        return false;
    }
    public boolean usernameExists(String username) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String storedUsername = line.split(",")[0];
                if (storedUsername.equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading user data: " + e.getMessage());
            throw e;
        }
        return false;
    }

    public boolean loginUser(String username, String password) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String storedUsername = parts[0];
                String storedPassword = parts[1];

                if (storedUsername.equals(username) && storedPassword.equals(password)) {
                    return true; // Login successful
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading user data: " + e.getMessage());
            throw e;
        }
        return false; // Invalid credentials
    }
    public int getTotalUsers() {
        return 1;
    }
    private int getHighestUserId() throws IOException {
        int maxId = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int userId = Integer.parseInt(parts[2]);
                if (userId > maxId) {
                    maxId = userId;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading user data: " + e.getMessage());
            throw e;
        } catch (NumberFormatException e) {
            System.err.println("Error parsing user ID: " + e.getMessage());
        }
        return maxId;
    }
}
