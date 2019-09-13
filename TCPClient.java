import java.io.*; 
import java.net.*;
import java.util.Base64;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

//import com.sun.org.apache.bcel.internal.generic.NEW;


class TCPClient { 
    public static final String ALGORITHM = "RSA";

    public static KeyPair generateKeyPair()
    		throws NoSuchAlgorithmException, NoSuchProviderException {
    	
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
    	
    	SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    	
    	// 512 is keysize
    	keyGen.initialize(512, random);
    	
    	KeyPair generateKeyPair = keyGen.generateKeyPair();
    	return generateKeyPair;
    }

    public static void main(String argv[]) throws Exception 
    { 
        KeyPair keyPair = generateKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKey = keyPair.getPrivate().getEncoded();
        String publicKeyString  = java.util.Base64.getEncoder().encodeToString(publicKey);

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
        System.out.println("Enter user name:");
        String username = inFromUser.readLine();
        while(username == null)
            username = inFromUser.readLine();
//        System.out.println();
        System.out.println("Enter server ip address:");
        String ipAddr = inFromUser.readLine();
        Socket sendMessageSocket = new Socket(ipAddr, 6789);
        DataOutputStream outToServerSender = new DataOutputStream(sendMessageSocket.getOutputStream());         
        BufferedReader inFromServerSender = new BufferedReader(new InputStreamReader(sendMessageSocket.getInputStream())); 
        
        boolean flag = false, acks = false, ackr = false;
        do{
            if(flag)
                System.out.println("Invalid user name. Enter again:\n");
            if(!flag)
            	flag = true;
            
            String temp = null;
//            System.out.println("REGISTER TOSEND " + username +" " + publicKeyString + " " + publicKeyString.length() + "\n\n");
            
            outToServerSender.writeBytes("REGISTER TOSEND " + username +" " + publicKeyString + "\n\n");
            while(temp == null)
                temp = inFromServerSender.readLine();
            acks = temp.equals("REGISTERED TOSEND "+ username);      
            do{
                temp = inFromServerSender.readLine();
            }while(temp == null);
            if(!temp.equals("")) 
                acks = false;
            if(acks)
                System.out.println("registered to send");
        }while(!acks);
        
        Socket recieveMessageSocket = new Socket(ipAddr, 6789);
        DataOutputStream outToServerReceiver = new DataOutputStream(recieveMessageSocket.getOutputStream());
        BufferedReader inFromServerReceiver = new BufferedReader(new InputStreamReader(recieveMessageSocket.getInputStream()));
        do{
            outToServerReceiver.writeBytes("REGISTER TORECV " + username + " " + publicKeyString+ "\n\n");
            String temp = inFromServerReceiver.readLine();
            while(temp == null)
                temp = inFromServerReceiver.readLine();
            ackr = temp.equals("REGISTERED TORECV " + username);
            do{
                temp = inFromServerReceiver.readLine();
            }while(temp == null);
            if(!temp.equals(""))
                ackr = false;
            if(acks)
                System.out.println("registered to receive");
        }while(!ackr);
	    outToServerReceiver.flush();
	    outToServerSender.flush();
        OutputToServer outputThread = new OutputToServer(sendMessageSocket, privateKey);//, outToServer, (inFromUser);
		InputFromServer inputThread = new InputFromServer(recieveMessageSocket, privateKey);//, (inFromServer);
        Thread outthread = new Thread(outputThread);
        Thread inthread = new Thread(inputThread);
        outthread.start();
        inthread.start();               
    }

} 

class InputFromServer implements Runnable {
	public static final String ALGORITHM = "RSA";
    Socket connectionSocket;
    BufferedReader inFromServer;
    DataOutputStream outToServer;
    byte[] privateKey;

    InputFromServer (Socket connectionSocket,  byte[] privateKey){ //, BufferedReader inFromServer) {  }
        this.connectionSocket = connectionSocket;
        this.privateKey = privateKey;
        try{
            this.inFromServer = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
            this.outToServer = new DataOutputStream(this.connectionSocket.getOutputStream());
        } catch(Exception e){
            System.out.println(e+" IFS constructor");
        }
        
    } 

