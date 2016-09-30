package UI;

public interface Button {
	
	void update();
	
	void activate();
	
	boolean isMouseOver();
	
	void draw();
	
	void toggleEnable();
	
	int getX();
	
	int getY();
	
	void setX(int newX);
	
	void setY(int newY);
	
	int getWidth();
	
	int getHeight();
	
	void setText(String newContents);
	
}