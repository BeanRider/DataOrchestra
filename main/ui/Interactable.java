package main.ui;

import main.Main;
import main.ui.UIAction.ActionSuite;
import processing.event.MouseEvent;

import java.awt.*;

/**
 * For all selectable UI components:
 * buttons, scroll-panels, draggers ... so on
 */
public interface Interactable {

  /**
   * @return the corner xy position of this component
   */
  Point getCornerXY();

  /**
   * EFFECT: Sets the corner xy position of this component
   * @param newX new corner x
   * @param newY new corner y
   */
  void setCornerXY(int newX, int newY);

  /**
   * EFFECT: Adds to this component's corner xy
   * @param addedX to x component
   * @param addedY to y component
   */
  void addCornerXY(int addedX, int addedY);

  /**
   * Renders this component to the parentView
   * @param parentView view to to render this on
   * @param dataModel origin of data to interpret into graphics form (influences graphs, button icons, positioning...)
   */
  void render(Main parentView);

  /**
   * Determines whether the given mouse positions are on top of this component
   * @param mX current x location of cursor
   * @param mY current y location of cursor
   * @return true if mouse if on "this"
   */
  boolean isMouseOver(float mX, float mY);

  /**
   * The logic to perform when the mouse is confirmed to be hovered on "this"
   * @param controller
   * @param event
   */
  void mouseHoverAction(Main controller, MouseEvent event);

  /**
   * Called when the parent's mouse is scrolling; does not check states before performing action.
   * Please check this states, as well as other Interactables' states before calling mouseScrollAction.
   * @param event mouse event
   * @param controller
   * @throws IllegalStateException if the state is not accepted when this is called
   */
  void mouseScrollAction(MouseEvent event, Main controller);

  /**
   * Called when the parent's mouse is clicked (can fail to activate).
   * @param controller
   * @param event
   */
  void mousePressedAction(Main controller, MouseEvent event);

  /**
   * Called when the parent's mouse is dragged
   * @param event
   * @throws IllegalStateException if the state is not accepted when this is called
   */
  void mouseDraggedAction(Main controller, MouseEvent event);

  /**
   * Called when the parent's mouse is dragged
   * @param event
   * @throws IllegalStateException if the state is not accepted when this is called
   */
  void mouseReleasedAction(Main controller, MouseEvent event);

  boolean getFocused();

  /**
   * EFFECT: Restores all hovered states to normal (un-hovered)
   */
  void removeHoveredStates();

  /**
   * @return the dimensions of this component
   */
  Dimension getDimension();

  /**
   * Checks the interactable's state before performing an event
   * @throws IllegalStateException if the state is invalid
   */
  void checkState(MouseEvent e);

  void bindAction(ActionSuite a);

  ActionSuite getActionSuite();

}