    public static byte[] decrypt(byte[] privateKey, byte[] inputData)
    		throws Exception {
    	
    	PrivateKey key = KeyFactory.getInstance(TCPClient.ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    	
    	Cipher cipher = Cipher.getInstance(TCPClient.ALGORITHM);
    	cipher.init(Cipher.DECRYPT_MODE, key);
    	
    	byte[] decryptedBytes = cipher.doFinal(inputData);
    	
    	return decryptedBytes;
    }
    
    public static byte[] decryptUsingPublic(byte[] publicKey, byte[] inputData) throws Exception {
        PublicKey key = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(inputData);
        return decryptedBytes;
    }
    
    public void run() {
        while(true) {
            try {
                boolean flag = true;
                int contentLength = 0;
                String senderUsername = null, senderUsernameLine = null, contentLengthLine = null , encryptedMsg;
                do{
                    senderUsernameLine = inFromServer.readLine();
                }while(senderUsernameLine == null);
                if(senderUsernameLine.substring(0, 8).equals("FORWARD "))
                    senderUsername = senderUsernameLine.substring(8);
                else
                    flag = false;
//                System.out.println("sender name: "+senderUsername);
                do{
                    contentLengthLine = inFromServer.readLine();
                }while(contentLengthLine == null);
                if(contentLengthLine.substring(0, 16).equals("Content-length: "))
                    contentLength = Integer.parseInt(contentLengthLine.substring(16));
                else 
                    flag = false;
//                System.out.println(contentLength);
                String temp = inFromServer.readLine();
                String hash = inFromServer.readLine();
                temp = inFromServer.readLine();
				char[] msgArr = new char[contentLength];
				int yoflag  = 0;
				int i=0;
//				System.out.println((i<contentLength && yoflag != -1));
                for(i =0; (i<contentLength && yoflag != -1); i++){
                	char[]  temparr = new char[1];
//                	System.out.println("statred");
                	yoflag = inFromServer.read(temparr);
                	if(temparr[0] == 0)
                	{
                		System.out.println(i);
                		break;
                	}
                	msgArr[i] = temparr[0];
//                	System.out.println("char: "+temparr[0]);
                }
//                System.out.println();
                if(yoflag==-1)
                	encryptedMsg = "";
                else
                	encryptedMsg = new String(msgArr);
                outToServer.writeBytes("FETCHKEY " + senderUsername + "\n\n");

                //System.out.println("1");
                String serverMsg = inFromServer.readLine();
//                System.out.println("msg from server after key fetch "  + serverMsg);
                while(serverMsg == null)
                    serverMsg = inFromServer.readLine();
                if(!serverMsg.substring(0,8).equals("SENTKEY ")){
                    System.out.println("error occured");
                    continue;
                }
                inFromServer.readLine();
//                System.out.println("msg from server after key fetch " + serverMsg.substring(8));
                byte[] senderPublicKey = java.util.Base64.getDecoder().decode(serverMsg.substring(8));
//                System.out.println(encryptedMsg);
                byte[] hashMsgBytes = java.util.Base64.getDecoder().decode(hash);
                byte[] encryptedMsgBytes = java.util.Base64.getDecoder().decode(encryptedMsg);
//                System.out.println(new String(encryptedMsgBytes)+" "+new String(privateKey));
                byte[] decryptedMsg  =  decrypt(privateKey, encryptedMsgBytes);
                byte[] decryptedhash =  decryptUsingPublic(senderPublicKey, hashMsgBytes);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] shaBytes = md.digest(encryptedMsgBytes);
                if((new String(shaBytes)).equals(new String(decryptedhash) )) {
	                System.out.println(senderUsername + ": " + new String(decryptedMsg));
	                outToServer.writeBytes("RECEIVED " + senderUsername + "\n\n");
                }
            }
            catch(Exception e) {
                try {
                    connectionSocket.close();
                } catch(Exception ee) { }
                System.out.println(e+" IFS");
                break;
            }
        } 
    }

}

class OutputToServer implements Runnable {
	public static final String ALGORITHM = "RSA";
    Socket connectionSocket;
    DataOutputStream outToServer;
    BufferedReader inFromUser;
    BufferedReader inFromServer;
    byte[] privateKey;
   
