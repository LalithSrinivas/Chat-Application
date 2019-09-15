import java.io.*;
import java.net.*;

import java.security.MessageDigest;

class OutputToServer implements Runnable {
	public static final String ALGORITHM = "RSA";
	Socket connectionSocket;
	DataOutputStream outToServer;
	BufferedReader inFromUser;
	BufferedReader inFromServer;
	byte[] privateKey;
	int mode;
   
	OutputToServer (Socket connectionSocket, byte[] privatekey, int mode){//, DataOutputStream outToServer, BufferedReader inFromUser) {
		this.connectionSocket = connectionSocket;
		this.inFromUser = new BufferedReader(new InputStreamReader(System.in));
		this.privateKey = privatekey;
		this.mode = mode;
		try{
			this.outToServer = new DataOutputStream(connectionSocket.getOutputStream());
			this.inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		} catch(Exception e){
			System.out.println(e + " OTS constructor");
		} 
	}

	
	public void run() {
		while(true) {
			try { 
				String clientSentence = inFromUser.readLine();
				// while(clientSentence == null)
				// 	clientSentence = inFromUser.readLine();
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
				System.out.println("message " + msg);
				if(mode != TCPClient.ENCRYP_WITH_SIGN_MODE){
					outToServer.writeBytes("SEND " + user + "\nContent-length: " + msg.length() + "\n\n" + msg);
					System.out.println("sent msg to server");	
				}
				else{
					outToServer.writeBytes("FETCHKEY " + user + "\n\n");

					//System.out.println("1");
					String serverMsg = inFromServer.readLine();
//      	          System.out.println("msg from server after key fetch "  + serverMsg);
					while(serverMsg == null)
						serverMsg = inFromServer.readLine();
					if(!serverMsg.substring(0,8).equals("SENTKEY ")){
						System.out.println("error occured");
						continue;
					}            
//              	  System.out.println("msg from server after key fetch " + serverMsg.substring(8));
					byte[] publicKey = java.util.Base64.getDecoder().decode(serverMsg.substring(8));
					serverMsg = inFromServer.readLine();

//               	 serverMsg = inFromServer.readLine();
//                	while(serverMsg == null)
//                  	  serverMsg = inFromServer.readLine();
					if(!serverMsg.equals("")){
						System.out.println("error occured");
						continue;
					}	

//                System.out.println(msg);
					byte[] encryptedMsgBytes = Cryptography.encrypt(publicKey, msg.getBytes());
					String encryptedMsg = java.util.Base64.getEncoder().encodeToString(encryptedMsgBytes);

					if(mode == TCPClient.ENCRYP_MODE){
						outToServer.writeBytes("SEND " + user + "\nContent-length: " + encryptedMsg.length() +"\n\n" + encryptedMsg);
					} else{
						MessageDigest md = MessageDigest.getInstance("SHA-256");
						byte[] shaBytes = md.digest(encryptedMsgBytes);
						byte[] enchash = Cryptography.encryptUsingPrivate(privateKey, shaBytes);
						String hash = java.util.Base64.getEncoder().encodeToString(enchash);
						outToServer.writeBytes("SEND " + user + "\nContent-length: " + encryptedMsg.length() + "\n\n"+ hash +"\n\n" + encryptedMsg);
					}

				}
				
				System.out.println("waiting for server.... " );
				 
				   // System.out.println("encryptedMsg sent to server :" + "SEND " + user + "\nContent-length: " + encryptedMsg.length() + "\n\n"+ encryptedMsg);
				String serverMsg = inFromServer.readLine();
				System.out.println("receive from server " + serverMsg);
				inFromServer.readLine();
				System.out.println("receive from server " + serverMsg);
//                System.out.println("msg from server after msg sent "  + serverMsg);
				boolean ack = serverMsg.equals("SENT " + user);

				
				if(ack)
					System.out.println("message sent sucessfully to " + user);
				else
					System.out.println("message sending to " + user + " failed");
				//serverMsg = inFromServer.readLine();
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
