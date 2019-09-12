import java.io.*; 
import java.net.*;


class TCPClient { 
    public static void main(String argv[]) throws Exception 
    { 
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
        System.out.println("Enter user name:");
        String username = inFromUser.readLine();
        while(username == null)
            username = inFromUser.readLine();
        System.out.println("Enter server ip address:");
        String ipAddr = inFromUser.readLine();
        while(ipAddr == null)
            ipAddr = inFromUser.readLine();
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
            
            outToServerSender.writeBytes("REGISTER TOSEND " + username + "\n\n");
            while(temp == null)
                temp = inFromServerSender.readLine();
            acks = temp.equals("REGISTERED TOSEND "+ username);
            if(acks)
            	System.out.println("registered");
            do{
                temp = inFromServerSender.readLine();
            }while(temp == null);
            if(!temp.equals("")) 
                acks = false;
        }while(!acks);
        
        Socket recieveMessageSocket = new Socket(ipAddr, 6789);
        DataOutputStream outToServerReceiver = new DataOutputStream(recieveMessageSocket.getOutputStream());
        BufferedReader inFromServerReceiver = new BufferedReader(new InputStreamReader(recieveMessageSocket.getInputStream()));
        do{
            outToServerReceiver.writeBytes("REGISTER TORECV " + username + "\n\n");
            String temp = inFromServerReceiver.readLine();
            while(temp == null)
                temp = inFromServerReceiver.readLine();
            ackr = temp.equals("REGISTERED TORECV " + username);
            do{
                temp = inFromServerReceiver.readLine();
            }while(temp == null);
            if(!temp.equals(""))
                ackr = false;
        }while(!ackr);
        // outToServerReceiver.flush();
        // outToServerSender.flush();
        OutputToServer outputThread = new OutputToServer(sendMessageSocket);//, outToServer, (inFromUser);
		InputFromServer inputThread = new InputFromServer(recieveMessageSocket);//, (inFromServer);
        Thread outthread = new Thread(outputThread);		
        Thread inthread = new Thread(inputThread);
        outthread.start();
        inthread.start();                   
    } 

    
} 

class InputFromServer implements Runnable { 
    Socket connectionSocket;
    BufferedReader inFromServer;
    DataOutputStream outToServer;
   
    InputFromServer (Socket connectionSocket){ //, BufferedReader inFromServer) {  }
        this.connectionSocket = connectionSocket;
        try{
            this.inFromServer = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
            this.outToServer = new DataOutputStream(this.connectionSocket.getOutputStream());
        } catch(Exception e){
            System.out.println(e+" IFS constructor");
        }
        
    } 

    public void run() {
        while(true) {
            try {
                boolean flag = true;
                int contentLength = 0;
                String senderUsername = null, senderUsernameLine = null, contentLengthLine = null;
                do{
                    senderUsernameLine = inFromServer.readLine();
                }while(senderUsernameLine == null);
                if(senderUsernameLine.substring(0, 8).equals("FORWARD "))
                    senderUsername = senderUsernameLine.substring(8);
                else
                    flag = false;

                do{
                    contentLengthLine = inFromServer.readLine();
                }while(contentLengthLine == null);

                if(contentLengthLine.substring(0, 16).equals("Content-length: "))
                    contentLength = Integer.parseInt(contentLengthLine.substring(16));
                else 
                    flag = false;
                String content = inFromServer.readLine();
				char[] msgArr = new char[contentLength];
				int yoflag  = 0;
                for(int i =0; i<contentLength && yoflag != -1; i++){
                	char[]  temparr = new char[1];
                	yoflag = inFromServer.read(temparr);
                	if(temparr[0] == 0)
                	{
                		System.out.println(i);
                		break;
                	}
                	msgArr[i] = temparr[0];
                }
                if(yoflag==-1)
                	content = "";
                else
                	content = new String(msgArr);
//				int check = inFromServer.read(msgArr, 0, contentLength);
//				if(check != -1)
//					content = new String(msgArr, 0, check);
//				else
//					content = "";
				// outToServer.flush();
                outToServer.writeBytes("RECEIVED " + senderUsername + "\n\n");
                System.out.println(senderUsername + ": " + content);
                
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
    Socket connectionSocket;
    DataOutputStream outToServer;
    BufferedReader inFromUser;
    BufferedReader inFromServer;
   
    OutputToServer (Socket connectionSocket){//, DataOutputStream outToServer, BufferedReader inFromUser) {
        this.connectionSocket = connectionSocket;
        this.inFromUser = new BufferedReader(new InputStreamReader(System.in));
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
                while(clientSentence == null)
                    clientSentence = inFromUser.readLine();
                String[] sentence_split = clientSentence.split(" ", 2);

                if(sentence_split[0].charAt(0) != '@'){
                    System.out.println("invalid message format. retype the message:");
                    continue;
                }
                String user = sentence_split[0].substring(1), msg;
                if(sentence_split.length == 1)
                    msg = "";
                else
                    msg = sentence_split[1];
                outToServer.writeBytes("SEND " + user + "\nContent-length: " + msg.length() + "\n\n"+ msg);

                String serverMsg = inFromServer.readLine();
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
