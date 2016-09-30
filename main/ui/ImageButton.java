package main.ui;

import main.Main;
import processing.core.PConstants;
import processing.core.PImage;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/**
 * A Button Rendered Using an Image
 */
public class ImageButton extends AbstractButton {

  protected PImage staticIcon = null;
  private Optional<PImage> toggledIcon = Optional.empty();
  private Optional<PImage> hoveredIcon = Optional.empty();
  private Optional<PImage> pressedIcon = Optional.empty();

  public ImageButton(PImage staticIcon, Point xy, int w, int h) {
    super(xy.x, xy.y, w, h);
    this.staticIcon = staticIcon;
  }

  public void setToggledIcon(PImage toggledIcon) {
    this.toggledIcon = Optional.of(toggledIcon);
  }

  public void setHoveredIcon(PImage hoveredIcon) {
    this.hoveredIcon = Optional.of(hoveredIcon);
  }

  public void setPressedIcon(PImage pressedIcon) {
    this.pressedIcon = Optional.of(pressedIcon);
  }

  /**
   * Toggled Icon has precedence to hover icon.
   * @param parentView
   * @param dataModel
   */
  @Override
  public void render(Main parentView) {
    Objects.requireNonNull(staticIcon);

    parentView.pushStyle();
    parentView.imageMode(PConstants.CORNER);

    PImage toRender = staticIcon;

    switch (state) {
      case STATIC:
        parentView.tint(100);
        break;
      case ROLLOVER:
        if (hoveredIcon.isPresent()) {
          toRender = hoveredIcon.get();
        }
        break;
      case ACTIVE:
        if (pressedIcon.isPresent()) {
          toRender = hoveredIcon.get();
        }
        break;
      default:
        throw new EnumConstantNotPresentException(ButtonState.class, state.name());
    }

    if (isToggled && toggledIcon.isPresent()) {
      toRender = toggledIcon.get();
    }

    parentView.image(toRender, cornerXY.x, cornerXY.y, width, height);
    parentView.popStyle();

    if (debugMode) {
      parentView.pushStyle();
      parentView.rectMode(PConstants.CORNER);
      parentView.noFill();
      parentView.strokeWeight(1f);
      parentView.stroke(255, 255, 255);
      parentView.rect(cornerXY.x, cornerXY.y, width, height);
      parentView.popStyle();
    }
  }
}
