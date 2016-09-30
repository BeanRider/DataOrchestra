package main.ui.UIAction;

import main.Main;
import processing.event.MouseEvent;

/**
 * Represents a button event acting of the parent
 */
public class ActionSuite {

  private Action pressedAction = Action.DEFAULT;
  private Action hoveredAction = Action.DEFAULT;
  private Action scrolledAction = Action.DEFAULT;
  private Action draggedAction = Action.DEFAULT;
  private Action releasedAction = Action.DEFAULT;

  public static final ActionSuite DEFAULT = new ActionSuite();

  /**
   * Action performed on the given parent
   * @param controller
   * @param e
   */
  public void actPressed(Main controller, MouseEvent e) {
    pressedAction.act(controller, e);
  }

  /**
   * Action performed on the given parent
   * @param controller
   * @param e
   */
  public void actHovered(Main controller, MouseEvent e) {
    hoveredAction.act(controller, e);
  }

  /**
   * Action performed on the given parent
   * @param controller
   * @param e
   */
  public void actScrolled(Main controller, MouseEvent e) {
    scrolledAction.act(controller, e);
  }

  /**
   * Action performed on the given parent
   * @param controller
   * @param e
   */
  public void actDragged(Main controller, MouseEvent e) {
    draggedAction.act(controller, e);
  }

  /**
   * Action performed on the given parent
   * @param controller
   * @param e
   */
  public void actReleased(Main controller, MouseEvent e) {
    releasedAction.act(controller, e);
  }



  public void setPressedAction(Action pressed) {
    this.pressedAction = pressed;
  }

  public void setHoveredAction(Action hovered) {
    this.hoveredAction = hovered;
  }

  public void setScrolledAction(Action scrolledAction) {
    this.scrolledAction = scrolledAction;
  }

  public void setDraggedAction(Action draggedAction) {
    this.draggedAction = draggedAction;
  }

  public void setReleasedAction(Action releasedAction) {
    this.releasedAction = releasedAction;
  }

}