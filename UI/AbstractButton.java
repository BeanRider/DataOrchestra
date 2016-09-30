package UI;

import main.Main;

public abstract class AbstractButton implements Button {
	
	public int topLeftx, topLefty;
	public float centerX, centerY;
	protected int width, height;
	protected boolean enabled = true;

	protected ButtonBindings bindings;
	protected final Main parent;
	
	public AbstractButton(int xLoc, int yLoc, int w, int h, ButtonBindings bindings, Main p) {
		width = w;
		height = h;
		// Sets topleft, center coords (based on w and h)
		setX(xLoc);
		setY(yLoc);
		this.bindings = bindings;
		
		parent = p;
	}
	
	boolean lockedIn = false;
	boolean isPressing = false;
  boolean activateNext = false;
	@Override
	public void update() {

    if (isMouseOver()) {
      if (parent.mousePressed) {
        activateNext = true;
      } else if (activateNext) { // Mouse still on the button, but it is not pressed, therefore it is released, therefore activate.
//        System.out.println("About to be activated!");
        activate();
        activateNext = false;
      } else { // Mouse still on button, no mouse is being pressed, and no previous act needed.
        activateNext = false;
      }
      parent.setLayer(Main.InterfaceLayer.UI_LAYER);
    } else {
      activateNext = false;
    }

//		if (isMouseOver()) {
//			if (parent.mousePressed) {
//				if (isPressing) {
//					// Do nothing
//				} else {
//					System.out.println("About to be activated!");
//					activate();
//					isPressing = true;
//				}
//			} else {
//				isPressing = false;
//			}
//			parent.setLayer(parent.UI_LAYER);
//		}
	}
	
	@Override
	public void activate() {
		if (enabled) {
			bindings.get(this).act();
		}
	}
	
	/**
	 * Mouse detection based on RECTANGLE bounds
	 */
	@Override
	public boolean isMouseOver() {
    return parent.mouseX >= topLeftx && parent.mouseX <= topLeftx + width
            && parent.mouseY >= topLefty && parent.mouseY <= topLefty + height;
  }
	
	@Override
	public abstract void draw();
	
	@Override
	public void toggleEnable() {
		enabled = !enabled;
	}
	
	@Override
	public int getX() {
		return topLeftx;
	}
	
	@Override
	public int getY() {
		return topLefty;
	}
	
	/**
	 * width must be defined first
	 */
	@Override
	public void setX(int newX) {
		this.topLeftx = newX;
		this.centerX = newX + width / 2;
	}
	
	/**
	 * height must be defined first
	 */
	@Override
	public void setY(int newY) {
		this.topLefty = newY;
		this.centerY = newY + height / 2;
	}
	
	@Override
	public int getWidth() {
		return width;
	}
	
	@Override
	public int getHeight() {
		return height;
	}
	
	@Override
	public abstract void setText(String newContents);
	
}
