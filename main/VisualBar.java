package main;
import processing.core.PGraphics;


/**
 * VisualBar is a container for the flattened image of a data bar.
 * It contains all drawing information needed to render the flattened image
 * It contains methods to change the drawing position, and to refresh/update the flattened image, if requested.
 */
public class VisualBar {
	PGraphics bar;
	int x, y;
	float h;
	Main parent;
	int serial;
	
	public VisualBar(PGraphics bar, int x, int y, Main parent, int serial, float h) {
		this.bar = bar;
		this.x = x;
		this.y = y;
		this.h = h;
		this.parent = parent;
		this.serial = serial;
	}
	
	void setX(int newX) {
		this.x = newX;
	}
	void setY(int newY) {
		this.y = newY;
	}
	
	void display() {
		parent.image(bar, this.x, this.y);
	}
	
	int getSerial() {
		return serial;
	}
	
	void refresh() {
		switch (serial) {
		case 0:
			bar = parent.visualizeTimebar(parent.timelineInterval); // NOT USING H!!
			break;
		case 1:
			bar = parent.visualizeDailyTemp(0, h);
			break;
		case 2:
			bar = parent.visualizeSensorGraph(h);
			break;
		case 3:
			bar = parent.visualizeTwitterData(h);
			break;
		case 4:
			bar = parent.visualizeOccupancyData(h);
			break;
		case 5:
			bar = parent.visualizeAcademicCalendar(h);
			break;
		 default:
			 throw new RuntimeException("Unsupported Serial num!");
		}
	}
	
}
