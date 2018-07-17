import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {

	private final int port = 10481;
	private BlockingQueue<Thread> playerQueue = 
			new LinkedBlockingQueue<Thread>();
	private List<PrintWriter> allOut;
	private PrintWriter gamingLog = null;
	private PrintWriter communicationLog = null;
	private Socket clientSocket = null;
	private Thread dealerThread = null;
	Dealer dealer = null;

	public Server() {
		this.initGame();
	}

	public static void main(String[] args) {
		new Server();
	}

	/* add player's print writer to one list for broadcast */
	private synchronized void addOut(PrintWriter pw) {
		allOut.add(pw);
	}

	/* remove player's print writer from the list */
	private synchronized void removeOut(PrintWriter pw) {
		allOut.remove(pw);
	}

	/* broadcast */
	private synchronized void sendMessageToAll(String msg) {
		for (PrintWriter pw : allOut) {
			pw.println(msg);
		}
	}

	/* merge the action with time and socket address */
	private synchronized String toString(String s) {
		Date date = new Date();
		s = "Time: [" + date + "], Socket Address: " + clientSocket + "\n" + s
				+ "\n";
		return s;
	}

	/* request card */
	private void dealCard(Person p, int i) {
		Card c = dealer.dealCard();
		p.setCards(i, c);
	}

	/*
	 * accept client create dealer
	 */
	private void initGame() {
		ServerSocket server = null;

		try {
			gamingLog = new PrintWriter("Gaming_Log.txt");
			communicationLog = new PrintWriter("Communication_Log.txt");

			allOut = new ArrayList<PrintWriter>();

			Turn turn = new Turn();
			server = new ServerSocket(port);

			/* every client try to connect this server will be accept */
			while ((clientSocket = server.accept()) != null) {
				System.out.println("Accept the client: " + clientSocket);
				gamingLog.println(this.toString("Accept the client."));

				/* create a thread to communicate with client */
				Thread thread = new Thread(
						new PlayerThread(clientSocket, turn));
				gamingLog.println(this.toString("Create player thread."));
				playerQueue.put(thread);
				gamingLog.println(this.toString("New thread have"
						+ " added to the player queue."));

				if (dealer == null) {
					dealer = new Dealer(0);
					gamingLog.println(this.toString("Dealer created."));
					dealerThread = new Thread(new DealerThread(turn));
					gamingLog.println(this.toString("Dealer thread created."));
					dealerThread.start();
					gamingLog.println(this.toString("Dealer thread started."));
				}
			}

		} catch (IOException | InterruptedException e) {
			/* problem with one client, do not shut down the server */
			System.err.println(e.getMessage());
		} finally {
			/* release resources */
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class PlayerThread implements Runnable {

		BufferedReader in;
		PrintWriter out;
		private Socket socket;
		Player player;
		Turn turn;

		public PlayerThread(Socket socket, Turn t) {
			this.socket = socket;
			this.turn = t;
		}

		@Override
		public void run() {
			String input;
			Boolean endGame = false;

			try {
				in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new 
						OutputStreamWriter(socket.getOutputStream())), true);
				addOut(out);
				
				gamingLog.println(Server.this.toString("Accept the client."));
				/* create player */
				out.println("Please enter your name to start game: ");
				player = new Player(String.valueOf(Thread.currentThread()
						.getId()), in.readLine(), 0);
				gamingLog.println(Server.this.toString("Player: "
						+ player.getName() + " ID: " + player.getID()
						+ " created!"));
				sendMessageToAll(player.getID() + ":" + player.getName()
						+ " join the game!");
				communicationLog.println(Server.this
						.toString("Send to all players: the player: "
								+ player.getID() + player.getName()
								+ "created!"));

				gamingLog.println(Server.this.toString("Game start."));

				/* the loop of game perform */
				while (!endGame) {

					out.println("\n\n~~~~~ Menu ~~~~~\n1.Start game\n2.Quit");
					out.println("Please enter your choice: ");
					communicationLog.println(Server.this.toString("Request to "
							+ "client: choice for start game or quit game"));
					
					input = in.readLine();
					communicationLog.println(Server.this.
							toString("Response from client: \""+ input + "\""));

					/* start game */
					if (input.equals("1")) {
						/*
						 * check the game status: "wait" means the game still
						 * waiting for new player join the game, "start" means
						 * the player can start the game, "next" means the
						 * player needs to wait for next round
						 */
						while (!endGame) {
							if (turn.getStatus().equals("wait")) {
								gamingLog.println(Server.this
										.toString("Game status: wait."));
								out.println("waiting a new round...");
								Thread.sleep(10000);
							} else if (turn.getStatus().equals("start")) {
								gamingLog.println(Server.this.toString(
										"Game status: start. Break the loop."));
								break;
							} else if (turn.getStatus().equals("next")) {
								gamingLog.println(Server.this
										.toString("Game status: next."));
								out.println("waiting a new round...");
								Thread.sleep(10000);
							}
						}

						endGame = this.startGame();
					}
					/* quit game */
					else if (input.equals("2")) {
						endGame = true;
					}
					/* wrong input */
					else {
						out.println("Invalid input");
						communicationLog.println(Server.this.toString(
								"Send to client: \"Invalid input! "
								+ "Please try again.\""));
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			} finally {
				out.println("bye");
				turn.askCardTurn(player.getClass().getName());
				turn.removePlayer();
				removeOut(out);
				sendMessageToAll(player.getID() + ":" + player.getName()
						+ " leave the game!");
				/* release resources */
				try {
					communicationLog.close();
					gamingLog.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private boolean startGame() throws IOException, InterruptedException {
			int i;
			String input;

			/* clean the data in player */
			player.freeData();
			gamingLog.println(Server.this
					.toString("Free data in player(cards and points)."));
			/* check the turn of ask card */
			turn.askCardTurn(player.getClass().getName());
			/*
			 * give first two cards to player use i as the card's key in hash
			 * map, and send the cards to client
			 */
			out.println("Your cards: ");
			for (i = 0; i < 2; i++) {
				dealCard(player, i);
				out.print(player.getCards().get(i).toString());
			}
			out.println();
			gamingLog.println(Server.this
					.toString("First two cards given to player."));
			player.setPoint(player.calculate());
			gamingLog.println(Server.this.toString("Points set to player."));
			out.println("waiting for other players...");
			/* wait all players finish ask card */
			turn.dealDone(player);
			gamingLog.println(Server.this.toString("Player finished "
					+ "waiting, access to the next step."));
			while (true) {

				out.println("1.ask one more\n2.no more cards\n3.quit");
				out.println("Please enter your choice:");
				communicationLog.println(Server.this.toString("Request to "
						+ "client: the choice of ask more card, "
						+ "wait result or quit"));
				input = in.readLine();
				communicationLog.println(Server.this
						.toString("Response from client: \"" + input + "\""));
				/* player ask one more card */
				if (input.equals("1")) {
					if (!this.askMore()) {
						/* player busted */
						return false;
					}
				}
				/*
				 * player do not want more card dealer's turn to ask cards give
				 * the result
				 */
				else if (input.equals("2")) {
					gamingLog.println(Server.this
							.toString("Player finished ask more cards."));
					/*
					 * count the number of finished asking card player if all
					 * player finished, turn the dealer ask card
					 */
					turn.askCardTurn(player.getClass().getName());
					out.println("waiting for other players finishing "
							+ "asking card...");
					turn.dealDone(player);

					/* dealer finished ask cards */
					gamingLog.println(Server.this
							.toString("Dealer finished ask cards."));
					/* send dealer cards to client */
					out.print("Dealer cards: ");
					for (i = 0; i < dealer.getCards().size(); i++) {
						out.print(dealer.getCards().get(i).toString());
					}
					out.println();
					communicationLog.println(Server.this.toString("Send to "
							+ "client: display all dealer's cards."));
					/* show result */
					this.evaluate();
					return false;
				} else if (input.equals("3")) {
					return true;
				} else {
					out.println("Invalid input!");
					communicationLog.println(Server.this
							.toString("Send to client: invalid input"));
				}

			}

		}

		private boolean askMore() {
			int i;
			/*
			 * give additional card to player use the current size of hash map
			 * as this card's key in hash map
			 */
			out.println("Your cards: ");
			dealCard(player, player.getCards().size());
			gamingLog.println(Server.this
					.toString("One more card given to player."));
			for (i = 0; i < player.getCards().size(); i++) {
				out.print(player.getCards().get(i).toString());
			}
			communicationLog.println(Server.this
					.toString("Send to client: the cards dealt for player"));
			out.println();
			player.setPoint(player.calculate());
			gamingLog.println(Server.this.toString("Player's points updated."));
			/* player busted */
			if (player.getPoint() > 21) {
				gamingLog.println(Server.this
						.toString("Player busted, dealer wins."));
				out.println("\nBusted!\nYou lose this round :(");
				communicationLog.println(Server.this
						.toString("Send to client: \"busted\""));
				/*
				 * the player finished ask card, the count of finished player
				 * plus 1
				 */
				turn.askCardTurn(player.getClass().getName());
				return false;
			}
			return true;
		}

		private void evaluate() {
			if (dealer.getPoint() > 21) {
				/* dealer busted */
				gamingLog.println(Server.this.toString("Dealer busted."));
				out.println("You win!\nDealer busted :)");
				communicationLog.println(Server.this.toString("Send to "
						+ "client: dealer busted, player wins."));
			} else if (player.getPoint() > dealer.getPoint()) {
				/* player points higher than dealer */
				gamingLog
						.println(Server.this.toString("Player wins the game."));
				out.println("You win!");
				communicationLog.println(Server.this
						.toString("Send to client: player wins."));
			} else if (player.getPoint() == dealer.getPoint()) {
				/* player draw with dealer */
				out.println("Draw with the dealer");
				communicationLog.println(Server.this
						.toString("Send to client: player draw with dealer."));
				gamingLog.println(Server.this
						.toString("Player draw with dealer."));

			} else {
				/* player lose the game */
				gamingLog
						.println(Server.this.toString("Dealer wins the game."));
				out.println("You lose! :(");
				communicationLog.println(Server.this
						.toString("Send to client: dealer wins."));
			}
		}

	}

	/*
	 * this is a thread class for dealer responsible for control game, take
	 * player from the queue, start the player thread
	 */
	private class DealerThread implements Runnable {

		private Turn turn;

		public DealerThread(Turn t) {
			this.turn = t;
		}

		@Override
		public void run() {
			int i;

			while (true) {
				dealer.freeData();
				try {
					/* wait user connect */
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				/* up to 5 player can play in one round */
				while (turn.getPlayerNum() < 5) {
					gamingLog.println(Server.this.toString("There are not "
							+ "enough players in the game."));

					/* if no player connected, break this loop */
					if (playerQueue.isEmpty()) {
						gamingLog.println(Server.this
								.toString("No player waiting in the queue."));
						break;
					}
					try {
						/* take the first player thread from player queue */
						Thread thread = playerQueue.take();
						thread.start();
						gamingLog.println(Server.this
								.toString("Dealer actives the player thread."));
						/* the count of player joined game +1 */
						turn.addPlayer();
						gamingLog.println(Server.this
								.toString("One player is added to game."));

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				/*
				 * check current player number, if no player in the game, skip
				 * to the begin of the loop
				 */
				if (turn.getPlayerNum() == 0) {
					gamingLog.println(Server.this.toString("No player "
							+ "in the game, dealer start again."));
					continue;
				}

				/* dealer sleep 10 seconds for waiting players start game */
				try {
					Thread.sleep(10000);
					turn.setStatus("start");
					gamingLog.println(Server.this.toString("Dealer change "
							+ "the game status to [start]."));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				/* dealer ask cards after all players */
				turn.askCardTurn(dealer.getClass().getName());
				gamingLog.println(Server.this
						.toString("Dealer is woke up by last player."));

				/*
				 * give first two cards to dealer using i as the card's key in
				 * hash map
				 */
				for (i = 0; i < 2; i++) {
					dealCard(dealer, i);
				}
				gamingLog.println(Server.this
						.toString("Dealer finished ask cards."));

				/* check any player staying in the game */
				if (turn.getPlayerNum() == 0) {
					turn.setStatus("wait");
					gamingLog.println(Server.this.toString("No player in the "
							+ "game, dealer start again.\nWaiting for new "
							+ "player."));
					continue;
				}
				dealer.setPoint(dealer.calculate());
				gamingLog
						.println(Server.this.toString("Points set to dealer."));

				turn.setStatus("next");
				gamingLog.println(Server.this
						.toString("Dealer change the game status to [next]"));

				/* stay for wait all players have done the deal */
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				/* awake all players to keep playing */
				turn.dealDone(dealer);
				gamingLog.println(Server.this
						.toString("Dealer awake all palyers"));

				/* ask more cards if under 17 */
				turn.askCardTurn(dealer.getClass().getName());
				gamingLog.println(Server.this
						.toString("Dealer is woke up by last player."));

				/* check any player staying in the game */
				if (turn.getPlayerNum() == 0) {
					turn.setStatus("wait");
					gamingLog.println(Server.this.toString("No player in the "
							+ "game, dealer start again.\nWaiting for "
							+ "new player."));
					continue;
				}

				/* check current dealer point */
				while (dealer.askCard()) {
					dealCard(dealer, i);
					dealer.setPoint(dealer.calculate());
				}

				/* awake all players to see the result */
				turn.dealDone(dealer);
				gamingLog.println(Server.this
						.toString("Dealer awake all palyers"));

				/* give player more time to finish this round */
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				turn.setStatus("wait");
				gamingLog.println(Server.this
						.toString("Dealer change the game status to [wait]"));
			}

		}

	}

}
