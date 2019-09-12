import java.io.*; 
import java.net.*; 
import java.util.*;
import java.util.regex.Pattern;


class TCPServer {
	private static HashMap<String, Socket> usernameInSocketPair = new HashMap<String, Socket>();
	private static HashMap<String, Socket> usernameOutSocketPair = new HashMap<String, Socket>();
    private static HashMap<String, String> publicKeySocketPair = new HashMap<String, String>();
    public static void main(String argv[]) throws Exception {
    	//accept requests concurrently
    	ServerSocket welcomeSocket = new ServerSocket(6789);
    	while(true) {
//    		System.out.println("working");
    		Socket connectionSocket = welcomeSocket.accept();
//    		System.out.println("connection accepted");
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
    			
   			//System.out.println("debug: " + requestSentence);

    			//waits for "\n"
    			boolean endln = false;

    			while(!endln) {
    				String temp = inFromClient.readLine();
    				if (temp.equals(""))
    					endln = !endln;
    			}

       			//System.out.println(requestSentence);
                String[] requestSentenceWords = requestSentence.split(" ", 4);
                if(requestSentenceWords.length  != 4){
                    outToClient.writeBytes("ERROR 101 Malformed request\n\n");
                    //System.out.println("ERROR 101 Malformed request" + requestSentenceWords.length);
                    return;
                } 
                //System.out.println("1");
                String username = requestSentenceWords[2];
                String publicKey = requestSentenceWords[3];
//                 System.out.println(requestSentenceWords[0]);
//                 System.out.println(requestSentenceWords[3] + " "+ requestSentenceWords[3].length());
//                 System.out.println(requestSentenceWords[2]);   
    			if(requestSentence.subSequence(0, 15).equals("REGISTER TORECV")) {
    				if(checkUsername(username) && !usernameOutSocketPair.containsKey(username)) {
    					usernameOutSocketPair.put(username, connectionSocket);
                        //System.out.println("2");
                        if(!publicKeySocketPair.containsKey(username))
                            publicKeySocketPair.put(username, publicKey);
    					outToClient.writeBytes("REGISTERED TORECV "+username+"\n\n");
    					MessageAck initConv = new MessageAck(username);
                        System.out.println("Registered to receive " + username);
    					initConv.run();
    				}
    				else {
    					outToClient.writeBytes("ERROR 100 Malformed username\n\n");
    					return;
    				}
    			}
    			else if(requestSentence.subSequence(0, 15).equals("REGISTER TOSEND")) {
    				if(checkUsername(username) && !usernameInSocketPair.containsKey(username)) {
    					usernameInSocketPair.put(username, connectionSocket);
                        if(!publicKeySocketPair.containsKey(username))
                            publicKeySocketPair.put(username, publicKey);
    					outToClient.writeBytes("REGISTERED TOSEND "+username+"\n\n");
                        System.out.println("Registered to send " + username);
                        // outToClient.close();
//    					inFromClient.close();
//    					System.out.println("REGISTERED TOSEND "+in_username+"\n");
    					MessageForwarding initConv = new MessageForwarding(username);
    					initConv.run();
    				}
    				else {
    					outToClient.writeBytes("ERROR 100 Malformed username\n\n");
    					return;
    				}
    			} else{
    				outToClient.writeBytes("ERROR 101 Malformed request");
                    System.out.println("ERROR 101 Malformed request");
                }

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
//    		Socket connectionSocket = ;
    		BufferedReader inFromClient;
			try {
				inFromClient = new BufferedReader(new InputStreamReader(usernameInSocketPair.get(sender_username).getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				return;
			}
			
    		DataOutputStream clientAck;
			
    		try {
				clientAck = new DataOutputStream(usernameInSocketPair.get(sender_username).getOutputStream());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				return;
			}
			
    		
    		while(true) {
    			try {
    				String keyRequest = inFromClient.readLine();
    				int count=0;
    				while(keyRequest == null) {
    					keyRequest = inFromClient.readLine();
    					count++;
                    	if(count == 2) {
                    		usernameInSocketPair.get(sender_username).close();
                    		usernameInSocketPair.remove(sender_username);
                    		System.out.println("removed "+sender_username);
                    		return;
                    	}
    				}
    				
                    if(!keyRequest.substring(0, 8).equals("FETCHKEY")){
                        clientAck.writeBytes("Improper request\n\n");
                        continue;
                    }
                    String receiver_username = keyRequest.substring(9);
                    String endln = inFromClient.readLine();
                    //System.out.println(endln + " hi");

                    while(endln == null){
                        endln = inFromClient.readLine();
                    }
                    if(!endln.equals("")){
                        //System.out.println("1");
                        clientAck.writeBytes("Improper request\n\n");
                        continue;
                    }else{
                        //System.out.println("2");
                        clientAck.writeBytes("SENTKEY " + publicKeySocketPair.get(receiver_username) + "\n\n");
                    }

                   // String keyAck = 

                    String recipientInfo = inFromClient.readLine();
                    
                    count = 0;
                    while(recipientInfo == null) {
                    	recipientInfo = inFromClient.readLine();
                    	count++;
                    	if(count == 2) {
                    		usernameInSocketPair.get(sender_username).close();
                    		usernameInSocketPair.remove(sender_username);
                    		System.out.println("removed "+sender_username);
                    		return;
                    	}
                    }
//                    System.out.println(recipientInfo);
                    String messageLength = inFromClient.readLine();
                    int len = Integer.parseInt(messageLength.substring(16));
                    //                        System.out.println("message length: " + len);
                    String message = inFromClient.readLine();
                    char[] msgArr = new char[len];
                    //	    				int check = inFromClient.read(msgArr, 0, len);
                    //                        System.out.println(check);
                    int yoflag  = 0;
                    for(int i =0; i<len && yoflag != -1; i++){
                    	char[]  temparr = new char[1];
                    	yoflag = inFromClient.read(temparr);
                    	if(temparr[0] == 0)
                    	{
                    		System.out.println(i);
                    		break;
                    	}
                    	msgArr[i] = temparr[0];
                    }
                    if(yoflag==-1)
                    	message = "";
                    else
                    	message = new String(msgArr);
                    if(recipientInfo.substring(0, 4).equals("SEND")) {
//                    	System.out.println("message: "+message);
                    	String recipientName = recipientInfo.substring(5);
                    	Socket outsocket = usernameOutSocketPair.get(recipientName);

                    	if(outsocket != null) {
                    		DataOutputStream  outToClient = new DataOutputStream(outsocket.getOutputStream());
                    		try {
                    			if(messageLength.substring(0, 15).equals("Content-length:")) {
                    				int length = Integer.parseInt(messageLength.substring(16));
                    				outToClient.writeBytes("FORWARD "+sender_username+"\n"+"Content-length: "+length+"\n\n"+message);
//                    				System.out.println("FORWARD TO "+receiver_username+" FROM "+ sender_username +" "+message);
                    			}
                    			else {
                    				clientAck.writeBytes("ERROR 103 Header incomplete\n\n");
//                    				System.out.println("doesn't match "+messageLength);
                    				continue;
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
    
    class MessageAck implements Runnable{
    	String sender_username;
    	
    	MessageAck(String username) {
			this.sender_username = username;
		}
    	
    	public void run() {
//    		Socket connectionSocket = ;
    		BufferedReader inFromClient;

    		try {
    			inFromClient = new BufferedReader(new InputStreamReader(usernameOutSocketPair.get(sender_username).getInputStream()));
    		} catch (IOException e1) {
    			// TODO Auto-generated catch block
    			return;
    		}

    		DataOutputStream clientAck;

    		try{
    			clientAck = new DataOutputStream(usernameOutSocketPair.get(sender_username).getOutputStream());
    		} catch (IOException e1) {
    			// TODO Auto-generated catch block
    			return;
    		}
    		while(true) {
    			try {
    				String recipientInfo = inFromClient.readLine();
    				int count = 0;
    				while(recipientInfo == null) {
    					recipientInfo = inFromClient.readLine();
    					count++;
    					if(count == 2) {
    						usernameOutSocketPair.get(sender_username).close();
    						usernameOutSocketPair.remove(sender_username);
    						System.out.println("removed "+sender_username);
    						return;
    					}
    				}
    				System.out.println("recipient info, receiver thread: "+recipientInfo);
    				if(recipientInfo.subSequence(0, 8).equals("RECEIVED")) {
    					inFromClient.readLine();
    					String sender = recipientInfo.substring(9);
    					Socket outsocket = usernameInSocketPair.get(sender);
    					DataOutputStream  outToClient = new DataOutputStream(outsocket.getOutputStream());
    					outToClient.writeBytes("SENT "+sender_username+"\n\n");
    				}
    				else if(recipientInfo.substring(0, 8).equals("FETCHKEY")) {
    					String receiver_username = recipientInfo.substring(9);
    					String endln = inFromClient.readLine();
    					
    					while(endln == null){
    						endln = inFromClient.readLine();
    					}
    					if(!endln.equals("")){
    						//System.out.println("1");
    						clientAck.writeBytes("Improper request\n\n");
    						continue;
    					}else{
    						//System.out.println("2");
    						clientAck.writeBytes("SENTKEY " + publicKeySocketPair.get(receiver_username) + "\n\n");
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
 

