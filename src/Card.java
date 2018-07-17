

public class Card {
	private String suit, value;
	
	public Card(String suit, String value) {
		this.suit = suit;
		this.value = value;
	}
	
	/* build a string with suit and value */
	public String toString() {
		return suit + "-" + value + " ";
	}
	
	public String getSuit() {
		return suit;
	}
	
	public String getValue() {
		return value;
	}
}
