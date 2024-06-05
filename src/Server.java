import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    // Create an execution pool for the Threads
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }
    @Override
    public void run() {
        try {
            server = new ServerSocket(1234);
            pool = Executors.newCachedThreadPool();

            while(!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadCastMessage(String message) {
        for(ConnectionHandler ch : connections) {
            if(ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        done = true;
        pool.shutdown();
        try {
            if(!server.isClosed()) {
                server.close();

                for (ConnectionHandler ch : connections) {
                    ch.shutdown();
                }
            }
        } catch (IOException e) {
            //Ignore
        }
    }
    class ConnectionHandler implements Runnable {

        private Socket client;
        //To read and write from the client
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        public ConnectionHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Enter the Nickname");
                nickname = in.readLine();
                if(nickname.isBlank()) {
                    out.print("String Empty");
                    //Todo: Handle exception here;
                }

                //Server Log
                System.out.println(nickname + " Connected!");
                broadCastMessage(nickname + " Joined the chat!");

                String message;
                while((message = in.readLine()) != null) {
                    if(message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ",2);
                        if(messageSplit.length == 2) {
                            broadCastMessage(nickname+" renamed themselves to " + messageSplit[1]);
                            //server Log
                            System.out.println(nickname+" renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully nickname changed to "+ nickname);
                        }else{
                            out.print("No nickname provided!");
                        }
                    }else if(message.startsWith("/quit")) {
                        broadCastMessage(nickname+" Left the chat!");
                        shutdown();
                    }else  {
                        broadCastMessage(nickname + ": " + message);
                    }
                }
            }catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();

                if(!server.isClosed()) {
                    server.close();
                }
            }catch (IOException e) {
                //ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
