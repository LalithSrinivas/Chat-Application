import java.io.*; 
import java.net.*;

import java.security.MessageDigest;

class InputFromServer implements Runnable {
	public static final String ALGORITHM = "RSA";
	Socket connectionSocket;
	BufferedReader inFromServer;
	DataOutputStream outToServer;
	byte[] privateKey;
	int mode;

	InputFromServer (Socket connectionSocket,  byte[] privateKey, int mode){ //, BufferedReader inFromServer) {  }
		this.connectionSocket = connectionSocket;
		this.privateKey = privateKey;
		this.mode = mode;
		try{
			this.inFromServer = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
			this.outToServer = new DataOutputStream(this.connectionSocket.getOutputStream());
		} catch(Exception e){
			e.printStackTrace();
			System.out.println(e+" IFS constructor");
		}
		
	} 
	
	public void run() {
		while(true) {
			try {
				boolean flag = true;
				int contentLength = 0;
				String senderUsername = null, senderUsernameLine = null, contentLengthLine = null , encryptedMsg;
				//do{

					System.out.println("receiving starts " );
					senderUsernameLine = inFromServer.readLine();
				// }while(senderUsernameLine == null);
					System.out.println("serverMsg: "  + senderUsernameLine);
				if(senderUsernameLine.length()>=8 && senderUsernameLine.substring(0, 8).equals("FORWARD "))
					senderUsername = senderUsernameLine.substring(8);
				else{
					continue;
					//flag = false;
				}
//                System.out.println("sender name: "+senderUsername);
				// do{
					contentLengthLine = inFromServer.readLine();System.out.println("serverMsg: "  + contentLengthLine);
				// }while(contentLengthLine == null);
				if(contentLengthLine.substring(0, 16).equals("Content-length: "))
					contentLength = Integer.parseInt(contentLengthLine.substring(16));
				else 
					flag = false;
//                System.out.println(contentLength);
				String endln = inFromServer.readLine();System.out.println("serverMsg: "  + endln);
				String hash = null;
				if(mode == TCPClient.ENCRYP_WITH_SIGN_MODE){
					hash = inFromServer.readLine();
					endln = inFromServer.readLine();	
				}
				char[] msgArr = new char[contentLength];
				int readFlag  = 0;
				
//				System.out.println((i<contentLength && yoflag != -1));
				System.out.print("chars:");
				for(int i =0; (i<contentLength && readFlag != -1); i++){
					char[]  temparr = new char[1];
					System.out.print(temparr[0]);
//                	System.out.println("statred");
					readFlag = inFromServer.read(temparr);
					if(temparr[0] == 0)
					{
					//	System.out.println(i);
						break;
					}
					msgArr[i] = temparr[0];
//                	System.out.println("char: "+temparr[0]);
				}
//                System.out.println();
				if(readFlag==-1)
					encryptedMsg = "";
				else
					encryptedMsg = new String(msgArr);
				System.out.println("\nserverMsg: "  + new String(msgArr));
				if(mode == TCPClient.ENCRYP_WITH_SIGN_MODE){
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
//  	              System.out.println("msg from server after key fetch " + serverMsg.substring(8));

					byte[] senderPublicKey = java.util.Base64.getDecoder().decode(serverMsg.substring(8));
//      	          System.out.println(encryptedMsg);
					byte[] hashMsgBytes = java.util.Base64.getDecoder().decode(hash);
					byte[] encryptedMsgBytes = java.util.Base64.getDecoder().decode(encryptedMsg);
	//                System.out.println(new String(encryptedMsgBytes)+" "+new String(privateKey));
					byte[] decryptedMsg  =  Cryptography.decrypt(privateKey, encryptedMsgBytes);
					byte[] decryptedhash =  Cryptography.decryptUsingPublic(senderPublicKey, hashMsgBytes);
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					byte[] shaBytes = md.digest(encryptedMsgBytes);
					if((new String(shaBytes)).equals(new String(decryptedhash) )) {
						System.out.println(senderUsername + ": " + new String(decryptedMsg));
						outToServer.writeBytes("RECEIVED " + senderUsername + "\n\n");
					}
				} else if(mode == TCPClient.ENCRYP_MODE) {
					byte[] encryptedMsgBytes = java.util.Base64.getDecoder().decode(encryptedMsg);
	//                System.out.println(new String(encryptedMsgBytes)+" "+new String(privateKey));
					byte[] decryptedMsg  =  Cryptography.decrypt(privateKey, encryptedMsgBytes);
					System.out.println(senderUsername + ": " + new String(decryptedMsg));
					outToServer.writeBytes("RECEIVED " + senderUsername + "\n\n");
				}

				else{
					System.out.println(senderUsername + ": " + encryptedMsg);
					outToServer.writeBytes("RECEIVED " + senderUsername + "\n\n");
				}
				
				
			}
			catch(Exception e) {
				e.printStackTrace();
				try {
					connectionSocket.close();
				} catch(Exception ee) { }
				System.out.println(e+" IFS");
				break;
			}
		} 
	}

}
