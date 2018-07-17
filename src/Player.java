

public class Player extends Person{

	private String id;
	public Player(String id, String name, int point) {
		super(name, point);
		this.id = id;
	}

	public String getID() {
		return id;
	}

}
