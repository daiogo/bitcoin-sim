/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bitcoin;

import java.io.Serializable;
import java.security.PublicKey;

/**
 *
 * @author Diogo
 */
public class UserInformation implements Serializable {
    private String username;
    private int coins;
    private PublicKey publicKey;
    private int unicastPort;
    private String coinPrice;
    
    
    public UserInformation(String username, int coins, String coinPrice,
            int unicastPort, PublicKey publicKey) {
        this.username = username;
        this.coins = coins;
        this.publicKey = publicKey;
        this.unicastPort = unicastPort;
        this.coinPrice = coinPrice;
        this.unicastPort = unicastPort;
    }

    public String getUsername() {
        return username;
    }
    
    public String getCoinPrice() {
        return coinPrice;
    }

    public int getCoins() {
        return coins;
    }
    
    public int getUnicastPort() {
        return unicastPort;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
    
    public void setUnicastPort(int unicastPort) {
        this.unicastPort = unicastPort;
    }
}
