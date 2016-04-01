/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoin;

import java.net.*;
import java.io.*;
import java.security.*;

import javax.crypto.*;

/**
 *
 * @author diego
 */
public class Peer {
    public static final int PORT = 6789;
    public static final String GROUP_IP = "228.5.6.7";
    
    private Wallet wallet;
    private SignatureVerifier signatureVerifier;
    private Database database;
    private UserInformation myUserInformation;
    private MessageSender sender;
    
    public Peer(String username){
        System.out.println("Peer Constructor");
        this.wallet = new Wallet();
        this.signatureVerifier = new SignatureVerifier();
        this.database = new Database();
        this.myUserInformation = new UserInformation(username, 100, this.wallet.getPublicKey());
    }
    
    public void test_signature(){
        
        byte [] signedFile = wallet.signFile("test_file.txt");
        signatureVerifier.verify(myUserInformation.getPublicKey(), signedFile, "test_file.txt");
        signatureVerifier.verify(myUserInformation.getPublicKey(), signedFile, "test_file2.txt");
    }
    
    public void start() {
        MulticastSocket s = null;
        
        try {
            // Sets group settings and join multicast group
            InetAddress group = InetAddress.getByName(GROUP_IP);
            s = new MulticastSocket(PORT);
            s.joinGroup(group);
            
            // Sends hello message to group
            //String helloMsg = "hello|" + myUserInformation.getPublicKey().toString() + "|" + myUserInformation.getCoins();
            String helloMsg = "hello," + "public key" + "," + myUserInformation.getCoins();
            this.sender = new MessageSender(s, helloMsg);
            sender.start();
            
            // Sends your wallet to group to construct database
            
            // Gets message from group
            byte[] buffer = new byte[1000];
            for (int i = 0; i < 4; i++) { // get messages from others in group
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                s.receive(messageIn);
                System.out.println("Received:" + new String(messageIn.getData()));
            }
            
            // Exit program
            s.leaveGroup(group);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            if (s != null)
                s.close();
        }
    }    
}

