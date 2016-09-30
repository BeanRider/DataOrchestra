package main;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import UI.Button;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PShape;
import processing.core.PVector;

public class CalendarPopup {

	PShape sign;
	PShape calendarShape;
	
	int calendarColumnWidth = 24;
	int calendarRowHeight = 22;
//	int calendarArrowWidth = 23;
	int pointHeight = 10;
	float topLeftX;
	float topLeftY;
	
	Main parent;
	Button origin;
	
    private final PFont fHel_B12;
    private final PFont fHel_L11;	
    
	public CalendarPopup(Main p, Button o) {
		parent = p;
		origin = o;
		
		fHel_B12 = Main.fHel_B12;
		fHel_L11 = Main.fHel_L11;
		
		createShapes();
		updatePosition();
	}
	
	public int getCalendarColWidth() {
		return calendarColumnWidth;
	}
	
	public void createShapes() {
		calendarShape = parent.createShape();
		calendarShape.beginShape();
		calendarShape.noStroke();
		calendarShape.vertex(0, 0);
		calendarShape.vertex(calendarColumnWidth * 7, 0);
		calendarShape.vertex(calendarColumnWidth * 7, calendarRowHeight * 7);
//		calendarShape.vertex((calendarColumnWidth * 7 - calendarArrowWidth) / 2 + calendarArrowWidth, calendarRowHeight * 7);
//		calendarShape.vertex(calendarColumnWidth * 7 / 2, calendarRowHeight * 7 + pointHeight); // Point
//		calendarShape.vertex((calendarColumnWidth * 7 - calendarArrowWidth) / 2, calendarRowHeight * 7);
    	calendarShape.vertex(0, calendarRowHeight * 7);
    	calendarShape.vertex(0, 0);
    	calendarShape.endShape();
    	calendarShape.setFill(parent.color(0, 160));
	}
	
	void updatePosition() {
		topLeftX = parent.width - calendarColumnWidth * 7 - parent.xMargin;
    	topLeftY = origin.getY() - origin.getHeight() - (calendarRowHeight * 7);
	}
 	
	public PVector onWhichCellCalendar() {
		if (parent.mouseX > topLeftX
				&& parent.mouseX < topLeftX + calendarColumnWidth * 7
				&& parent.mouseY > topLeftY
				&& parent.mouseY < topLeftY + calendarRowHeight * 7) {
			
			int row = Math.floorDiv((int) (parent.mouseY - topLeftY), calendarRowHeight);
			int col = Math.floorDiv((int) (parent.mouseX - topLeftX), calendarColumnWidth);
			if (row == 0 || row > 6) {
				return null;
			} else {
				return new PVector(row, col);
			}
		} else {
			return null;
		}
	}
	
	// Click on day button to activate, click anywhere other than calendar will turn it off
	void display() {
		updatePosition();
		parent.shapeMode(PConstants.CORNER);
		parent.shape(calendarShape, topLeftX, topLeftY);
		
		parent.textAlign(PConstants.CENTER, PConstants.CENTER);
		parent.textFont(fHel_B12, 10);
		long beginUnixMonth = TimeRangeUtil.floorMonth(parent.timeRange.getCurUnix());
		DateTime currentMo = new DateTime(beginUnixMonth * 1000L, DateTimeZone.forID("America/New_York"));
		int daysOffset = currentMo.getDayOfWeek();
		if (daysOffset == 7) {
			daysOffset = 0;
		}
		PVector cell = onWhichCellCalendar();
		if (cell != null) {
			parent.fill(40, 100);
			parent.ellipseMode(PConstants.CENTER);
			parent.noStroke();
			parent.ellipse(Math.round(topLeftX + cell.y * calendarColumnWidth + calendarColumnWidth / 2),
					Math.round(topLeftY + cell.x * calendarRowHeight + calendarRowHeight / 2 + 1),
					20, 20);
		}
		for (int row = 0; row < 7; ++row) {
			for (int col = 0; col < 7; ++col) {
				if (row == 0) {
					parent.fill(255);
					if (col == 0) {
						parent.text(TimeRangeUtil.getDayAsOneLetterStringFromNum(7),
								topLeftX + col * calendarColumnWidth + calendarColumnWidth / 2,
								topLeftY + row * calendarRowHeight + calendarRowHeight / 2);
					} else {
						parent.text(TimeRangeUtil.getDayAsOneLetterStringFromNum(col),
								topLeftX + col * calendarColumnWidth + calendarColumnWidth / 2,
								topLeftY + row * calendarRowHeight + calendarRowHeight / 2);
					}
				} else {
					DateTime currentDay = new DateTime(beginUnixMonth * 1000L, DateTimeZone.forID("America/New_York"));
					currentDay = currentDay.plusDays((row - 1) * 7 + col);
					// As long as the currentDay is still within the month
					if (currentDay.getMonthOfYear() == currentMo.getMonthOfYear()) {
						
						// Offset the days
						int offsetCol = (col + daysOffset) % 7;
						int offsetRow = row + (col + daysOffset) / 7;
						
						int curDayOfMonth = new DateTime(parent.timeRange.getCurUnix() * 1000L, DateTimeZone.forID("America/New_York")).getDayOfMonth();
						if (curDayOfMonth == currentDay.getDayOfMonth()) {
							parent.fill(250);
							parent.ellipseMode(PConstants.CENTER);
							parent.noStroke();
							parent.ellipse(Math.round(topLeftX + offsetCol * calendarColumnWidth + calendarColumnWidth / 2),
									Math.round(topLeftY + offsetRow * calendarRowHeight + calendarRowHeight / 2 + 1),
									22, 22);
							parent.fill(0);
							parent.textFont(fHel_L11, 11);
							parent.text(currentDay.getDayOfMonth(),
									topLeftX + offsetCol * calendarColumnWidth + calendarColumnWidth / 2,
									topLeftY + offsetRow * calendarRowHeight + calendarRowHeight / 2);
						} else {
							parent.fill(255);
							parent.textFont(fHel_L11, 11);
							parent.text(currentDay.getDayOfMonth(),
									topLeftX + offsetCol * calendarColumnWidth + calendarColumnWidth / 2,
									topLeftY + offsetRow * calendarRowHeight + calendarRowHeight / 2);
						}
					}
				}
			}
		}
		parent.shapeMode(PConstants.CORNER);
	}
}