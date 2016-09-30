package UI;

import main.Main;
import processing.core.PImage;

public class RoundImageButtonOld extends ImageButtonOld {
	
	public RoundImageButtonOld(int xLoc, int yLoc, PImage image, PImage alt,
														 ButtonBindings binds, Main p) {
		super(xLoc, yLoc, image, alt, binds, p);
	}

	@Override
	public boolean isMouseOver() {
		float xDistance = Math.abs(parent.mouseX - centerX);
		float yDistance = Math.abs(parent.mouseY - centerY);
		float distance = (float) Math.sqrt(xDistance * xDistance + yDistance * yDistance);
		return distance < getWidth() / 2f;
	}
}
