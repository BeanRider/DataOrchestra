package main.ui;

import main.Main;
import main.TimeRangeUtil;
import main.ui.UIAction.Action;
import main.ui.UIAction.ActionSuite;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.MouseEvent;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

import static processing.core.PConstants.*;

// Click on day button to activate,
// click anywhere other than calendar will turn it off
public class CalendarModule implements Interactable {

  private final int colWidth = 30;
  private final int rowHeight = 30;

  private Point cornerXY;
  int width = colWidth * 7;
  int height = rowHeight * 7;


  private Optional<PVector> selectedCell = Optional.empty(); // x = row; y = col
  private AbstractButton leftButton;
  private AbstractButton rightButton;

  /**
   * The calendar page to render
   */
  private DateTime firstDayOfDisplayedMonth;

  private DateTime firstDayInGrid;

  /**
   * The date limits that the calendar can display
   */
  private Optional<DateTime> firstDayLimit = Optional.empty();

  private Optional<DateTime> lastDayLimit = Optional.empty();

  public CalendarModule(int xLoc, int yLoc, PImage leftArrow, PImage rightArrow, DateTime currentTime) {
    cornerXY = new Point(xLoc, yLoc);

    leftButton = new ImageButton(
            leftArrow,
            new Point(xLoc, yLoc),
            leftArrow.width,
            leftArrow.height);
    ActionSuite leftButtonSuit = new ActionSuite();
    leftButton.bindAction(leftButtonSuit);
    leftButtonSuit.setPressedAction(new Action(){
      @Override
      public void act(Main controller, MouseEvent e) {
        if (firstDayLimit.isPresent()) {
          if (firstDayLimit.get().compareTo(firstDayOfDisplayedMonth) < 0) {
            // firstDayLimit ... this month's first day, then proceed.
            setDisplayedMonth(firstDayOfDisplayedMonth.minusMonths(1));
          }
        } else {
          setDisplayedMonth(firstDayOfDisplayedMonth.minusMonths(1));
        }
      }
    });

    rightButton = new ImageButton(
            rightArrow,
            new Point(xLoc + width - colWidth, yLoc),
            rightArrow.width,
            rightArrow.height);
    ActionSuite rightButtonSuit = new ActionSuite();
    rightButton.bindAction(rightButtonSuit);
    rightButtonSuit.setPressedAction(new Action() {
      @Override
      public void act(Main controller, MouseEvent e) {
        if (lastDayLimit.isPresent()) {
          if (lastDayLimit.get().compareTo(firstDayOfDisplayedMonth.plusMonths(1)) > 0) {
            // next month's first day ... lastDayLimit, then proceed.
            setDisplayedMonth(firstDayOfDisplayedMonth.plusMonths(1));
          }
        } else {
          setDisplayedMonth(firstDayOfDisplayedMonth.plusMonths(1));
        }
      }
    });

    firstDayOfDisplayedMonth = new DateTime(
            currentTime.getYear(),
            currentTime.getMonthOfYear(),
            1, 0, 0, 0, 0, DateTimeZone.forID("America/New_York"));;
  }

  public void setFirstDayLimit(DateTime firstDay) {
    firstDayLimit = Optional.of(new DateTime(firstDay));
  }

  public void setLastDayLimit(DateTime lastDay) {
    lastDayLimit = Optional.of(new DateTime(lastDay));
  }

  public Optional<DateTime> getHoveredDate() {
    if (selectedCell.isPresent()) {
      PVector cell = selectedCell.get();
      // cell.x - 2 because there are two rows of non-selectable dates
      if (cell.x > 1) {
        DateTime selected = firstDayInGrid.plusDays((int) cell.y + ((int) cell.x - 2) * 7);
        // if the left bound exists, && selected is left of the left bound
        if (firstDayLimit.isPresent() && selected.compareTo(firstDayLimit.get()) < 0) {
          return Optional.empty();
        }

        // if the right bound exists, && selected is right of the right bound
        if (lastDayLimit.isPresent() && selected.compareTo(lastDayLimit.get()) > 0) {
          return Optional.empty();
        }

        return Optional.of(selected);
      }
    }
    return Optional.empty();
  }

  @Override
  public Point getCornerXY() {
    return cornerXY;
  }

