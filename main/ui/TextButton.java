package main.ui;

import main.Main;
import processing.core.PApplet;
import processing.core.PConstants;

import java.awt.*;

import static processing.core.PConstants.ROUND;

/**
 * Represents a rectangular text-based Button, with a transparent black background
 */
public class TextButton extends AbstractButton {

	private String displayedText = "";
	private boolean autoWidth = true;

	public TextButton(Point xy,
                    String contents,
                    PApplet p) {
    super(xy.x, xy.y, 0, 0);
		displayedText = contents;
		autoWidth(p);
	}

	private void drawBottomCase(Main parent) {
    parent.stroke(100, 255);
    parent.strokeCap(ROUND);
    parent.strokeJoin(ROUND);
    parent.strokeWeight(1);
		parent.fill(0, 200);
		parent.rectMode(PConstants.CORNER);
		parent.rect(cornerXY.x, cornerXY.y, width, height);
	}

	public void setSize(int newWidth, int newHeight) {
		autoWidth = false;
		width = newWidth;
		height = newHeight;
	}
	
	private void autoWidth(PApplet p) {
		width = Math.round(p.textWidth(displayedText) + 12);
		height = Math.round(p.textAscent() + p.textDescent() + 10);
	}

	public void setText(String newContents, PApplet context) {
		displayedText = newContents;
		if (autoWidth) {
			autoWidth(context);
		}
	}

	@Override
	public void render(Main parentView) {
    parentView.pushStyle();
		drawBottomCase(parentView);
		parentView.fill(255);
		parentView.noStroke();
		parentView.textFont(Main.fHel_B14, 14);
		parentView.textAlign(PConstants.CENTER, PConstants.CENTER);
		if (super.state == ButtonState.ROLLOVER) {
      parentView.fill(150);
    }
		parentView.text(displayedText, cornerXY.x + width / 2f, cornerXY.y + height / 2f);
		parentView.noTint();
    parentView.popStyle();
	}
}
