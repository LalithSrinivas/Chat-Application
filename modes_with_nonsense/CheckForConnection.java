import java.io.*; 
import java.net.*; 
import java.util.regex.Pattern;


class CheckForConnection implements Runnable{
		Socket connectionSocket;
		int mode;
		int count = 0;
		CheckForConnection(Socket socket, int mode ){
			this.connectionSocket = socket;
			this.mode = mode;
		}

		public boolean checkUsername(String username) {
			return Pattern.matches("\\w+", username);
		}

		public void run() {
			try {
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream  outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				//waits till it receives some message
				String requestSentence = null, endln, username = null;
				
				requestSentence= inFromClient.readLine();
				System.out.println(requestSentence);
				
				if(!requestSentence.substring(0, 16).equals("REGISTER TOSEND ") && !requestSentence.substring(0, 16).equals("REGISTER TORECV ")){
					outToClient.writeBytes(TCPServer.USER_NOTFOUND + "\n\n");
					this.connectionSocket.close();
					return;
				}
				
				endln = inFromClient.readLine();
				// if(!endln.equals("")){
				// 		outToClient.writeBytes(USER_NOTFOUND + "\n\n");
				// 		//inFromClient.reset();
				// 		//return;
				// }

				username = requestSentence.substring(16);
				
				if(requestSentence.subSequence(0, 16).equals("REGISTER TORECV ")) {//request to register to receive
					if(checkUsername(username) && !TCPServer.hasUserOutSocket(username)) {
						TCPServer.addUserOutSocket(username, connectionSocket);
						outToClient.writeBytes("REGISTERED TORECV "+username+"\n\n");
						System.out.println("Registered to receive " + username);
						MessageAck initConvA = new MessageAck(username);
						initConvA.run();
					}
					else {
						outToClient.writeBytes(TCPServer.MALFORMED_USERNAME +"\n\n");
						this.run();
					}
					return;
				}
				else if(requestSentence.subSequence(0, 16).equals("REGISTER TOSEND ")) {//request to register tosend
					if(checkUsername(username) && !TCPServer.hasUserInSocket(username)) {
						TCPServer.addUserInSocket(username, connectionSocket);
						outToClient.writeBytes("REGISTERED TOSEND "+username+"\n\n");
						System.out.println("Registered to send " + username);
					}
					else {
						outToClient.writeBytes(TCPServer.MALFORMED_USERNAME +"\n\n");
						//inFromClient.reset();
						//return;
					}

					// System.out.println("yo1 " );
					// requestSentence = inFromClient.readLine();
				
					// endln = inFromClient.readLine();
					// if(!endln.equals("")){
					// 	outToClient.writeBytes(MALFORMED_REQUEST + "\n\n");
					// 		//inFromClient.reset();
					// 	//return;
					// }

				
					int clientMode = -1;
					//if client mode and server mode aren't matching request for new mode 3 times
					//if modes don't match even after 3 times detroy socket;
					for(int i =0; i<TCPServer.REQUESTS_LIMIT; i++){
						requestSentence = inFromClient.readLine();System.out.println(requestSentence);
						endln = inFromClient.readLine();
						
						if(!requestSentence.substring(0, 5).equals("MODE ") || !endln.equals("")){
						// 	outToClient.writeBytes(MALFORMED_REQUEST + "\n\n");
						// 	//inFromClient.reset();
						// 	// run()
						// 	// return;
						// }
				
						// if(!endln.equals("")){
							outToClient.writeBytes(TCPServer.MALFORMED_REQUEST + "\n\n");
							return;
						//inFromClient.reset();
						//return;
						}
						clientMode = Integer.parseInt(requestSentence.substring(5));

						if(clientMode != this.mode){
							outToClient.writeBytes(TCPServer.INCOMPATIBLE_MODE + "\n\n");
						//System.out.println("request");
					//inFromClient.reset();
					//return;
						} else{
							outToClient.writeBytes("RUNNING ON COMPATIBLE MODE\n\n");
							break;
						}
					}
				
					if(clientMode != this.mode){
						TCPServer.removeUser(username);
						return;
					}
				
					

				// if(mode == UNENCRYP_MODE)
				// 	return;

				

					if(this.mode != TCPServer.UNENCRYP_MODE){
						for(int i =0; i<TCPServer.REQUESTS_LIMIT; i++){
							requestSentence = inFromClient.readLine();System.out.println(requestSentence);
							endln = inFromClient.readLine();
							
							if(!requestSentence.substring(0, 10).equals("PUBLICKEY ")||!endln.equals("")){
							outToClient.writeBytes(TCPServer.MALFORMED_REQUEST + "\n\n");
					// //inFromClient.reset();
					// //return;
					// 	}

					// 	if(!endln.equals("")){
					// 		outToClient.writeBytes(MALFORMED_REQUEST + "\n\n");
					// //inFromClient.reset();
					// //return;
							}
							String publicKey = requestSentence.substring(10);
							if(TCPServer.hasUserPublicKey(username)){
								outToClient.writeBytes(TCPServer.MALFORMED_REQUEST + "\n\n");
								TCPServer.removeUser(username);
								return;
					//inFromClient.reset();
					//return;
							}
						
							TCPServer.addUserPublicKey(username, publicKey);
						}							
					}
				

					MessageForwarding initConvF = new MessageForwarding(username);
					initConvF.run();	
				} 
				
				
				
			}
			catch(Exception e) {
				System.out.println("Error at Server while accepting request: "+ e);
				try{
					//outToClient.writeBytes(MALFORMED_REQUEST + "\n\n");
					connectionSocket.close();
				} catch (IOException e1) {
					System.out.println(e1);
					return;
				}
			}
		}
	}
	