  @Override
  public void setCornerXY(int newX, int newY) {
    cornerXY = new Point(newX, newY);
    leftButton.setCornerXY(newX, newY);
    rightButton.setCornerXY(newX + width - colWidth, newY);
  }

  @Override
  public void addCornerXY(int addedX, int addedY) {
    cornerXY = new Point(
            cornerXY.x + addedX,
            cornerXY.y + addedY);
    leftButton.addCornerXY(addedX, addedY);
    rightButton.addCornerXY(addedX, addedY);
  }

  @Override
  public void render(Main parent) {

    if (isVisible()) {
      parent.pushStyle();

      // Panel
      parent.fill(0, 160);
      parent.stroke(100, 255);
      parent.strokeCap(ROUND);
      parent.strokeJoin(ROUND);
      parent.strokeWeight(1);

      // Outside
      parent.rectMode(CORNER);
      parent.rect(cornerXY.x, cornerXY.y, width, height);

      // Top row fill
      parent.pushStyle();
      parent.fill(30);
      parent.noStroke();
      parent.rect(
              cornerXY.x + 1, cornerXY.y + 1,
              width - 1, rowHeight - 1);
      parent.popStyle();

      // Current time
      DateTime currentTime = new DateTime(
              parent.timeRange.getCurUnix() * 1000L,
              DateTimeZone.forID("America/New_York"));

      // First Row: Label "Month Year" with left and right controls:
      parent.fill(255);
      parent.textAlign(CENTER, CENTER);
      parent.textFont(Main.fHel_B12, 11);

      String monthYearLabel = firstDayOfDisplayedMonth.monthOfYear().getAsText().toUpperCase() + " "
              + firstDayOfDisplayedMonth.year().getAsText();
      parent.text(monthYearLabel,
              cornerXY.x + width / 2,
              cornerXY.y + rowHeight / 2);
      leftButton.render(parent);
      rightButton.render(parent);
      parent.strokeCap(SQUARE);
      parent.stroke(200);
      parent.strokeWeight(0.25f);
      parent.line(
              cornerXY.x, cornerXY.y + rowHeight,
              cornerXY.x + width, cornerXY.y + rowHeight);

      // Second Row: Letter representing days of Week
      int day = 0;
      while (day < 7) {
        int dayRep = day;
        if (day == 0) {
          dayRep = 7;
        }
        parent.text(TimeRangeUtil.getDayAsOneLetterStringFromNum(dayRep),
                cornerXY.x + day * colWidth + colWidth / 2,
                cornerXY.y + 1 * rowHeight + rowHeight / 2);
        ++day;
      }

      parent.stroke(200);
      parent.line(
              cornerXY.x, cornerXY.y + 2 * rowHeight,
              cornerXY.x + width, cornerXY.y + 2 * rowHeight);

      // Third row +: Grid of selectable dates of the displayed month page.

      // First day in the rendered grid
      firstDayInGrid = firstDayOfDisplayedMonth.withDayOfWeek(calendarStartDay);
      // Case if first day of grid is NOT sunday, then it should be the sunday of the week before, and not this week's Sunday
      if (firstDayInGrid.isAfter(firstDayOfDisplayedMonth)) {
        firstDayInGrid = firstDayInGrid.minusWeeks(1);
      }

      DateTime gridDay = firstDayInGrid;
      DateTime gridEnd;

      // First day of model's month
      DateTime firstDayOfCurrentMonth = new DateTime(
              currentTime.getYear(),
              currentTime.getMonthOfYear(),
              1, 0, 0, 0, 0, DateTimeZone.forID("America/New_York"));

      // First day of next month
      DateTime firstDayOfNextMonth = firstDayOfDisplayedMonth.plusMonths(1);

      if (firstDayOfNextMonth.getDayOfWeek() == calendarStartDay) {
        // Last day of the current month
        gridEnd = firstDayOfDisplayedMonth.dayOfMonth().withMaximumValue();
      } else {
        // Last day (determined by calendarStartDay) of week of the next month
        gridEnd = firstDayOfNextMonth.withDayOfWeek(Math.floorMod(calendarStartDay - 1, 7));
      }

      int column = 0;
      int row = 2;

//      System.out.println(gridDay);
//      System.out.println(gridEnd);

      parent.textFont(Main.fHel_B12, 12);

      while (true) {

        if (column > 6) {
          column = 0;
          ++row;
        }

        renderCell(parent, currentTime, gridDay, row , column);

        column++;
        gridDay = gridDay.plusDays(1);
        if (gridDay.getDayOfYear() == gridEnd.getDayOfYear()) {
          renderCell(parent, currentTime, gridDay, row , column);
          break;
        }
      }

      // Hovered cell overlay
      if (selectedCell.isPresent()) {
        PVector cell = selectedCell.get();
        if (cell.x > 1) {
          parent.fill(255, 100);
          parent.noStroke();
          parent.rect(
                  Math.round(cornerXY.x + cell.y * colWidth),
                  Math.round(cornerXY.y + cell.x * rowHeight + 1),
                  colWidth, rowHeight);
        }
      }
      parent.popStyle();
    }
  }

