package UI;

import main.Main;
import processing.core.PConstants;
import processing.core.PImage;

public class ImageButtonOld extends AbstractButton {
	
	private PImage icon;
	private PImage iconNorm;
	private PImage iconAlt;
	
	public ImageButtonOld(int xLoc, int yLoc, PImage image, PImage iconAlt,
												ButtonBindings binds, Main p) {
		super(xLoc, yLoc, image.width, image.height, binds, p);

		this.iconNorm = image;
		this.iconAlt = iconAlt;
		this.icon = image;
	}
	
	/**
	 * Draws the:
	 * 1. Button casing. (defined depending on constructor)
	 * 2. Button icon
	 */
	@Override
	public void draw() {
		if (icon == null) {
			throw new NullPointerException("ImageButtonOld's icon cannot be null!");
		} else {
			parent.imageMode(PConstants.CENTER);
			if (isMouseOver() && enabled) {
				parent.tint(150, 150, 150);
			}
			parent.image(icon, centerX, centerY);
			parent.noTint();
			if (!enabled) {
				drawDisabled();
			}
		}
	}
	
	private void drawDisabled() {
		parent.fill(255, 100);
		parent.noStroke();
		parent.rectMode(PConstants.CORNER);
		parent.rect(topLeftx, topLefty, width, height);
	}

	public void setIconAltVisible(boolean isAlt) {
		if (isAlt) {
			icon = iconAlt;
		} else {
			icon = iconNorm;
		}
		super.width = icon.width;
		super.height = icon.height;
		super.setX(topLeftx);
		super.setY(topLefty);
	}
	
	@Override
	public void setText(String newContents) {
		// DO NOTHING
	}
}
