public class Dealer extends Person {

	public Dealer(int point) {
		super(point);
		this.setName("dealer");
	}

	public Card dealCard() {
		Card hand = null;
		int x, y;
		String suit, value;

		/* get a random number from 1 to 4 */
		x = (int) (Math.random() * 3);
		/* get a random number from 1 to 13 */
		y = (int) (Math.random() * 12);

		/* get the corresponding suit and value */
		suit = CardModel.suits[x];
		value = CardModel.value[y];
		/* get the card */
		hand = new Card(suit, value);

		return hand;
	}

	/* ask card if points under 17 */
	public boolean askCard() {
		if (getPoint() < 17) {
			return true;
		}
		return false;
	}

}
