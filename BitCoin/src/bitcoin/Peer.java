/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoin;

import bitcoin.peerClient.MessageSender;
import bitcoin.peerServer.MessageListener;
import bitcoin.messages.BuyMessage;
import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author diego
 */
public class Peer {
    // Declare constants
    public static final int MIN_USERS = 4;
    public static final int MULTICAST_PORT = 6789;
    public static final String GROUP_IP = "228.5.6.7";
    
    // Peer atributes
    private Wallet wallet;
    private Database database;
    private Database intermediateDatabase;
    private UserInformation myUserInformation;
    private MessageSender messageSender;
    private MessageListener messageListener;
    private boolean exit;
    private String username;
    private int unicastPort;
    private String coinPrice;
    private PeerWindow peerWindow;
    private InetAddress group;
    private MulticastSocket multicastSocket = null;
    private DebugThread debugThread;
    
    static Semaphore mutex = new Semaphore(1);
    
    public Peer(String username, int unicastPort, String coinPrice) throws IOException, InterruptedException {
        this.username = username;
        this.unicastPort = unicastPort;
        this.wallet = new Wallet();

        mutex.acquire();
        
        File f = new File("./" + username + ".txt");
        if(f.exists() && !f.isDirectory()) { 
            readDatabaseFromFile();
            myUserInformation = database.getMyUserInformation();
        } else {
            System.out.println("File does not exists");
            this.database = new Database();
            this.intermediateDatabase = new Database();
            myUserInformation = new UserInformation(username, 100, coinPrice, unicastPort, this.wallet.getPublicKey());
            this.database.addUserInformation(myUserInformation);
            this.database.setMyUserInformation(myUserInformation);
            this.intermediateDatabase.addUserInformation(myUserInformation);
            this.intermediateDatabase.setMyUserInformation(myUserInformation);
        }
        
        System.out.println("Peer Constructor: " + myUserInformation.getUsername());
        peerWindow = new PeerWindow(myUserInformation, this);
        peerWindow.setVisible(true);
 
        this.coinPrice = coinPrice;
        peerWindow.updateDatabase(database);

        mutex.release();
    }
    
    
    public void initPeer() {
        try {
            // Sets group settings and join multicast group
            group = InetAddress.getByName(GROUP_IP);
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            multicastSocket.joinGroup(group);
            
            // Starts MessageListener multicast thread and UDP server
            messageListener = new MessageListener(multicastSocket, this);
            messageListener.start();
            messageListener.startUDPServer(unicastPort);
            
            // Sends Hello Message at the start to the multicast group
            messageSender = new MessageSender(multicastSocket);
            System.out.println("User " + username + " has just entered this group! Sending Hello Message!");
            sendMulticastMessage("hello");
            
            // Sets exit flag as false
            exit = false;
        } catch (UnknownHostException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
        //debugThread = new DebugThread(this);
        //debugThread.start();
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public void sendMulticastMessage(String command) {
        try {
            switch (command) {
                case "hello":
                    messageSender.sendHello(myUserInformation);
                    windowAddMessageSent("Hello");
                    break;
                case "exit":
                    messageSender.sendExit(myUserInformation);
                    exit = true;
                    messageListener.setExit(exit);
                    System.out.println("Exiting...");
                    windowAddMessageSent("Exit");
                    break;
                case "help":
                    System.out.println("Commands Help:");
                    break;
                default:
                    System.out.println("ERROR | Command not found");
                    break;
            }
        } catch (IOException ex) {
            Logger.getLogger(PeerWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendTransactionMessage(BuyMessage buyMessage) {
        try {
            messageSender.sendTransaction(buyMessage, wallet);
            windowAddMessageSent("Transaction");
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendUnicastMessage(String command, int unicastPort, String seller, int coins) {
        try {
            switch (command) {
                case "database":
                    messageSender.sendDatabase(database, unicastPort);
                    windowAddMessageSent("Database");
                    break;
                case "buy":
                    //if (database.getNumberOfUsers() >= MIN_USERS) {
                        BuyMessage buyMessage = new BuyMessage(coins, username, seller);
                        messageSender.sendBuy(buyMessage, unicastPort);
                    //} else {
                    //    System.out.println("ERROR | You may only perform a transaction when at least 4 users are in the network.");
                    //    System.out.println("      | There are currently " + database.getNumberOfUsers() + " users.");
                    //}
                    windowAddMessageSent("Buy");
                    break;
                default:
                    
            }    
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void exitProgram() throws IOException {
        // Exits group and close socket
        multicastSocket.leaveGroup(group);
        if (multicastSocket != null)
            multicastSocket.close();
    }
    
    
    // ----------------- Database Methods -----------------
    
    public void setDatabase(Database database) {
        // Uses mutex to prevent database corruption during update
        try {
            mutex.acquire();
            try {
                this.database = database;
                this.intermediateDatabase = database;
                peerWindow.updateDatabase(database);
                saveDatabaseToFile();
            } finally {
                mutex.release();
            }
        } catch(InterruptedException ie) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ie);
        }
    }
    
    public void databaseAddUserInformation(UserInformation userInformation) {
        // Uses mutex to prevent database corruption during update
        try {
            mutex.acquire();
            try {
                database.addUserInformation(userInformation);
                intermediateDatabase.addUserInformation(userInformation);
                peerWindow.updateDatabase(database);
                saveDatabaseToFile();
            } finally {
                mutex.release();
            }
        } catch(InterruptedException ie) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ie);
        }
    }
    
    public void databaseRemoveUserInformation(UserInformation userInformation) {
        try {
            mutex.acquire();
            try {
                database.removeUserInformation(userInformation);
                intermediateDatabase.removeUserInformation(userInformation);
                peerWindow.updateDatabase(database);
                saveDatabaseToFile();
            } finally {
                mutex.release();
            }
        } catch(InterruptedException ie) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ie);
        }
    }
   
    private void saveDatabaseToFile() {
        ObjectOutputStream oos = null;
        FileOutputStream fout = null;
        try{
            fout = new FileOutputStream("./" + username+".txt", true);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(database);
            oos.flush();    
            System.out.println("\nFile saved: "+username+".txt");
            database.printDatabase();
        } catch (Exception ex) {
        } finally {
            if(oos  != null){
                try {
                    oos.close();
                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }  
            if(fout  != null){
                try {
                    fout.close();
                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }  
        }
    }
    
    private void readDatabaseFromFile() throws IOException {
        ObjectInputStream objectinputstream = null;
        FileInputStream streamIn = null;
        try {
            streamIn = new FileInputStream("./" + username + ".txt");
            objectinputstream = new ObjectInputStream(streamIn);
            database = (Database) objectinputstream.readObject();
            intermediateDatabase = database;
            System.out.println("File read: "+username + ".txt");
            database.printDatabase();
        } catch (IOException | ClassNotFoundException e) {
        } finally {
            if(objectinputstream != null){
                objectinputstream.close();
            }          
            if(streamIn != null){
                streamIn.close();
            } 
        }
    }

    
    public String getUsername() {
        return username;
    }
    
    public void printDatabase() {
        database.printDatabase();
    }
    
    public Database getDatabase() {
        return database;
    }
    
    public void windowAddMessageSent(String messageSent){
        peerWindow.addMessageSentText(messageSent);
    }
    
    public void windowAddMessageReceived(String messageReceived){
        peerWindow.addMessageReceivedText(messageReceived);
    }
    
}