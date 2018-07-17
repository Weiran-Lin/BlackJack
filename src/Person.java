import java.util.HashMap;

public class Person {

	private String name;
	private int point;
	/* <CardId, Card> */
	private HashMap<Integer, Card> cards = new HashMap<Integer, Card>();

	public Person(String name, int point) {
		this.setName(name);
		this.point = point;
	}

	public Person(int point) {
		this.point = point;
	}

	/* calculate cards point */
	public int calculate() {
		/* sum: total point count: the number of ace */
		int sum = 0, count = 0;
		Card c;
		for (int i = 0; i < cards.size(); i++) {
			c = cards.get(i);
			/* if the card is an ace counted as 1 and count+1 */
			if (c.getValue().equals("A")) {
				sum += 1;
				count++;
			}
			/* if this is a face card counted as 10 points */
			else if (c.getValue().equals("J") || c.getValue().equals("Q")
					|| c.getValue().equals("K")) {
				sum += 10;
			} 
			/* counted as the card's value */
			else {
				sum += Integer.parseInt(c.getValue());
			}

		}
		/* has one or more ace */
		while (count > 0) {
			/*
			 * if the total point not over 11 (will not bust after add 10
			 * points)
			 */
			if (sum <= 11) {
				sum += 10;
			}
			count--;
		}
		return sum;
	}

	/* remove all cards, set point = 0 */
	public void freeData() {
		cards.clear();
		point = 0;
	}

	public int getPoint() {
		return point;
	}

	/* change the value of point */
	public void setPoint(int p) {
		this.point = p;
	}
	
	public HashMap<Integer, Card> getCards() {
		return cards;
	}
	
	public void setCards(Integer i, Card c) {
		cards.put(i, c);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