    OutputToServer (Socket connectionSocket, byte[] privatekey){//, DataOutputStream outToServer, BufferedReader inFromUser) {
        this.connectionSocket = connectionSocket;
        this.inFromUser = new BufferedReader(new InputStreamReader(System.in));
        this.privateKey = privatekey;
        try{
            this.outToServer = new DataOutputStream(connectionSocket.getOutputStream());
            this.inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        } catch(Exception e){
            System.out.println(e + " OTS constructor");
        } 
    }

    public static byte[] encrypt(byte[] publicKey, byte[] inputData)
    		throws Exception {
    	PublicKey key = KeyFactory.getInstance(TCPClient.ALGORITHM)
    			.generatePublic(new X509EncodedKeySpec(publicKey));
    	
    	Cipher cipher = Cipher.getInstance(TCPClient.ALGORITHM);
    	cipher.init(Cipher.ENCRYPT_MODE, key);
    	
    	byte[] encryptedBytes = cipher.doFinal(inputData);
    	
    	return encryptedBytes;
    }
    
    public static byte[] encryptUsingPrivate(byte[] privateKey, byte[] inputData) throws Exception {        
        PrivateKey key = KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKey));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(inputData);
        return encryptedBytes;
    }
    
    public void run() {
        while(true) {
            try { 
                String clientSentence = inFromUser.readLine();
                while(clientSentence == null)
                    clientSentence = inFromUser.readLine();
                String[] sentence_split = clientSentence.split(" ", 2);

                if(sentence_split[0].charAt(0) != '@'){
                    System.out.println("invalid message format. retype the message:");
                    continue;
                }
                String user = sentence_split[0].substring(1);
                String msg;
//                System.out.println("message to: "+user);
                if(sentence_split.length == 1)
                    msg = "";
                else
                    msg = sentence_split[1];

                outToServer.writeBytes("FETCHKEY " + user + "\n\n");

                //System.out.println("1");
                String serverMsg = inFromServer.readLine();
//                System.out.println("msg from server after key fetch "  + serverMsg);
                while(serverMsg == null)
                    serverMsg = inFromServer.readLine();
                if(!serverMsg.substring(0,8).equals("SENTKEY ")){
                    System.out.println("error occured");
                    continue;
                }            
//                System.out.println("msg from server after key fetch " + serverMsg.substring(8));
                byte[] publicKey = java.util.Base64.getDecoder().decode(serverMsg.substring(8));
                serverMsg = inFromServer.readLine();

//                serverMsg = inFromServer.readLine();
//                while(serverMsg == null)
//                    serverMsg = inFromServer.readLine();
                if(!serverMsg.equals("")){
                    System.out.println("error occured");
                    continue;
                }
//                System.out.println(msg);
                byte[] encryptedMsgBytes = encrypt(publicKey, msg.getBytes());
                String encryptedMsg = java.util.Base64.getEncoder().encodeToString(encryptedMsgBytes);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] shaBytes = md.digest(encryptedMsgBytes);
                byte[] enchash = encryptUsingPrivate(privateKey, shaBytes);
                String hash = java.util.Base64.getEncoder().encodeToString(enchash);
                outToServer.writeBytes("SEND " + user + "\nContent-length: " + encryptedMsg.length() + "\n\n"+ hash +"\n\n" + encryptedMsg);
//                System.out.println("encryptedMsg sent to server :" + "SEND " + user + "\nContent-length: " + encryptedMsg.length() + "\n\n"+ encryptedMsg);
                serverMsg = inFromServer.readLine();
                inFromServer.readLine();
//                System.out.println("msg from server after msg sent "  + serverMsg);
                boolean ack = serverMsg.equals("SENT " + user);

                
                if(ack)
                    System.out.println("message sent sucessfully to " + user);
                else
                    System.out.println("message sending to " + user + " failed");
                serverMsg = inFromServer.readLine();
            } catch(Exception e) {
                try {
                    connectionSocket.close();
                } catch(Exception ee) { }
                System.out.println("outputToServer "+e);
                break;
            }
        } 
    }

}
