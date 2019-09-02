import java.io.*; 
import java.net.*; 
class TCPClient { 

    public static void main(String argv[]) throws Exception 
    { 
        String sentence; 
        String modifiedSentence; 

        BufferedReader inFromUser = 
          new BufferedReader(new InputStreamReader(System.in)); 

        Socket clientSocket = new Socket("localhost", 6789); 

        DataOutputStream outToServer = 
          new DataOutputStream(clientSocket.getOutputStream()); 

        
        BufferedReader inFromServer = 
          new BufferedReader(new
          InputStreamReader(clientSocket.getInputStream())); 

		OutputToServer outputThread = new OutputToServer(clientSocket, outToServer, inFromUser);
		InputFromServer inputThread = new InputFromServer(clientSocket, inFromServer);
        Thread outthread = new Thread(outputThread);
		Thread inthread = new Thread(inputThread);
        outthread.start();
        inthread.start();
		System.out.println("Started threads");
//        clientSocket.close(); 
                   
    } 
} 

class InputFromServer implements Runnable {
     String serverSentence; 
     Socket connectionSocket;
     BufferedReader inFromServer;
   
     InputFromServer (Socket connectionSocket, BufferedReader inFromServer) {
		this.connectionSocket = connectionSocket;
        this.inFromServer = inFromServer;
		System.out.println("Constructor check ./");
     } 

     public void run() {
       while(true) { 
	   try {

	           serverSentence = inFromServer.readLine();
				if(serverSentence != null)
					System.out.println("From Server: "+serverSentence);
	   } catch(Exception e) {
		try {
			connectionSocket.close();
		} catch(Exception ee) { }
		System.out.println(e);
		break;
	   }
        } 
    }
}

class OutputToServer implements Runnable {
     String clientSentence; 
     Socket connectionSocket;
     DataOutputStream outToServer;
	 BufferedReader inFromUser;
   
     OutputToServer (Socket connectionSocket, DataOutputStream outToServer, BufferedReader inFromUser) {
		this.connectionSocket = connectionSocket;
        this.outToServer = outToServer;
		this.inFromUser = inFromUser;
     } 

     public void run() {
       while(true) {
	   try {
			   
	           clientSentence = inFromUser.readLine();
			   if(clientSentence != null)
				   outToServer.writeBytes(clientSentence + '\n');
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