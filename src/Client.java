import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	private Socket echoSocket = null;
	private PrintWriter out = null;
	private BufferedReader in = null;
	private BufferedReader stdIn = null;
	private String msg;
	private String input;

	public Client() {
		this.initClient();
		this.startGame();
	}

	public static void main(String[] args) {
		new Client();
	}

	/*
	 * Initialize variables, connect client to server
	 */
	public void initClient() {
		String serverHostname = new String("localhost");
		int serverPort = 10481;

		try {
			echoSocket = new Socket(serverHostname, serverPort);
			if (echoSocket.isConnected()) {
				System.out.println("Waiting for new game...\n");
			}
			/* print writer for client to send message to server */
			out = new PrintWriter(echoSocket.getOutputStream(), true);
			/* read message from server */
			in = new BufferedReader(new InputStreamReader(
					echoSocket.getInputStream()));
			/* read message from console */
			stdIn = new BufferedReader(new InputStreamReader(System.in));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * game communication
	 */
	public void startGame() {

		try {
			/* read messages from server */
			while ((msg = in.readLine()) != null) {
				System.out.println(msg);
				/*
				 * if message contains "enter" get input from console and send
				 * to server
				 */
				if (msg.contains("enter")) {
					input = stdIn.readLine();
					out.println(input);
				}
				/* if message contains bye, break the loop and end the game */
				if (msg.contains("bye")) {
					break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
