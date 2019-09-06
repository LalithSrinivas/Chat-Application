import java.io.*; 
import java.net.*;
import javafx.util.Pair;

class TCPClient { 
    public static void main(String argv[]) throws Exception 
    { 
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
        System.out.println("Enter user name:\n");
        String username = inFromUser.readLine();
        while(username == null)
            username = inFromUser.readLine();
        System.out.println("Enter server ip address:\n");
        String ipAddr = inFromUser.readLine();
        while(ipAddr == null)
            ipAddr = inFromUser.readLine();

        Socket sendMessageSocket = new Socket(ipAddr, 6789);
        DataOutputStream outToServerSender = new DataOutputStream(sendMessageSocket.getOutputStream());         
        BufferedReader inFromServerSender = new BufferedReader(new InputStreamReader(sendMessageSocket.getInputStream())); 
        
        Socket recieveMessageSocket = new Socket(ipAddr, 6789);
        DataOutputStream outToServerReceiver = new DataOutputStream(recieveMessageSocket.getOutputStream());
        BufferedReader inFromServerReceiver = new BufferedReader(new InputStreamReader(recieveMessageSocket.getInputStream()));
        
        boolean flag = false, acks = false, ackr = false; 
        
        do{
            if(flag)
                System.out.println("Invalid user name. Enter again:\n");
            if(!flag) flag = true;

            username = inFromUser.readLine();//check if username can be null
            
            String temp = null;
            outToServerSender.writeBytes("REGISTER TOSEND [" + username + "]\n\n");
            while(temp == null)
                temp = inFromServerSender.readLine();
            acks = temp.equals("REGISTERED TOSEND "+ username);
            //if(!acks) continue;

            do{
                temp = inFromServerSender.readLine();
            }while(temp == null);
            if(temp != "") 
                acks = false;   
        }while(!acks);
        
        do{
            outToServerReceiver.writeBytes("REGISTER TORECV [" + username + "]\n\n");
            String temp = inFromServerReceiver.readLine();
            while(temp == null)
                temp = inFromServerReceiver.readLine();
            ackr = temp.equals("REGISTERED TORECV " + username);
            do{
                temp = inFromServerReceiver.readLine();
            }while(temp == null);
            if(temp != "")
                ackr = false;
        }while(!ackr);

        OutputToServer outputThread = new OutputToServer(sendMessageSocket);//, outToServer, inFromUser);
		InputFromServer inputThread = new InputFromServer(recieveMessageSocket);//, inFromServer);
        Thread outthread = new Thread(outputThread);		
        Thread inthread = new Thread(inputThread);
        outthread.start();
        inthread.start();
		//System.out.println("Started threads");
//        clientSocket.close();                    
    } 

    
} 

class InputFromServer implements Runnable { 
    Socket connectionSocket;
    BufferedReader inFromServer;
    DataOutputStream outToServer;
   
    InputFromServer (Socket connectionSocket){//, BufferedReader inFromServer) {
        this.connectionSocket = connectionSocket;
        try{
            this.inFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            this.outToServer = new DataOutputStream(connectionSocket.getOutputStream());
        } catch(Exception e){
            System.out.println(e);
        }
        
    } 

    public void run() {
        while(true) { 
            try {
                boolean flag = true;
                int contentLength = 0;
                String senderUsername = null, senderUsernameLine = null, contentLengthLine = null;
                //String senderUsernameLine = inFromServer.readLine();
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

                String endl = inFromServer.readLine();
                while(endl == null)
                    endl = inFromServer.readLine();
                
                if(endl != "")
                    flag = false;
                //Char[] content = new Char[]
                String content;
                content = inFromServer.readLine();
                while(content == null)
                    content = inFromServer.readLine();


                // if(!Parser.isHeaderComplete(serverSentence)){
                //     outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
                //     continue;
                // }

                // Pair server_msg = Parser.getServerMsg(serverSentence);
                // String sender = server_msg.getKey().toString();
                // String msg = server_msg.getValue().toString();
                outToServer.writeBytes("RECEIVED " + senderUsername + "\n\n");
                System.out.println(senderUsername + ": " + content);
            } 
            catch(Exception e) {
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
            System.out.println(e);
        } 
    }

    public void run() {
        while(true) {
            try { 
                String clientSentence = inFromUser.readLine();
                while(clientSentence == null)
                    clientSentence = inFromUser.readLine();

                Pair user_msg = getUserMsg(clientSentence);
                if(user_msg == null){
                    System.out.println("invalid message format. retype the message:");
                    continue;
                }
                String user = user_msg.getKey().toString();
                String msg = user_msg.getValue().toString();
                outToServer.writeBytes("SEND " + user + "\nContent-length: " + msg.length() + "\n\n"+ msg);

                String serverMsg = inFromServer.readLine();
                while(serverMsg == null)
                    serverMsg = inFromServer.readLine();
                boolean ack = serverMsg.equals("SEND " + user);
                do{
                    serverMsg = inFromServer.readLine();
                }while(serverMsg == null);

                if(serverMsg != null)
                    ack = false;

                if(ack)
                    System.out.println("message sent sucessfully to " + user);
                else
                    System.out.println("message sending to " + user + " failed");
            } catch(Exception e) {
                try {
                    connectionSocket.close();
                } catch(Exception ee) { }
                System.out.println("outputToServer "+e);
                break;
            }
        } 
    }

    public static Pair<String, String> getUserMsg(String s){
        int count = 0;
        while(s.charAt(count) != ' ' && count < s.length())
            count ++;
        if(count == s.length() || s.charAt(0) != '@') return null;
        String username = s.substring(1, count-1);
        String msg = s.substring(count+1);
        return new Pair(username, msg);
    }
}
