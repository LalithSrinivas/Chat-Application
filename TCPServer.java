import java.io.*; 
import java.net.*; 
import java.util.*;
import java.util.regex.Pattern;


class TCPServer {
	static HashMap<String, Socket> usernameInSocketPair = new HashMap<String, Socket>();
	static HashMap<String, Socket> usernameOutSocketPair = new HashMap<String, Socket>();
    public static void main(String argv[]) throws Exception {
    	//accept requests concurrently
    	while(true) {																		
    		ServerSocket welcomeSocket = new ServerSocket(6789);
    		Socket connectionSocket = welcomeSocket.accept();
    		TCPServer server = new TCPServer();
    		CheckForConnection connection = server.new CheckForConnection(connectionSocket);
    		Thread connectionsThread = new Thread(connection);
    		connectionsThread.start();
    	}
    }
    
    //for registering the user
    class CheckForConnection implements Runnable{
    	Socket connectionSocket;
    	CheckForConnection(Socket socket){
    		this.connectionSocket = socket;
    	}
    	public boolean checkUsername(String username) {
    		return Pattern.matches("\\w+", username);
    	}
    	public void run() {
    		try {
    			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
    			DataOutputStream  outToClient = new DataOutputStream(connectionSocket.getOutputStream());
    			//waits till it receives some message
    			String requestSentence = inFromClient.readLine();

    			while(requestSentence == null)
    				requestSentence = inFromClient.readLine();

    			//waits for "\n"
    			boolean endln = false;

    			while(!endln) {
    				if (inFromClient.readLine() == "")
    					endln = !endln;
    			}

    			String in_username = requestSentence.substring(16);

    			if(requestSentence.substring(0, 15).equals("REGISTER TOSEND")) {
    				if(checkUsername(in_username)) {
    					usernameInSocketPair.put(in_username, connectionSocket);
    					outToClient.writeBytes("REGISTERED TOSEND "+in_username+"\n");
    				}
    				else {
    					outToClient.writeBytes("ERROR 100 Malformed username\n\n");
    					return;
    				}
    			}

    			requestSentence = inFromClient.readLine();

    			while(requestSentence == null)
    				requestSentence = inFromClient.readLine();

    			System.out.println(requestSentence);

    			if(requestSentence.substring(0, 15).equals("REGISTER TORECV")) {
    				String username = requestSentence.substring(16);
    				if(checkUsername(username)) {
    					usernameOutSocketPair.put(username, connectionSocket);
    					outToClient.writeBytes("REGISTERED TORECV "+username+"\n");
    				}
    				else {
    					outToClient.writeBytes("ERROR 100 Malformed username\n\n");
    					return;
    				}
    			}

    			MessageForwarding initConv = new MessageForwarding(in_username);
    			Thread conv = new Thread(initConv);
    			conv.start();   				
    		}
    		catch(Exception e) {
    			System.out.println("Error at Server while accepting request: "+ e);
    			try {
    				connectionSocket.close();
    			} catch (IOException e1) {
    				System.out.println(e1);
    			}
    		}
    	}
    }
    
    //this runs after creating the user correctly..
    class MessageForwarding implements Runnable{
    	String sender_username;
    	
    	MessageForwarding(String username) {
			this.sender_username = username;
		}
    	
    	public void run() {
    		Socket connectionSocket = usernameInSocketPair.get(sender_username);
    		BufferedReader inFromClient;
    		
			try {
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				return;
			}
			
    		DataOutputStream clientAck;
			
    		try {
				clientAck = new DataOutputStream(connectionSocket.getOutputStream());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				return;
			}
			
    		System.out.println("started "+sender_username);
    		
    		while(true) {
    			try {
    				String recipientInfo = inFromClient.readLine();
    				
    				while(recipientInfo == null)
    					recipientInfo = inFromClient.readLine();
    				
    				System.out.println(recipientInfo);
    				String messageLength = inFromClient.readLine();
    				
    				while(messageLength == null)
    					messageLength = inFromClient.readLine();
    				String message = inFromClient.readLine();
    				
    				while(message == null || message == "")
    					message = inFromClient.readLine();
    				
    				if(recipientInfo.substring(0, 4) == "SEND") {
    					String recipientName = recipientInfo.substring(5);
    					Socket outsocket = usernameOutSocketPair.get(recipientName);
    					
    					if(outsocket != null) {
    						DataOutputStream  outToClient = new DataOutputStream(outsocket.getOutputStream());
    						
    						try {
	    						if(messageLength.substring(0, 15) == "Content-length:") {
	    							int length = Integer.parseInt(messageLength.substring(16));
	    							message = message.substring(0, length);
	    							int count = 0;
	    							
	    							while(true) {
	    								count++;
	    								outToClient.writeBytes("FORWARD "+sender_username+"\n"+"Content-length: "+length+"\n"+"\n"+message);
	    								Socket inSocket = usernameInSocketPair.get(recipientName);
	    								BufferedReader rec = new BufferedReader(new InputStreamReader(inSocket.getInputStream()));
	    								String ack = rec.readLine();
	    								
	    								while(ack == null)
	    									ack = rec.readLine();
	    								
	    								if(ack.substring(0, 8) == "RECEIVED") {
	    									clientAck.writeBytes("SENT "+recipientName+"\n\n");
	    									break;
	    								}
	    								
	    								if(count == 5) {
	    									clientAck.writeBytes("ERROR 102 Unable to send\n\n");
	    									continue;
	    								}
	    							
	    							}
	    						}
    						}
    						
    						catch (Exception e) {
    							System.out.println(e);
    							clientAck.writeBytes("ERROR 103 Header incomplete\n\n");
    							continue;
								// TODO: handle exception
							}
    					}
    					
    					else {
    						clientAck.writeBytes("ERROR 102 Unable to send\n\n");
    					}
    				}
    			}
    			catch(Exception e) {
    				System.out.println(e);
    			}
    		} 
    	}
    }
} 
 

