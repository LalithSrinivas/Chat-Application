import java.io.*; 
import java.net.*; 
import java.util.*;


class TCPServer {
	public static final int UNENCRYP_MODE = 1, ENCRYP_MODE = 2, ENCRYP_WITH_SIGN_MODE = 3;
	public static final String MALFORMED_USERNAME = "ERROR 100 Malformed username"; 
	public static final String USER_NOTFOUND = "ERROR 101 No user registered"; 
	public static final String UNABLE_TOSEND = "ERROR 102 Unable to send"; 
	public static final String HEADER_INCOMPLETE = "ERROR 103 Header incomplete"; 
	public static final String INCOMPATIBLE_MODE = "ERROR 104 Incompatible mode"; 
	public static final String MALFORMED_REQUEST = "ERROR 105 Malformed request"; 
	public static final int REQUESTS_LIMIT = 3;

	private static HashMap<String, Socket> usernameInSocketPair = new HashMap<String, Socket>();
	private static HashMap<String, Socket> usernameOutSocketPair = new HashMap<String, Socket>();
	private static HashMap<String, String> publicKeySocketPair = new HashMap<String, String>();
	private static int mode = UNENCRYP_MODE;


	public static void main(String argv[]) throws Exception {
		//accept requests concurrently
		ServerSocket welcomeSocket = new ServerSocket(6789);
		if(argv.length>1){//if mode is not mentioned default will be considered
			System.out.println("Invalid Arguments");
			return;
		} else if(argv.length == 1){
			mode = Integer.parseInt(argv[0]);
		}

		String modeName = "Unencrypted mode";
		
		while(mode > ENCRYP_WITH_SIGN_MODE){
			System.out.println("Invalid mode. Please enter a valid mode.");
			Scanner scanner = new Scanner(System.in);
			mode = scanner.nextInt();
			scanner.close();
		}

		if(mode == UNENCRYP_MODE)
			modeName = "Unencrypted mode";
		else if(mode == ENCRYP_MODE)
			modeName = "Encrypted without signature mode";
		else if(mode == ENCRYP_WITH_SIGN_MODE)
			modeName = "Encrypted with signature mode";

		System.out.println("Running server in " + modeName + "....");

		while(true) {
			Socket connectionSocket = welcomeSocket.accept();
			CheckForConnection connection = new CheckForConnection(connectionSocket, mode);
			Thread connectionsThread = new Thread(connection);
			connectionsThread.start();
		}
	}
	
	public static Socket getUserInSocket(String user){
		return usernameInSocketPair.get(user);
	}

	public static Socket getUserOutSocket(String user){
		return usernameOutSocketPair.get(user);
	}

	public static String getUserPublicKey(String user){
		return publicKeySocketPair.get(user);
	}

	public static void addUserInSocket(String user, Socket socket){
		usernameInSocketPair.put(user, socket);
	}

	public static boolean hasUserInSocket(String user){
		return usernameInSocketPair.containsKey(user);
	}

	public static void addUserOutSocket(String user, Socket socket){
		usernameOutSocketPair.put(user, socket);
	}

	public static boolean hasUserOutSocket(String user){
		return usernameOutSocketPair.containsKey(user);
	}

	public static void addUserPublicKey(String user, String key){
		publicKeySocketPair.put(user, key);
	}

	public static boolean hasUserPublicKey(String user){
		return publicKeySocketPair.containsKey(user);
	}	

	public static void removeUser(String user) throws IOException{
		if(usernameOutSocketPair.containsKey(user)){
			Socket removedSocket = usernameOutSocketPair.remove(user);
			removedSocket.close();
		}
		if(usernameInSocketPair.containsKey(user)){
			Socket removedSocket = usernameInSocketPair.remove(user);
			removedSocket.close();
		}
		if(publicKeySocketPair.containsKey(user)){
			String removedKey = publicKeySocketPair.remove(user);
		}
	}
	//for registering the user
	
	//this runs after creating the user correctly..
}	