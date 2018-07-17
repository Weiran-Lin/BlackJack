/*
 * This class is responsible for control the game performing turns
 * in the ask cards step, dealer asks cards until all players had a turn
 * after dealer finished this step, players go to the next step
 */
public class Turn {

	/* the total number of players */
	private int playerNum;
	/* the total number of players who have done the action */
	private int currPlayer;
	/*
	 * The game status: "wait" means the game still waiting for new player join
	 * the game, "start" means the player can start the game, "next" means the
	 * player needs to wait for next round
	 */
	private String status;

	public Turn() {
		status = "wait";
		playerNum = 0;
		currPlayer = 0;
	}

	public void setStatus(String s) {
		status = s;
	}

	public synchronized String getStatus() {
		return status;
	}

	/* manage the turns between dealer and players when ask card */
	public synchronized void askCardTurn(String s) {
		if (!s.equals("Player")) {
			try {
				/* dealer waits for all player finished ask card */
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			/*
			 * every one player finished ask card, increase the number of
			 * current player who have done asking card
			 */
			currPlayer++;
		}
		if (currPlayer == playerNum) {
			/* all player finished, wake dealer up */
			notify();
			currPlayer = 0;
		}
	}

	/*
	 * when players have done the card, they stay here waiting for dealer finish
	 * asking card
	 */
	public synchronized void dealDone(Person p) {
		if (p.getClass().getName().equals("Player")) {
			/* players wait to be notified */
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			/* dealer wakes up all players */
			notifyAll();
		}
	}

	/* increase the number of player when player join the game */
	public synchronized void addPlayer() {
		this.playerNum++;
	}

	public synchronized int getPlayerNum() {
		return playerNum;
	}

	public synchronized int getCurrPlayer() {
		return currPlayer;
	}

	public synchronized void setCurrPlayer(int i) {
		currPlayer = i;
	}

	/* decrease the number of player when player quit the game */
	public synchronized void removePlayer() {
		this.playerNum--;
		if (this.currPlayer != 0) {
			this.currPlayer--;
		}
	}

}
