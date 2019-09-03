import java.io.*; 
import java.net.*; 

class TCPServer { 
    public static void main(String argv[]) throws Exception { 
        String clientSentence; 
        String capitalizedSentence; 
        System.out.println("6789");
        ServerSocket welcomeSocket = new ServerSocket(6789);
        // ServerSocket welcomeSocket2 = new ServerSocket(6789);
        Socket connectionSocket1 = welcomeSocket.accept();

        BufferedReader inFromClient1 = new BufferedReader(new InputStreamReader(connectionSocket1.getInputStream()));
        System.out.println("connection 1 formed");
        // BufferedReader inFromClient2 = 
        // new BufferedReader(new
        // InputStreamReader(connectionSocket2.getInputStream()));


        // DataOutputStream  outToClient1 = 
        // new DataOutputStream(connectionSocket1.getOutputStream());
        Socket connectionSocket2 = welcomeSocket.accept();
        DataOutputStream  outToClient2 = new DataOutputStream(connectionSocket2.getOutputStream());
        System.out.println("connection 2 formed");
        while(true) { 
            clientSentence = inFromClient1.readLine(); 

            if(clientSentence != null){
			   System.out.println(clientSentence);
			   capitalizedSentence = clientSentence.toUpperCase() + '\n'; 

			   outToClient2.writeBytes(capitalizedSentence); 
			}
        } 
    } 
} 
 