  int calendarStartDay = DateTimeConstants.SUNDAY;

  private int determineNumberOfRows() {

    // First day in the rendered grid
    firstDayInGrid = firstDayOfDisplayedMonth.withDayOfWeek(calendarStartDay);
    // Case if first day of grid is NOT sunday, then it should be the sunday of the week before, and not this week's Sunday
    if (firstDayInGrid.isAfter(firstDayOfDisplayedMonth)) {
      firstDayInGrid = firstDayInGrid.minusWeeks(1);
    }

    int row = 3;
    int column = 0;

    DateTime firstDayOfNextMonth = firstDayOfDisplayedMonth.plusMonths(1);
    DateTime dayEnd;

    if (firstDayOfNextMonth.getDayOfWeek() == calendarStartDay) {
      // Last day of the current month
      dayEnd = firstDayOfDisplayedMonth.dayOfMonth().withMaximumValue();
    } else {
      // Last day (determined by calendarStartDay) of week of the next month
      dayEnd = firstDayOfNextMonth.withDayOfWeek(Math.floorMod(calendarStartDay - 1, 7));
    }
    DateTime day = firstDayInGrid;

    while (true) {
      if (column > 6) {
        column = 0;
        ++row;
      }
      ++column;
      day = day.plusDays(1);
      if (day.getDayOfYear() == dayEnd.getDayOfYear()) {
        break;
      }
    }
    return row;
  }

  private void renderCell(Main parent, DateTime currentTime, DateTime gridDay, int row, int column) {
    if ((firstDayLimit.isPresent() && gridDay.compareTo(firstDayLimit.get()) < 0)
            || (lastDayLimit.isPresent() && gridDay.compareTo(lastDayLimit.get()) > 0)) {
      parent.pushStyle();
      parent.fill(35);
      parent.noStroke();
      parent.rect(
              Math.round(cornerXY.x + column * colWidth),
              Math.round(cornerXY.y + row * rowHeight + 1),
              colWidth, rowHeight);
      parent.popStyle();
    }

    if (currentTime.getDayOfMonth() == gridDay.getDayOfMonth()
            && currentTime.getMonthOfYear() == gridDay.getMonthOfYear()
            && currentTime.getYear() == gridDay.getYear()) {
      // TODAY
      parent.pushStyle();
      parent.fill(255);
      parent.noStroke();
      parent.rect(
              cornerXY.x + column * colWidth,
              cornerXY.y + row * rowHeight + 1,
              colWidth, rowHeight);
      parent.popStyle();
      parent.fill(0);
      parent.text(gridDay.getDayOfMonth(),
              cornerXY.x + column * colWidth + colWidth / 2,
              cornerXY.y + row * rowHeight + rowHeight / 2 + 1);
    } else if (gridDay.getMonthOfYear() == currentTime.getMonthOfYear()
            && gridDay.getYear() == currentTime.getYear()) {
      // THIS MONTH and THIS YEAR, BUT NOT TODAY
      parent.fill(255);
      parent.text(gridDay.getDayOfMonth(),
              cornerXY.x + column * colWidth + colWidth / 2,
              cornerXY.y + row * rowHeight + rowHeight / 2 + 1);
    } else {
      // OTHER MONTH
      parent.fill(130);
      parent.text(gridDay.getDayOfMonth(),
              cornerXY.x + column * colWidth + colWidth / 2,
              cornerXY.y + row * rowHeight + rowHeight / 2 + 1);
    }
  }

