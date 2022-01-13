package com.npole.app;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static int uniqueId;
    private ArrayList<ClientThread> al;
    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;
    
    public Server(int port) {
        // console or gui
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        al = new ArrayList<ClientThread>();
    }
    
    public void sendToAll(String message) {
        for (ClientThread client: al)
            client.writeMsg(message);
    }
    
    public void start() {
        keepGoing = true;
        // create socket server and wait for connection requests
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            
            while(keepGoing) {
                display("Server waiting for Clients on port " + port + ".");
                
                Socket socket = serverSocket.accept();
                if(!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket);
                al.add(t);
                t.start();
            }
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); i++) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch (IOException ioE) {
                        // oof
                    }
                }
            }
            catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    
    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
            // oof
        }
    }
    
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        
        System.out.println(time);
    }
    
    private synchronized void broadcast(String message) {
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";

        System.out.print(messageLf);

        for (int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);
            if (!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }
    
    // client logs out using LOGOUT message
    synchronized void remove(int id) {
        for (int i = 0; i < al.size(); i++) {
            ClientThread ct = al.get(i);
            if (ct.id == id) {
                al.remove(i);
                return;
            }
        }
    }
    
    /*
    *  To run as a console application just open a console window and: 
    * > java Server
    * > java Server portNumber
    * If the port number is not specified 8000 is used
    */ 
    public static void main(String[] args) {
        int portNumber = 8000;
        switch(args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is > java Server [portNumber]");
                return;
        }
        
        // create a new server object and start it
        Server server = new Server(portNumber);
        server.start();
    }
    
    // one instance of this thread will run for each client
    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;
        
        // constructor
        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streans");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                display(username + " just connected.");
                
                //scan connected users
                sendToAll("serverResetUserList:");
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread ct = al.get(i);
                    sendToAll("ServerAddToUserList:" + ct.username);
                }
            }
            catch (IOException e) {
                display("Exception creating new Input/Output streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
                // oof
            }
            date = new Date().toString() + "\n";
        }
        
        public void run() {
            boolean keepGoing = true;
            while(keepGoing) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();
                
                switch(cm.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        sendToAll("ServerResetUserList:");
                        for (int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            sendToAll("ServerAddToUserList:" + ct.username);
                        }
                        break;
                }
            }
            remove(id);
            sendToAll("ServerResetUserList:");
            for (int i = 0; i < al.size(); ++i) {
                ClientThread ct = al.get(i);
                sendToAll("ServerAddToUserList:" + ct.username);
            }
            close();
        }
        
        private void close() {
            System.out.println("Closed Connection");
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {
                // oof
            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {
                // oof
            }
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
                // oof
            }
        }
        
        // write a string to the client output stream
        public boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}
