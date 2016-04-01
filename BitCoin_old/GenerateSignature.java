package BitCoin;

import java.io.*;
import java.security.*;

class GenerateSignature {

    private PrivateKey privateKey; 
    private PublicKey publicKey;
    
    public GenerateSignature(){
        generateKeys();
        
    }
    
    private void generateKeys(){
        
        /* Generate a DSA signature */
        try{
            /* Generate a key pair */

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

            keyGen.initialize(1024, random);

            KeyPair pair = keyGen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
            
            /* Save the public key in a file */
            byte[] key = publicKey.getEncoded();
            FileOutputStream keyfos = new FileOutputStream("publicKey");
            keyfos.write(key);

            keyfos.close();
            
        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }
    }
    
    public void signFile(String nameOfFileToSign) {

        try {
            /* Create a Signature object and initialize it with the private key */

            Signature dsa = Signature.getInstance("SHA1withDSA", "SUN"); 

            dsa.initSign(privateKey);

            /* Update and sign the data */

            FileInputStream fis = new FileInputStream(nameOfFileToSign);
            BufferedInputStream bufin = new BufferedInputStream(fis);
            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                dsa.update(buffer, 0, len);
            };

            bufin.close();

            /* Now that all the data to be signed has been read in, 
                    generate a signature for it */

            byte[] realSig = dsa.sign();

        
            /* Save the signature in a file */
            FileOutputStream sigfos = new FileOutputStream("sig");
            sigfos.write(realSig);

            sigfos.close();

        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }

    }

}
