package UI;

import processing.core.PConstants;
import processing.core.PFont;
import main.Main;

import static processing.core.PConstants.ROUND;

/**
 * Represents a rectangular text-based Button, with a transparent black background
 */
public class TextButtonOld extends AbstractButton {

	private String displayedText = "";
	private PFont font;
	private boolean autoWidth = true;
	
	/**
	 * @param xLoc
	 * @param yLoc
	 * @param contents
	 * @param contentFont
	 * @param p
	 */
	public TextButtonOld(int xLoc, int yLoc, String contents, PFont contentFont,
											 ButtonBindings binds, Main p) {
		super(xLoc, yLoc, 0, 0, binds, p);

		displayedText = contents;
		font = contentFont;
		autoWidth();
	}
	
	/**
	 * @param xLoc
	 * @param yLoc
	 * @param contents
	 * @param p
	 */
	public TextButtonOld(int xLoc, int yLoc, String contents,
											 ButtonBindings binds, Main p) {
		super(xLoc, yLoc, 0, 0, binds, p);
		
		displayedText = contents;
		font = Main.fHel_B14;
		
		autoWidth();
	}

	@Override
	public void draw() {
		drawBottomCase();
		parent.fill(255);
		parent.noStroke();
		parent.textFont(font, 14);
		parent.textAlign(PConstants.CENTER, PConstants.CENTER);
		if (isMouseOver() && enabled)
			parent.fill(150);
		parent.text(displayedText, topLeftx + width / 2f, topLefty + height / 2f);
		parent.noTint();
	}

	private void drawBottomCase() {
		parent.stroke(100, 255);
		parent.strokeCap(ROUND);
		parent.strokeJoin(ROUND);
		parent.strokeWeight(1);
		parent.fill(0, 200);
		parent.rectMode(PConstants.CORNER);
		parent.rect(topLeftx, topLefty, width, height);
	}

	public void setSize(int newWidth, int newHeight) {
		autoWidth = false;
		width = newWidth;
		height = newHeight;
	}
	
	private void autoWidth() {
		parent.textFont(font);
		width = Math.round(parent.textWidth(displayedText) + 12);
		height = Math.round(parent.textAscent() + parent.textDescent() + 10);
	}

	@Override
	public void setText(String newContents) {
		displayedText = newContents;
		if (autoWidth) {
			autoWidth();
		}
	}
}
