import java.io.*; 
import java.net.*;
import java.util.Base64;

import java.security.KeyPair;
import java.security.UnrecoverableEntryException;



//import com.sun.org.apache.bcel.internal.generic.NEW;


class TCPClient { 
	public static final int UNENCRYP_MODE = 1, ENCRYP_MODE = 2, ENCRYP_WITH_SIGN_MODE = 3;
	private static final String MALFORMED_USERNAME = "ERROR 100 Malformed username"; 
	private static final String USER_NOTFOUND = "ERROR 101 No user registered"; 
	private static final String UNABLE_TOSEND = "ERROR 102 Unable to send"; 
	private static final String HEADER_INCOMPLETE = "ERROR 103 Header incomplete"; 
	private static final String INCOMPATIBLE_MODE = "ERROR 104 Incompatible mode"; 
	private static final String MALFORMED_REQUEST = "ERROR 105 Malformed request"; 

	
	private static BufferedReader inFromUser, inFromServerSender, inFromServerReceiver;
	private static DataOutputStream outToServerReceiver, outToServerSender;
	private static String username, ipAddr;
	private static int mode = UNENCRYP_MODE;

	public static void main(String argv[]) throws Exception 
	{ 
		String serverMsg, endln;

		KeyPair keyPair = Cryptography.generateKeyPair();
		byte[] publicKey = keyPair.getPublic().getEncoded();
		byte[] privateKey = keyPair.getPrivate().getEncoded();
		String publicKeyString  = java.util.Base64.getEncoder().encodeToString(publicKey);

		inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
		if(argv.length!=3 && argv.length != 2){
			System.out.println("Invalid arguments. Please enter arguments in the order of Username, IP Address and mode"); 
			return;    
		}
		username = argv[0];
		ipAddr = argv[1];
		if(argv.length == 2)
			mode = Integer.parseInt(argv[2]); 
		while(mode!=UNENCRYP_MODE && mode!=ENCRYP_MODE && mode!=ENCRYP_WITH_SIGN_MODE){
			System.out.printf("Invalid arguments. Please enter valid mode: "); 
			Scanner scn = new Scanner(System.in);
			try{
				mode = scn.nextInt();
			}catch(Exception e){
				System.out.println("\nreturnning...");
				return;
			}
		}

		Socket sendMessageSocket = new Socket(ipAddr, 6789);
		Socket recieveMessageSocket = new Socket(ipAddr, 6789);
		boolean statusRec, statusSend;
		try{
			outToServerSender = new DataOutputStream(sendMessageSocket.getOutputStream());         
			outToServerReceiver = new DataOutputStream(recieveMessageSocket.getOutputStream());

			inFromServerSender = new BufferedReader(new InputStreamReader(sendMessageSocket.getInputStream())); 
			inFromServerReceiver = new BufferedReader(new InputStreamReader(recieveMessageSocket.getInputStream()));

			statusSend = registerToSend();
			if(statusSend)
				registerToRecv();
			verifyMode();
			if(mode!=UNENCRYP_MODE){			
				sendPublickey(publicKeyString);
			}
			System.out.println("Client successfully registered");			
		}
		catch(Exception e){
			System.out.println(e);
			return;
		}
		
		outToServerReceiver.flush();
		outToServerSender.flush();
		if(statusSend) {
			OutputToServer outputThread = new OutputToServer(sendMessageSocket, privateKey, mode);//, outToServer, (inFromUser);
			InputFromServer inputThread = new InputFromServer(recieveMessageSocket, privateKey, mode);//, (inFromServer);
			Thread outthread = new Thread(outputThread);
			Thread inthread = new Thread(inputThread);
			outthread.start();
			inthread.start();
		}
	}

	private static boolean registerToSend() throws IOException{
		boolean acks = false;
		int count=0;
		while(!acks){	
			outToServerSender.writeBytes("REGISTER TOSEND " + username + "\n\n");
			String serverMsg = inFromServerSender.readLine();
			acks = serverMsg.equals("REGISTERED TOSEND "+ username);
			String endln = inFromServerSender.readLine();
			
			if(acks && endln.equals(""))
				break;
			acks = false;
			if(count > REQUESTS_LIMIT) {
				System.out.println("failed more than two times, returning..")
				return false;
			}
			System.out.println("Invalid username format. Please enter again");
			username = inFromUser.readLine();
		}
		return true;
		//System.out.println("Registration to send successful");
	}

	private static boolean registerToRecv() throws IOException{
		outToServerReceiver.writeBytes("REGISTER TORECV " + username + "\n\n");
		String serverMsg = inFromServerReceiver.readLine();
		boolean ackr = serverMsg.equals("REGISTERED TORECV " + username);
		String endln = inFromServerReceiver.readLine();
		if(!ackr) {
			System.out.println("returning..");
			return false;
		}
		return true;
		//if(ackr)
			//System.out.println("Registration to receive successful");
	}

	private static void verifyMode() throws IOException{	
		
		do{
			outToServerSender.writeBytes("MODE " + mode + "\n\n");
			String serverMsg = inFromServerSender.readLine();System.out.println(serverMsg);
			String endln = inFromServerSender.readLine();
			if(serverMsg.equals(INCOMPATIBLE_MODE)){
				System.out.println("Mode is incompatible with that of server");
			//	return;
				System.out.println("Retype mode:");
				mode = Integer.parseInt(inFromUser.readLine());
				//inFromServerSender.reset();
				//continue;
			}
			else 
				return;           
		}while(true);
	}

	private static void sendPublickey(String publicKeyString) throws IOException{
		String serverMsg, endln;

//		do{
				outToServerSender.writeBytes("PublicKey " + publicKeyString + "\n\n");
				serverMsg = inFromServerSender.readLine();
				// if(!serverMsg.equals("RECEIVED PUBLICKEY")){
				// 	inFromServerSender.reset();
				// 	continue;
				// }

				endln = inFromServerSender.readLine();
			// 	if(!endln.equals("")){
			// 		inFromServerSender.reset();
			// 		continue;
			// 	}       
			// 	break;           
			// }while(true);
	}
} 