package main.ui;

import main.Main;
import main.ui.UIAction.ActionSuite;
import processing.event.MouseEvent;

import java.awt.*;
import java.util.Objects;

/**
 * Represents a root structure and states of a Button
 */
public abstract class AbstractButton implements Interactable {

  protected boolean isFocused = false;
  protected Point cornerXY;
  protected int width, height;

  protected ActionSuite actionSuite = ActionSuite.DEFAULT;

  public AbstractButton(int xLoc, int yLoc, int w, int h) {
    width = w;
    height = h;
    setCornerXY(xLoc, yLoc);
  }

  @Override
  public abstract void render(Main parentView);

  @Override
  public void setCornerXY(int newX, int newY) {
    this.cornerXY = new Point(newX, newY);
  }

  @Override
  public void addCornerXY(int addedX, int addedY) {
    this.cornerXY = new Point(cornerXY.x + addedX, cornerXY.y + addedY);
  }

  @Override
  public Point getCornerXY() {
    return cornerXY;
  }

  /**
   * Mouse detection based on RECTANGLE bounds
   */
  @Override
  public boolean isMouseOver(float mX, float mY) {
    if (mX >= cornerXY.getX() && mX <= cornerXY.getX() + width
            && mY >= cornerXY.getY() && mY <= cornerXY.getY() + height) {
      return true;
    }
    return false;
  }

  @Override
  public void removeHoveredStates() {
    setState(ButtonState.STATIC);
  }

  @Override
  public void checkState(MouseEvent event) {
    if (isFocused) {
      return;
    }

    if (!isMouseOver(event.getX(), event.getY())) {
      throw new IllegalStateException("You didn't check for this AbstractButton's state before activation!");
    }
  }

  @Override
  public void mouseHoverAction(Main controller, MouseEvent event) {
    checkState(event);
    actionSuite.actHovered(controller, event);
    setState(ButtonState.ACTIVE);
  }

  @Override
  public void mousePressedAction(Main controller, MouseEvent event) {
    Objects.requireNonNull(controller);
    Objects.requireNonNull(event);
    checkState(event);
    actionSuite.actPressed(controller, event);
    setState(ButtonState.ACTIVE);
    isFocused = true;
  }

  /**
   * By default, this will check the state, but DO NOTHING.
   * @param event
   * @param controller
   */
  @Override
  public void mouseScrollAction(MouseEvent event, Main controller) {
    Objects.requireNonNull(controller);
    Objects.requireNonNull(event);
    checkState(event);
    actionSuite.actScrolled(controller, event);
  }

  /**
   * By default, this will check the state, but DO NOTHING.
   * @param controller
   * @param event
   */
  @Override
  public void mouseDraggedAction(Main controller, MouseEvent event) {
    Objects.requireNonNull(controller);
    Objects.requireNonNull(event);
    checkState(event);
    actionSuite.actDragged(controller, event);
  }

  /**
   * By default, this will activate the released action, regardless of states.
   *
   * @param controller
   * @param event
   */
  @Override
  public void mouseReleasedAction(Main controller, MouseEvent event) {
    Objects.requireNonNull(controller);
    Objects.requireNonNull(event);
//    checkState(event);
    actionSuite.actReleased(controller, event);
    isFocused = false;
  }

  @Override
  public boolean getFocused() {
    return isFocused;
  }

  @Override
  public void bindAction(ActionSuite a) {
    this.actionSuite = a;
  }

  @Override
  public ActionSuite getActionSuite() {
    return actionSuite;
  }

  @Override
  public Dimension getDimension() {
    return new Dimension(width, height);
  }

  @Override
  public String toString() {
    return "AbstractButton [" +
            cornerXY.toString() + ", width = " +
            width + ", height = " +
            height + " ]";
  }


  public enum ButtonState {
    STATIC, ROLLOVER, ACTIVE
  }
  protected ButtonState state = ButtonState.STATIC;
  protected Boolean isToggled = false;
  public void setState(ButtonState newState) {
    state = newState;
  }

  public void setIsToggled(boolean t) {
    isToggled = t;
  }

  protected boolean debugMode = false;

  public void setDebugMode(boolean d) {
    this.debugMode = d;
  }

}

