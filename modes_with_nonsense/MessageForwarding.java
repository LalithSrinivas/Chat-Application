import java.io.*; 
import java.net.*; 

class MessageForwarding implements Runnable{
	String sender_username;
		
	MessageForwarding(String username) {
		this.sender_username = username;
	}
		
	public void run() {
		BufferedReader inFromClient;
		DataOutputStream clientAck;

		//System.out.println("Message ack created");
		
		try {
			inFromClient = new BufferedReader(new InputStreamReader(TCPServer.getUserInSocket(sender_username).getInputStream()));
			clientAck = new DataOutputStream(TCPServer.getUserInSocket(sender_username).getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
			// TODO Auto-generated catch block
			return;
		}
			
			
		while(true) {
			try {
				System.out.println("-1");
				if(TCPServer.mode != TCPServer.UNENCRYP_MODE){
					String keyRequest = inFromClient.readLine();
					int count=0;
					// while(keyRequest == null) {
						// 	keyRequest = inFromClient.readLine();
						// 	count++;
						// 	if(count == 2) {
						// 		usernameInSocketPair.get(sender_username).close();
						// 		usernameInSocketPair.remove(sender_username);
						// 		System.out.println("removed "+sender_username);
						// 		return;
						// 	}
						// }
					
					if(!keyRequest.substring(0, 8).equals("FETCHKEY")){
						clientAck.writeBytes(TCPServer.MALFORMED_REQUEST + "\n\n");
						continue;
					}

					String receiver_username = keyRequest.substring(9);
					String endln = inFromClient.readLine();
					//System.out.println(endln + " hi");

						// while(endln == null){
						// 	endln = inFromClient.readLine();
						// }
					
					if(!endln.equals("")){
							//System.out.println("1");
						clientAck.writeBytes(TCPServer.MALFORMED_REQUEST + "\n\n");
						continue;
					}else{
							//System.out.println("2");
						clientAck.writeBytes("SENTKEY " + TCPServer.getUserPublicKey(receiver_username) + "\n\n");
					}

				}
					
					
				   // String keyAck = 
				System.out.println("0");
				String recipientInfo = inFromClient.readLine();
				System.out.println("1");

				int count = 0;
					// while(recipientInfo == null) {
					// 	recipientInfo = inFromClient.readLine();
					// 	count++;
					// 	if(count == 2) {
					// 		usernameInSocketPair.get(sender_username).close();
					// 		usernameInSocketPair.remove(sender_username);
					// 		System.out.println("removed "+sender_username);
					// 		return;
					// 	}
					// }
//                    System.out.println(recipientInfo);
				String messageLength = inFromClient.readLine();System.out.println("2");
				int len = Integer.parseInt(messageLength.substring(16));
					//                        System.out.println("message length: " + len);
				String message = inFromClient.readLine();System.out.println("3");

				String hash =null;

				if(TCPServer.mode == TCPServer.ENCRYP_WITH_SIGN_MODE){
					hash = inFromClient.readLine();	System.out.println("4");
					message = inFromClient.readLine();System.out.println("5");
				} 
				
				char[] msgArr = new char[len];
					//	    				int check = inFromClient.read(msgArr, 0, len);
					//                        System.out.println(check);
				int flag  = 0;
				for(int i =0; i<len && flag != -1; i++){
					char[]  temparr = new char[1];
					flag = inFromClient.read(temparr);
					if(temparr[0] == 0)
					{
						System.out.println(i);
						break;
					}
					msgArr[i] = temparr[0];
				}
				if(flag==-1)
					message = "";
				else
					message = new String(msgArr);
				System.out.println("message: " + message);
				if(recipientInfo.substring(0, 4).equals("SEND")) {
//                   	System.out.println("message: "+message);
					String recipientName = recipientInfo.substring(5);
					Socket outsocket = TCPServer.getUserOutSocket(recipientName);
					if(outsocket != null) {
						DataOutputStream  outToClient = new DataOutputStream(outsocket.getOutputStream());
						try {
							if(messageLength.substring(0, 15).equals("Content-length:")) {
								int length = Integer.parseInt(messageLength.substring(16));
								if(TCPServer.mode!=TCPServer.ENCRYP_WITH_SIGN_MODE)
									outToClient.writeBytes("FORWARD "+sender_username+"\n"+"Content-length: "+length+"\n\n"+message);
                   				else
                   					outToClient.writeBytes("FORWARD "+sender_username+"\n"+"Content-length: "+length+"\n\n"+hash+"\n\n"+message);
							}
							else {
								clientAck.writeBytes(TCPServer.HEADER_INCOMPLETE+"\n\n");
								continue;
							}
						}
						catch (Exception e) {
							System.out.println(e);
							clientAck.writeBytes(TCPServer.HEADER_INCOMPLETE+ "\n\n");
							continue;
								// TODO: handle exception
						}
					}
					else {
						clientAck.writeBytes(TCPServer.UNABLE_TOSEND+"\n\n");
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				System.out.println(e);
			}
		}
	}
}