  boolean isShowing = false;
  @Override
  public boolean isMouseOver(float mX, float mY) {
    if (!isShowing) {
      return false;
    }

    return mX > cornerXY.x
            && mX < cornerXY.x + width
            && mY > cornerXY.y
            && mY < cornerXY.y + height;
  }

  @Override
  public void mouseHoverAction(Main controller, MouseEvent event) {
    checkState(event);

    // 1. Update internal view
    // Set the state.
    selectedCell = Optional.of(
            new PVector(
                    Math.floorDiv(event.getY() - cornerXY.y, rowHeight),
                    Math.floorDiv(event.getX() - cornerXY.x, colWidth)));

    if (leftButton.isMouseOver(event.getX(), event.getY())) {
      leftButton.mouseHoverAction(controller, event);
    } else {
      leftButton.removeHoveredStates();
    }

    if (rightButton.isMouseOver(event.getX(), event.getY())) {
      rightButton.mouseHoverAction(controller, event);
    } else {
      rightButton.removeHoveredStates();
    }

    // 2. Interact with the model
    actionSuite.actHovered(controller, event);
  }

  @Override
  public void mouseScrollAction(MouseEvent event, Main controller) {
    // DO NOTHING
  }

  @Override
  public void mousePressedAction(Main controller, MouseEvent event) {
    Objects.requireNonNull(controller);
    Objects.requireNonNull(event);
    checkState(event);
    if (leftButton.isMouseOver(event.getX(), event.getY())) {
      leftButton.mousePressedAction(controller, event);
      return;
    }
    if (rightButton.isMouseOver(event.getX(), event.getY())) {
      rightButton.mousePressedAction(controller, event);
      return;
    }
    actionSuite.actPressed(controller, event);
  }

  public void setVisible(boolean isVisible) {
    isShowing = isVisible;
  }

  @Override
  public void mouseDraggedAction(Main controller, MouseEvent event) {
    // DO NOTHING
  }

  @Override
  public void mouseReleasedAction(Main controller, MouseEvent event) {
    // DO NOTHING
  }

  @Override
  public boolean getFocused() {
    return false;
  }

  @Override
  public void removeHoveredStates() {
    selectedCell = Optional.empty();
  }

  @Override
  public Dimension getDimension() {
    return new Dimension(width, height);
  }

  @Override
  public void checkState(MouseEvent e) {
    if (!isMouseOver(e.getX(), e.getY())) {
      throw new IllegalStateException("You didn't check for this Calendar's state before activation!");
    }
  }

  private ActionSuite actionSuite;
  @Override
  public void bindAction(ActionSuite a) {
    this.actionSuite = a;
  }

  @Override
  public ActionSuite getActionSuite() {
    return actionSuite;
  }

  public void setDisplayedMonth(DateTime displayedMonth) {
    this.firstDayOfDisplayedMonth = displayedMonth;
    height = rowHeight * determineNumberOfRows();
    setCornerXY(cornerXY.x, cornerXY.y);
    selectedCell = Optional.empty();
  }

  public boolean isVisible() {
    return isShowing;
  }

//  private void createShapes(PApplet context) {
//    calendarShape = context.createShape();
//    calendarShape.beginShape();
//    calendarShape.noStroke();
//    calendarShape.vertex(0, 0);
//    calendarShape.vertex(colWidth * 7, 0);
//    calendarShape.vertex(colWidth * 7, rowHeight * 7);
////		calendarShape.vertex((colWidth * 7 - calendarArrowWidth) / 2 + calendarArrowWidth, rowHeight * 7);
////		calendarShape.vertex(colWidth * 7 / 2, rowHeight * 7 + pointHeight); // Point
////		calendarShape.vertex((colWidth * 7 - calendarArrowWidth) / 2, rowHeight * 7);
//    calendarShape.vertex(0, rowHeight * 7);
//    calendarShape.vertex(0, 0);
//    calendarShape.endShape();
//    calendarShape.setFill(context.color(0, 160));
//  }
}