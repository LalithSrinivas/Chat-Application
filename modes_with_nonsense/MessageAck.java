import java.io.*; 
import java.net.*;
	
class MessageAck implements Runnable{
	String sender_username;
	
	MessageAck(String username) {
		this.sender_username = username;
	}
	
	public void run() {
//    		Socket connectionSocket = ;
		//System.out.println("Message ack created");
		BufferedReader inFromClient;

		try {
			inFromClient = new BufferedReader(new InputStreamReader(TCPServer.getUserOutSocket(sender_username).getInputStream()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			return;
		}
		DataOutputStream clientAck;

		try{
			clientAck = new DataOutputStream(TCPServer.getUserOutSocket(sender_username).getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			return;
		}
		while(true) {
			try {
				String recipientInfo = inFromClient.readLine();
				int count = 0;
					// while(recipientInfo == null) {
					// 	recipientInfo = inFromClient.readLine();
					// 	count++;
					// 	if(count == 2) {
					// 		usernameOutSocketPair.get(sender_username).close();
					// 		usernameOutSocketPair.remove(sender_username);
					// 		System.out.println("removed "+sender_username);
					// 		return;
					// 	}
					// }
				System.out.println("recipient info, receiver thread: "+recipientInfo);
				if(recipientInfo.subSequence(0, 8).equals("RECEIVED")) {
					inFromClient.readLine();
					String sender = recipientInfo.substring(9);
					Socket outsocket = TCPServer.getUserInSocket(sender);
					DataOutputStream  outToClient = new DataOutputStream(outsocket.getOutputStream());
					outToClient.writeBytes("SENT "+sender_username+"\n\n");
				}
				else if(TCPServer.mode == TCPServer.ENCRYP_WITH_SIGN_MODE && recipientInfo.substring(0, 8).equals("FETCHKEY")) {
					String receiver_username = recipientInfo.substring(9);
					String endln = inFromClient.readLine();
					
					while(endln == null){
						endln = inFromClient.readLine();
					}
					if(!endln.equals("")){
							//System.out.println("1");
						clientAck.writeBytes(TCPServer.MALFORMED_REQUEST+"\n\n");
						continue;
					}else{
							//System.out.println("2");
						clientAck.writeBytes("SENTKEY " + TCPServer.getUserPublicKey(receiver_username) + "\n\n");
					}
				}
			}
			catch(Exception e) {
				System.out.println(e);
			}
		}
	}
}
 
 

