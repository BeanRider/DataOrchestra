package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

import UI.*;
import UI.Button;
import com.google.common.collect.EvictingQueue;
import main.alarm.TimedUIAction;
import main.ui.CalendarModule;
import main.ui.TextButton;
import main.ui.UIAction.Action;
import main.ui.UIAction.ActionSuite;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import UI.ImageButtonOld;
import processing.data.Table;
import processing.data.TableRow;
import processing.event.MouseEvent;
import processing.core.*;

/*
 * TODO Improve lookup efficiency of academic calendar
 */
public class Main extends PApplet {

  // ========
  // RAW DATA
  // ========

  // List of keys
  private Table sensorData;
  private Table buildingDB;
  private Table twitter;
  private Table academicCalendar;
  public Table abbrevToName;
  private Table sensorPropertiesTable;

  private DataSource<Integer> dailyTempDS;
  // private DataSource<Integer> hourlyTempDS;
  private DataSource<Float> totalSensorLevelDS;

  // ==============
  // GRAPHICAL DATA
  // ==============

  /**
   * [BUILDING/MAP]
   */
  private Point2D.Double originLatLong; // Origin (Ell Hall [12])

  private PImage scrubberIcon;

  private PImage satelliteMap;

  // Buildings on Map
  private List<PShape> buildingShapes;
  private PVector[] centroidsXY; // All centroids (x/y) in relation to the origin (Ell Hall)

  // Margin of the HUD
  public int xMargin = 15;
  private int yMargin = -15;

  /**
   * [UI/HUD/APP STATE]
   */
  private Queue<SchoolBuilding> selectedBuildings = EvictingQueue.create(3);

  /**
   * [TIME STATE]
   */
  // Timeline (size = 35136; span one year)
  // start: Sun, 08 Dec 2013 19:00:00 EST;
  // end  : Tue, 09 Dec 2014 18:45:00 EST;
  // interval: 15 min;
  public TimeRangeUtil timeRange;

  private int loopCount = 0; // used for animating scrubber

  private IntervalType howLongIsTimeRange = IntervalType.MONTHLY;
  IntervalType timelineInterval = IntervalType.WEEKLY;

  // ====================
  // STATISTICAL ANALYSIS
  // ====================
  private int numBuildings;
  private float maxSingleBuildingEnergy;
  private int maxSingleBuildingOccupancy;

  // =====
  // FONTS
  // =====
  public static PFont fOswald_130;
  public static PFont fHel_B12;
  public static PFont fTitle;
  public static PFont fHel_L11;
  public static PFont fHel_B14;
  public static PFont fHel_N14;

  // ========
  // APP INFO
  // ========

  private int DETECTED_WIDTH;
  private int DETECTED_HEIGHT;

  private int NATIVE_WIDTH = 1280;
  private int NATIVE_HEIGHT = 800;

  private int mapAndBuildingX = 350;
  private int mapAndBuildingY = 140;

  private TextButton calendarButton;
  private CalendarModule calendar;
  private boolean autoMode = true;

  // PApplet Setup
  public void setup() {
    try {
      satelliteMap = loadImage("map.jpg");
      scrubberIcon = loadImage("Scrubber-01.png");

      fOswald_130 = loadFont("Oswald-130.vlw");
      fHel_B12 = loadFont("HelveticaNeue-Bold-12.vlw");
      fTitle = createFont("Helvetica-Bold", 200, true);
      fHel_L11 = loadFont("Helvetica-11.vlw");
      fHel_B14 = loadFont("Helvetica-Bold-14.vlw");
      fHel_N14 = loadFont("Helvetica-14.vlw");

      wideIcon = loadImage("1.png");
      dayIcon = loadImage("dayIcon.png");

      semesterRanges = loadTable("semesterRanges.csv", "header");
      buildingTypeRenameTable = loadTable("buildingTypeRename.csv", "header");

      sensorPropertiesTable = loadTable("sensorProperties.csv", "header");

      DateTimeZone.setDefault(DateTimeZone.forID("America/New_York"));
      DETECTED_WIDTH = displayWidth;
      DETECTED_HEIGHT = displayHeight;
      println(displayWidth, displayHeight);
      noLoop();
    } catch (Exception | Error e) {
      e.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  private void updateButtonText() {
    yearButton.setText(String.valueOf(timeRange.getDateTimeFor(TimeRangeUtil.CUR).getYear()));
    monthButton.setText(timeRange.getDateTimeFor(TimeRangeUtil.CUR).monthOfYear().getAsText().toUpperCase().substring(0, 3));
    monthButton.setX(yearButton.getX() + yearButton.getWidth() + 7);
    calendarButton.setText(timeRange.getDateTimeFor(TimeRangeUtil.CUR).getDayOfMonth() + "", this);
    calendarButton.setCornerXY(monthButton.getX() + monthButton.getWidth() + 7, calendarButton.getCornerXY().y);
  }

  private boolean isDoingInitialSetup = false;
  ButtonBindings bindings = new ButtonBindings(this);

  private TimedUIAction autoCameraTimer;

  private void load() {
    final int FPS = 60;
    autoCameraTimer = new TimedUIAction(FPS * 30);
    autoCameraTimer.addCompletionAction(new Action() {
      @Override
      public void act(Main controller, MouseEvent e) {
        autoMode = true;
        isMovingThroughTime = true;
        timePinTimeInterval = 6;
        resetFadeInText();
        if (inspectorState != 'O') {
          inspectorButton.activate();
        }
        scale = 0.5f;
        mapAndBuildingX = 350;
        mapAndBuildingY = 140;
      }
    });


    unitkWButton = new ImageButtonOld(DETECTED_WIDTH - xMargin - 42, xMargin,
            loadImage("kwButton.png"),
            null, bindings, this);
    unitkWButton.toggleEnable(); // Disable it when selected

    unitNormalizedButton = new ImageButtonOld(DETECTED_WIDTH - xMargin - 42, xMargin + 20,
            loadImage("normalizedButton.png"),
            null, bindings, this);

    inspectorButton = new RoundImageButtonOld(xMargin, DETECTED_HEIGHT - 240 + yMargin,
            loadImage("inspectorButton.png"),
            null,bindings, this);

//    barSettings = new RoundImageButtonOld(xMargin + 100, DETECTED_HEIGHT - 240 + yMargin,
//            loadImage("plusButton.png"), bindings, this);

    playButton = new RoundImageButtonOld(
            DETECTED_WIDTH / 2 - loadImage("startButton.png").width / 2,
            DETECTED_HEIGHT - 233 + yMargin - loadImage("startButton.png").height / 2,
            loadImage("startButton.png"),
            loadImage("pauseButton.png"), bindings, this);

    stepBackButton = new RoundImageButtonOld(
            DETECTED_WIDTH / 2 - 30 - loadImage("stepBackButton.png").width / 2,
            DETECTED_HEIGHT - 241 + yMargin,
            loadImage("stepBackButton.png"),
            null, bindings, this);

    stepForwardButton = new RoundImageButtonOld(
            DETECTED_WIDTH / 2 + 30 - loadImage("stepForwardButton.png").width / 2,
            DETECTED_HEIGHT - 241 + yMargin,
            loadImage("stepForwardButton.png"),
            null, bindings, this);

    bindings.bind(unitkWButton, new VisualButtonEvent() {
      @Override
      public void act() {
//        println("unit Activated!");
        unit = 'S';
        unitkWButton.toggleEnable();
        unitNormalizedButton.toggleEnable();
      }
    });

    bindings.bind(unitNormalizedButton, new VisualButtonEvent() {
      @Override
      public void act() {
//        println("unit Activated!");
        unit = 'N';
        unitkWButton.toggleEnable();
        unitNormalizedButton.toggleEnable();
      }
    });

    bindings.bind(inspectorButton, new VisualButtonEvent() {
      @Override
      public void act() {
        if (inspectorState == 'C') {
          inspectorState = 'O';
        } else {
          inspectorState = 'C';
        }
      }
    });

//    bindings.bind(barSettings, new VisualButtonEvent() {
//      @Override
//      public void act() {
//        println("Plus Button Activated!");
//        if (inspectorState == 'C') {
//          throw new RuntimeException("You can't use plus when inspector is closed!");
//        } else if (inspectorState == 'O') {
//          inspectorState = 'E';
//        } else if (inspectorState == 'E') {
//          inspectorState = 'O';
//        }
//      }
//    });

    bindings.bind(playButton, new VisualButtonEvent() {
      @Override
      public void act() {
        isMovingThroughTime = !isMovingThroughTime;
      }
    });

    bindings.bind(stepBackButton, new VisualButtonEvent() {
      @Override
      public void act() {
        if (timeRange.getCurIdx() == 0) {
          timeRange.jumpToPrevSection();
          indexDataWithNewRange();
        } else {
          timeRange.scrubTo(0);
        }
//        printConfirm();
      }
    });

    bindings.bind(stepForwardButton, new VisualButtonEvent() {
      @Override
      public void act() {
        timeRange.jumpToNextSection();
        indexDataWithNewRange();
//        printConfirm();
      }
    });

    // Time
    timeRange = new TimeRangeUtil(1385874000, this.howLongIsTimeRange);

    // [START: DATA PORTION]
    // Raw Data
    readDataFiles(loadTable("informationAboutData.csv", "header"));

    numBuildings = buildingDB.getRowCount();

    abbrevToName = loadTable("buildingID.csv", "header");

    Table entireSchoolSum = loadTable("measureDB_parallel_totals_div4.csv", "header");
    totalSensorLevelDS = new DataSourceImplFloat(entireSchoolSum,
            timeRange.getStartUnix(), timeRange.getEndUnix(),
            IntervalType.QUARTER, howLongIsTimeRange, "school", "timestamp");

    initializeMaxOccupancyLevel();
    indexCampusOcc();

    // Statistical analysis
    initializeTweetHoursInDay();


//    maxSingleBuildingEnergy = getMaxSingleBuildingEnergyLevel();
//    maxSingleBuildingOccupancy = getMaxSingleBuildingOccupancyLevel();
    // TODO using pre-calculated values
    maxSingleBuildingEnergy = 5003.44f / 4f;
    maxSingleBuildingOccupancy = 998;
    println("maxSingleBuildingEnergy = " + maxSingleBuildingEnergy);
    println("maxSingleBuildingOccupancy = " + maxSingleBuildingOccupancy);

    // [END: DATA PORTION]

    // [START: GRAPHICS PORTION]
    // Rendering
    initRenderingData();

    // Time related controls:
    // Calendar
    calendar = new CalendarModule(
            displayWidth - 226,
            displayHeight - 508,
            loadImage("arrowLeft.png"),
            loadImage("arrowRight.png"),
            timeRange.getDateForIndex(0));
    calendar.setFirstDayLimit(new DateTime(1356670800 * 1000L));
    calendar.setLastDayLimit(new DateTime(1418101200 * 1000L));
    ActionSuite calendarSuit = new ActionSuite();
    calendar.bindAction(calendarSuit);
    calendarSuit.setPressedAction(new Action() {
      @Override
      public void act(Main controller, MouseEvent e) {
        Optional<DateTime> hoveredDate = calendar.getHoveredDate();
        if (!hoveredDate.isPresent()) {
          return;
        }

        // Switch to the new month to display
        calendar.setDisplayedMonth(hoveredDate.get().withDayOfMonth(1));
//        timeRange = new TimeRangeUtil(hoveredDate.get().getMillis() / 1000L, howLongIsTimeRange);
        howLongIsTimeRange = IntervalType.DAILY;
        timelineInterval = IntervalType.HOURLY;
        timeRange.updateTime(hoveredDate.get().getMillis() / 1000L, IntervalType.DAILY);
//        System.out.println("After calendar day pressed, timeline date: " + hoveredDate.get().getMillis() / 1000L);
//        controller.setTimePercentage(0); TODO set time percentage if converting to smooth animation
        indexDataWithNewRange();
      }
    });

    yearButton = new TextButtonOld(
            calendar.getCornerXY().x,
            DETECTED_HEIGHT - 245 + yMargin,
            "", fHel_B14, bindings, this);

    monthButton = new TextButtonOld(
            yearButton.getX() + yearButton.getWidth() + 7,
            DETECTED_HEIGHT - 245 + yMargin,
            "",fHel_B14, bindings, this);

    calendarButton = new TextButton(
            new Point(
                    monthButton.getX() + monthButton.getWidth() + 7 ,
                    DETECTED_HEIGHT - 245 + yMargin),
            "",
            this); // used to have x = 221
    ActionSuite calendarSuite = new ActionSuite();
    calendarButton.bindAction(calendarSuite);
    calendarSuite.setPressedAction(new Action() {
      @Override
      public void act(Main controller, MouseEvent e) {
        calendar.setVisible(!calendar.isVisible());
        calendar.setDisplayedMonth(timeRange.getDateTimeFor(TimeRangeUtil.START).withDayOfMonth(1));
      }
    });

    bindings.bind(yearButton, new VisualButtonEvent() {
      @Override
      public void act() {
        howLongIsTimeRange = IntervalType.YEARLY;
        timelineInterval = IntervalType.MONTHLY;
        timeRange.updateTime(TimeRangeUtil.floorYear(timeRange.getCurUnix()), howLongIsTimeRange);
        indexDataWithNewRange();
//        printConfirm();
      }
    });

    bindings.bind(monthButton, new VisualButtonEvent() {
      @Override
      public void act() {
        howLongIsTimeRange = IntervalType.MONTHLY;
        timelineInterval = IntervalType.WEEKLY;
        timeRange = new TimeRangeUtil(TimeRangeUtil.floorMonth(timeRange.getCurUnix()), howLongIsTimeRange);
        indexDataWithNewRange();
//        printConfirm();
      }
    });

    allBars = new ArrayList<>(6);

    timebar = visualizeTimebar(timelineInterval);
    temperatureBar = visualizeDailyTemp(0, 35);
    electricityBar = visualizeSensorGraph(70);
    twitterBar = visualizeTwitterData(19);
    occupancyBar = visualizeOccupancyData(35);
    academicBar = visualizeAcademicCalendar(35);

    allBars.add(new VisualBar(timebar, xMargin, DETECTED_HEIGHT - 212 + yMargin, this, 0, timebarHeight));
    allBars.add(new VisualBar(temperatureBar, xMargin, DETECTED_HEIGHT - 193 + yMargin, this, 1, 35));
    allBars.add(new VisualBar(electricityBar, xMargin, DETECTED_HEIGHT - 158 + yMargin, this, 2, 70));
    allBars.add(new VisualBar(twitterBar, xMargin, DETECTED_HEIGHT - 89 + yMargin, this, 3, 19));
    allBars.add(new VisualBar(occupancyBar, xMargin, DETECTED_HEIGHT - 70 + yMargin, this, 4, 35));
    allBars.add(new VisualBar(academicBar, xMargin, DETECTED_HEIGHT - 35 + yMargin, this, 5, 35));

    timeScrubber = new Scrubber();

    sign = createShape();
    sign.beginShape();
    sign.noStroke();
    sign.vertex(0, 0);
    sign.vertex(mainPanelW, 0);
    sign.vertex(mainPanelW + pointerW, pointerH);
    sign.vertex(mainPanelW, pointerW);
    sign.vertex(mainPanelW, mainPanelH);
    sign.vertex(0, mainPanelH);
    sign.vertex(0, 0);
    sign.endShape();
    sign.setFill(color(0, 200));

    // [END: GRAPHICS PORTION]

//    printConfirm();
    isDoingInitialSetup = false;
    isLoading = false;

    updateButtonText();
  }

  float mainPanelW = 142 + 50;
  float mainPanelH = 68 + 20;
  float pointerW = 20;
  float pointerH = 35;

  // STATISTICAL METHODS
  int getMaxSingleBuildingOccupancyLevel() {
    List<Integer> buildingMaxes = new ArrayList<>(numBuildings);

    long startOcc = semesterRanges.getRow(0).getLong("startUnix");
    long endOcc = semesterRanges.getRow(semesterRanges.getRowCount() - 1).getLong("endUnix");

    // 1. Get all occupancy levels for each building, then copy the max for each
    for (int i = 0; i < numBuildings; ++i) {
      SchoolBuilding currentBuilding = new SchoolBuilding(i, startOcc, endOcc, this);
      if (currentBuilding.releventOccupancyValues.size() != 0)
        buildingMaxes.add(Collections.max(currentBuilding.releventOccupancyValues).getValue());
    }

    // 2. Get the biggest of the maxes from each building
//    println("Occupancy Max Found to Be = " + Collections.max(buildingMaxes));
    return Collections.max(buildingMaxes);
  }

  float getMaxSingleBuildingEnergyLevel() {
    float maxEnergyEver = 0;
    for (int i = 0; i < numBuildings; ++i) {
      if (i == 23) {
        i += 5;
      } else if (i == 43) {
        i += 1;
      }

      float possibleMax = getMaxEnergyValueOfBuilding(i);
      if (possibleMax > maxEnergyEver) {
        maxEnergyEver = possibleMax;
      }
    }
    return maxEnergyEver;
  }

  float getMaxEnergyValueOfBuilding(int bNum) {
    float max = 0;
    for (TableRow row : sensorData.rows()) {

      Float sensorValue = row.getFloat(Integer.toString(bNum));

      if (bNum == 23) {
        sensorValue += row.getInt(Integer.toString(bNum + 1));
        sensorValue += row.getInt(Integer.toString(bNum + 2));
        sensorValue += row.getInt(Integer.toString(bNum + 3));
        sensorValue += row.getInt(Integer.toString(bNum + 4));
        sensorValue += row.getInt(Integer.toString(bNum + 5));
      } else if (bNum == 43) {
        sensorValue += row.getInt(Integer.toString(bNum + 1));
      }

      if (sensorValue > max) {
        max = sensorValue;
      }
    }
    return max;
  }

  PShape sign;
  List<VisualBar> allBars;
  PGraphics timebar;
  PGraphics temperatureBar;
  PGraphics electricityBar;
  PGraphics twitterBar;
  PGraphics occupancyBar;
  PGraphics academicBar;

  void printConfirm() {

//		println("=== ORIGIN =====================");
//		println(Double.toString(originLatLong.x) +", " + Double.toString(originLatLong.y));
//		println();
//		
//		println("=== TIMELINE =====================");
//    	println("unix range: " + timeRange.getStartUnix() + " - " + timeRange.getEndUnix());
//    	println("data search range: " + timeRange.getStartUnix() + " - " + timeRange.getLastUnixSegment());
//    	println("numberOfSegments = " + timeRange.length());
//    	println("date range: "
//    	+ timeRange.formatDate(timeRange.getStartUnix()) + ", " + timeRange.getTime(TimeRangeUtil.START)
//    	+ " - " + 
//    	timeRange.formatDate(timeRange.getEndUnix()) + ", " + timeRange.getTime(TimeRangeUtil.END));
//    	println();
//    	
//		println("Ready!");
  }

  // Every time the timerange changes to a new range, call this to ensure the raw data gets re-indexed as well.
  void indexDataWithNewRange() {
    dailyTempDS.updateStats(timeRange.getStartUnix(), timeRange.getEndUnix(), IntervalType.DAILY, howLongIsTimeRange);
    //hourlyTempDS.updateStats(this.timeRange.getStartUnix(), this.timeRange.getEndUnix(), IntervalType.HOURLY, howLongIsTimeRange);
    totalSensorLevelDS.updateStats(timeRange.getStartUnix(), timeRange.getEndUnix(), IntervalType.QUARTER, howLongIsTimeRange);
    indexCampusOcc(); // SPECIAL: occupancy has indexing stuff.
    for (SchoolBuilding s : this.selectedBuildings) {
      s.indexOccupancyValues(timeRange);
    }
    allBars.forEach(VisualBar::refresh);
  }

  // initialize dataTables from the given meta-data for the data sheets
  void readDataFiles(Table metadata) {
    HashMap<String, Table> dataTables = new HashMap<>();
    for (TableRow row : metadata.rows()) {
      String dataSheetName = row.getString("dataSheetName");
      String filePath = row.getString("filePath");
      dataTables.put(dataSheetName, loadTable(filePath, "header"));
    }
    sensorData = loadTable("measureDB_parallel_div4_with_sums.csv", "header");
    buildingDB = dataTables.get("buildingDB");
    twitter = dataTables.get("twitter");
    academicCalendar = dataTables.get("academicCalendar");

    Table dailyWeather = dataTables.get("dailyWeather");
//		Table hourlyTemp = dataTables.get("hourlyTemp");

    dailyTempDS = new DataSourceImpl(dailyWeather,
            timeRange.getStartUnix(), timeRange.getEndUnix(),
            IntervalType.DAILY, howLongIsTimeRange, "Mean.TemperatureF", "timestamp");
//		hourlyTempDS = new DataSourceImpl(hourlyTemp,
//				timeRange.getStartUnix(), timeRange.getEndUnix(),
//				IntervalType.HOURLY, howLongIsTimeRange, "DryBulbFarenheit", "unix");

  }

  // Init. the origin, the centroids, the marker shapes, the building polygons,
  private void initRenderingData() {

    // Origin
    originLatLong = StringParseUtil.parseCentroidString(buildingDB.getRow(12).getString("Centroid"));

    // Array of PVector centroids (x/y as screen)
    centroidsXY = new PVector[numBuildings];
    for (int r = 0; r < numBuildings; ++r) {
      this.centroidsXY[r] = GeoUtil.convertGeoToScreen(
              StringParseUtil.parseCentroidString(buildingDB.getRow(r).getString("Centroid")),
              originLatLong,
              NATIVE_WIDTH,
              NATIVE_HEIGHT);
    }

    // List of PShape Building Outlines
    buildingShapes = new ArrayList<>(numBuildings);
    for (int i = 0; i < numBuildings; ++i) {
      TableRow row = buildingDB.getRow(i);
      this.buildingShapes.add(buildShape(row, this.getBuildingColor(row.getString("Primary Use"), false)));
    }
  }

  // =======================================
  // Methods That Initialize Graphical Data:
  // =======================================

  // Returns a new PShape based on:
  // building row, and color
  PShape buildShape(TableRow row, int c) {
    // Parse a String (set of sets) -> a list of String/Points (list of points)
    List<String> verticesAsStringSets = StringParseUtil.getListFromSet(row.getString("Outline"));
    // Make an outline of this particular shape
    PShape outline = createShape();
    outline.beginShape();
    outline.fill(c, 150);
    outline.strokeWeight(1.8f);
    outline.stroke(color(255, 255, 255));
    for (String s : verticesAsStringSets) {
      PVector v = GeoUtil.convertGeoToScreen(
              StringParseUtil.parseCentroidString(s),
              this.originLatLong,
              NATIVE_WIDTH,
              NATIVE_HEIGHT); // Converts to a PVector using 1280 X 800
      outline.vertex(v.x, v.y); // adds adjustment to keep it centered
    }
    outline.endShape(CLOSE);
    return outline;
  }

  // Returns the building color depending on the given primary use
  int getBuildingColor(String primaryUse, boolean isNewName) {
    String buildingColorAsString = null;
    for (int i = 0; i < buildingTypeRenameTable.getRowCount(); ++i) {
      TableRow row = buildingTypeRenameTable.getRow(i);
      if (isNewName && row.getString("newName").equals(primaryUse)) {
        buildingColorAsString = row.getString("rgb");
      } else if (!isNewName && row.getString("oldName").equals(primaryUse)) {
        buildingColorAsString = row.getString("rgb");
      }
    }
    Objects.requireNonNull(buildingColorAsString);
    String[] buildingRGBAsString = buildingColorAsString.split(",");
    int[] buildingRGB = new int[3];
    for (int i = 0; i < 3; ++i) {
      String s = buildingRGBAsString[i];
      buildingRGB[i] = Integer.parseInt(s.trim());
    }
    return color(buildingRGB[0], buildingRGB[1], buildingRGB[2]);
  }

  // Returns the building color depending on the given primary use
  String getNewBuildingName(String primaryUse, boolean isNewName) {
    String newName = null;
    for (int i = 0; i < buildingTypeRenameTable.getRowCount(); ++i) {
      TableRow row = buildingTypeRenameTable.getRow(i);
      if (isNewName && row.getString("newName").equals(primaryUse)) {
        newName = row.getString("newName");
      } else if (!isNewName && row.getString("oldName").equals(primaryUse)) {
        newName = row.getString("newName");
      }
    }
    return newName;
  }

  // ===============================
  // Methods that Constructs the Bar
  // ===============================

  // Visualizing the outlines of buildings
  void visualizeMapAndBuildings() {
    strokeWeight(4);
    stroke(255);
    buildingShapes.forEach(this::shape);
  }

  // Visualizing the sensor data circles
  void visualizeSensorData(boolean enabled) {
    if (enabled) {
      for (int i = 0; i < numBuildings; ++i) {
        drawSensorCircle(i);
      }
    }
  }

  // Visualizing the timebar
  int timebarHeight = 17;

  public PGraphics visualizeTimebar(IntervalType segmentLength) {
    int mainW = DETECTED_WIDTH - xMargin * 2;
    int mainH = timebarHeight;

    PGraphics output = createGraphics(mainW, mainH);
    output.beginDraw();

    // Positioning and Dimensions
    float mainX = 0;
    float mainY = 0;

    // How many segments to render?
    int numSegments = timeRange.getNumberOfSegmentsOf(segmentLength);
    float segmentWidth = (float) mainW / (float) numSegments;

    if (segmentWidth < 5) {
      throw new RuntimeException("Segment Width is overly tiny: " + segmentWidth
              + ". Consider increasing the segment IntervalType!");
    }

    // Render the segments:
    for (int cellNo = 0; cellNo < numSegments; ++cellNo) {
      switch (segmentLength) {
        case HOURLY:
          DateTime cellTime = new DateTime(
                  (timeRange.getStartUnix() + cellNo * 60 * 60) * 1000L, DateTimeZone.forID("America/New_York"));
          if (cellTime.getDayOfWeek() == 6 || cellTime.getDayOfWeek() == 7) {
            output.fill(80, 230);
          } else {
            output.fill(0, 230);
          }
          break;
        case WEEKLY:
          // Bleeds through weekly
        case DAILY:
          cellTime = new DateTime(
                  (timeRange.getStartUnix() + cellNo * 24 * 60 * 60) * 1000L, DateTimeZone.forID("America/New_York"));
          if (cellTime.getDayOfWeek() == 6 || cellTime.getDayOfWeek() == 7) {
            output.fill(80, 230);
          } else {
            output.fill(0, 230);
          }
          // Bleeds through weekly
          break;
        default:
          output.fill(0, 230);
          break;
      }
      output.noStroke();
      output.rect(mainX + cellNo * segmentWidth, mainY, segmentWidth, mainH);

      if (cellNo != 0) {
        output.stroke(color(255));
        int shrink = 4;
        output.line(mainX + cellNo * segmentWidth, mainY + shrink,
                mainX + cellNo * segmentWidth, mainY + mainH - shrink);
      }

      output.fill(255);
      output.noStroke();
      output.textAlign(CENTER, TOP);
      output.textFont(fHel_B12, 12);
      if (IntervalType.HOURLY == segmentLength) {
        output.text(cellNo + 1 % 24, // 1st hour --- 24th hour
                mainX + cellNo * segmentWidth + segmentWidth / 2,
                mainY + 4);
      } else if (IntervalType.MONTHLY == segmentLength) {
        String contents = new DateTime(1970, cellNo + 1, 1, 0, 0, DateTimeZone.forID("America/New_York")).toString("MMM").toUpperCase();
        output.text(contents,
                mainX + cellNo * segmentWidth + segmentWidth / 2,
                mainY + 4);
      } else if (IntervalType.WEEKLY == howLongIsTimeRange) {
        DateTime date = new DateTime(DateTimeZone.forID("America/New_York"));
        date = date.withDayOfWeek(cellNo + 1);
        String contents = date.dayOfWeek().getAsText();
        output.text(contents,
                mainX + cellNo * segmentWidth + segmentWidth / 2,
                mainY + 4);
      } else {
        output.text(cellNo + 1,
                mainX + cellNo * segmentWidth + segmentWidth / 2,
                mainY + 4);
      }
    }
    output.endDraw();
    return output;
  }

  // Visualizing the hourTemp (-1 for off; 0 for daily; 1 for hourly)
  PGraphics visualizeDailyTemp(int mode, float h) {

    float adjustment = -1;
    float x = 0 + adjustment;
    float y = 0;
    float mainW = DETECTED_WIDTH - 2 * xMargin;

    PGraphics output = createGraphics((int) mainW, (int) h, JAVA2D);
    output.beginDraw();

    // Background bar (darkgrey)
    output.noStroke();
    output.fill(20, 230);
    output.rect(x, y, DETECTED_WIDTH - 2 * x, h);

    // Learn about how many days/hours to visualize
    int numHours = timeRange.length() / 4;
    int numDays = numHours / 24;

    float barWidth = mainW / (float) numDays;
    DataSource<Integer> source = dailyTempDS;

    if (mode == 1) {
      barWidth = (DETECTED_WIDTH - 2 * x) / (float) numHours;
      // source = hourlyTempDS;
    }

    if (source == null || source.maxValue == null) {
      output.endDraw();
      return output; // Meaning no values are present
    }
    // loop through indexed array and draw the things.
    int dayOrHour = 0;
    for (Integer i : source.getIndexedArray()) {
      if (i != null) {
        int meanTemp = i;
        int maxTemp = source.maxValue;

        //println("Max temp = " + maxTemp + " vs. " + meanTemp);
        // Draw the visual
        float heightRatio = (float) meanTemp / (float) maxTemp;
//							println(heightRatio);
        float valueHeight = heightRatio * h;
//							println(valueHeight);

        output.fill(color(255, 255, 255));
        output.rect(x + dayOrHour * barWidth, y + h - valueHeight, barWidth, 1);

        if (barWidth > 24) {
          output.noStroke();
          output.textFont(fHel_B12, 12);
          if (valueHeight < 15) {
            output.text(meanTemp, x + dayOrHour * barWidth, y + h - valueHeight - 2);
          } else {
            output.text(meanTemp, x + dayOrHour * barWidth, y + h - valueHeight + 12);
          }
        }
      }
      ++dayOrHour;
    }
    output.endDraw();
    return output;

  }

  // Visualizes the sensor graph (the total amount of energy used for all buildings at a particular time)
  PGraphics visualizeSensorGraph(float h) {

    float mainX = 0;
    float mainY = 0;
    float mainW = DETECTED_WIDTH - 2 * xMargin;

    PGraphics output = createGraphics((int) mainW, (int) h, JAVA2D);
    output.beginDraw();

    // Background
    output.noStroke();
    output.fill(20, 230);
    output.rect(mainX, mainY, mainW, h);

    // Graph
    if (totalSensorLevelDS == null) {
      output.endDraw();
      return output;
    }

    float segmentWidth = mainW / (float) totalSensorLevelDS.getIndexedArray().length;
    if (howLongIsTimeRange == IntervalType.YEARLY) {
      // Yearly is too packed
      segmentWidth *= 54f; // 18 hour interval
    }
    float oldX = 0;
    float oldY = 0;
    output.stroke(255);
    output.strokeWeight(2);
    output.fill(255);
    int segmentNumber = 0;

    Float[] source = totalSensorLevelDS.getIndexedArray();
    int interval = 1;
    if (howLongIsTimeRange == IntervalType.YEARLY) {
      interval = 54;
    }
    for (int index = 0; index < source.length; index += interval) {

      Float value = source[index];

      if (value == null) {
        // Ignore, first point not found, OR there is a gap here
      } else if (index == 0 || source[index - interval] == null) {
        // Value exists, and: it is the first value OR the first value after a null.
        oldX = mainX + segmentNumber * segmentWidth;
        oldY = mainY + h - ((value - totalSensorLevelDS.minValue) / (totalSensorLevelDS.maxValue - totalSensorLevelDS.minValue)) * h;
      } else {
        float newPointX = mainX + segmentNumber * segmentWidth;
        float newPointY = mainY + h - ((value - totalSensorLevelDS.minValue) / (totalSensorLevelDS.maxValue - totalSensorLevelDS.minValue)) * h;
        output.line(oldX, oldY, newPointX, newPointY);
        oldX = newPointX;
        oldY = newPointY;
      }
      ++segmentNumber;
    }
    output.endDraw();
    return output;
  }

  // Visualizes the twitter data on a [DAILY]
  PGraphics visualizeTwitterData(float h) {

    float mainX = 0;
    float mainY = 0;
    float mainW = DETECTED_WIDTH - 2 * xMargin;

    PGraphics output = createGraphics((int) mainW, (int) h, JAVA2D);
    output.beginDraw();

    output.noStroke();
    // Background bar (red for error checking)
//    output.fill(20, 230);
    output.fill(255);
    output.rect(mainX, mainY, DETECTED_WIDTH - mainX * 2 - 0.5f, h);

    int numDays = timeRange.howManyDays();
    // 1. Find how wide each bar should be.
    float barWidth = mainW / (float) numDays;
    if (barWidth < 1) { // if barWidth is practically invisible
      barWidth = 1;
    }

    // 2. Drawing the bars
    for (int day = 0; day < numDays; ++day) {
      Long dateKey = TimeRangeUtil.floorDate(timeRange.getStartUnix() + day * 24 * 60 * 60);
      if (!tweetHoursInDay.containsKey(dateKey)) {
        //println(timeRange.formatDate(dateKey) + " NOT FOUND!");
        // Make a blue:
        output.noStroke();
        output.fill(255);
        output.rect(mainX + (day * barWidth), mainY, barWidth, h);

      } else {
        // Make a bar with greyscaling
        int tint = this.tweetCountToColor(this.tweetHoursInDay.get(dateKey).size(),
                255, 250, 255,
                10, 10, 10);
        output.noStroke();
        output.fill(tint);
        output.rect(mainX + (day * barWidth), mainY, barWidth, h);
      }
      if (day != 0 && howLongIsTimeRange != IntervalType.YEARLY) {
        output.strokeWeight(1);
        output.stroke(color(255));
        output.line(mainX + (day * barWidth), mainY, mainX + (day * barWidth), mainY + h - 1);
      }
    }
    output.endDraw();
    return output;
  }

  // Visualizing the calendar data
  PGraphics visualizeAcademicCalendar(float h) {
    float x = 0;
    float y = 0;

    float mainW = DETECTED_WIDTH - 2 * xMargin;

    PGraphics output = createGraphics((int) mainW, (int) h, JAVA2D);
    output.beginDraw();

    // Background bar
    output.noStroke();
    output.fill(20, 230);
    output.rect(x, y, mainW, h);

    if (academicCalendar == null) {
      output.endDraw();
      return output;
    }

    int numHours = timeRange.length() / 4;
    int numDays = numHours / 24;
    float barWidth = mainW / (float) numDays;
    // Case 1: Less than a day; only show one bar
    if (numDays == 0) {
      Date date = timeRange.getDateForIndex(0).toDate();
      String searchForDate = "," + TimeRangeUtil.slashesFormater.format(date);

      TableRow currentSchoolDay = null;
      for (TableRow row : academicCalendar.rows()) {
        //println("Finding..." + searchForDate + " vs "+ row.getString("Date"));
        if (searchForDate.equals(row.getString("Date"))) {
          //println("Found date! = " + searchForDate);
          currentSchoolDay = row;
          break;
        }
      }

      // No such school day found in academicCalendar
      if (currentSchoolDay == null) {
        output.endDraw();
        return output;
      }

      output.strokeWeight(1);
      output.stroke(color(118, 118, 118));
      // First bar: exams
      if (currentSchoolDay.getInt("Exam") == 1) {
        output.fill(255);
        output.rect(x, y + 5, mainW, 10);
      }

      // Second bar: holidays
      if (currentSchoolDay.getInt("Holiday") == 1) {
        output.fill(60);
        output.rect(x, y + 20, mainW, 10);
      }

      // Third bar: lectures
      if (currentSchoolDay.getInt("Lecture") == 1) {
        output.fill(255);
        output.rect(x, y + 20, mainW, 10);
      }

    } else {
      // println("numDays = " + numDays);

      for (int i = 0; i < numDays; ++i) {
        TableRow currentSchoolDay = null;

        // println("Getting index = "  + i * 24 * 4);
        Date date = timeRange.getDateForIndex(i * 24 * 4).toDate();
        String searchForDate = "," + TimeRangeUtil.slashesFormater.format(date);

        // Search for date
        for (TableRow row : academicCalendar.rows()) {
          // println("Finding..." + searchForDate + " vs "+ row.getString("Date"));
          if (searchForDate.equals(row.getString("Date"))) {
            // println("Found date! = " + searchForDate);
            currentSchoolDay = row;
            break;
          }
        }

        if (currentSchoolDay == null) {
          continue;
        }

        output.strokeWeight(1);
        output.stroke(color(118, 118, 118));
        // First bar: exams
        if (currentSchoolDay.getInt("Exam") == 1) {
          output.fill(255);
          output.rect(x + barWidth * i, y + 5, barWidth, 10);
        }

        // Second bar: holidays
        if (currentSchoolDay.getInt("Holiday") == 1) {
          output.fill(60);
          output.rect(x + barWidth * i, y + 20, barWidth, 10);
        }

        // Third bar: lectures
        if (currentSchoolDay.getInt("Lecture") == 1) {
          output.fill(255);
          output.rect(x + barWidth * i, y + 20, barWidth, 10);
        }
      }
    }
    output.endDraw();
    return output;
  }

  // Records when ("in hour numbers") a tweet is made in a day
  HashMap<Long, ArrayList<Integer>> tweetHoursInDay = new HashMap<>();

  void initializeTweetHoursInDay() {

    long start = System.nanoTime();

    long currentDay = TimeRangeUtil.floorDate(twitter.getRow(0).getLong("timestampUnix"));
    ArrayList<Integer> hoursInADay = new ArrayList<>();

    // Run through every single tweet in data
    for (int r = 0; r < twitter.getRowCount(); ++r) {
      TableRow row = this.twitter.getRow(r);
      // for each tweet;
      // If the date is NOT the current day, add the old list to the mainlist,
      // then make a new list, finally add the 1st new hour in.
      long dateUnix = TimeRangeUtil.floorDate(row.getLong("timestampUnix"));
      //println(row.getLong("timestampUnix") + "->" + dateUnix);
      if (dateUnix != currentDay) {
        //println(dateUnix + " != currentDay: " + currentDay);
        this.tweetHoursInDay.put(currentDay, hoursInADay);
        hoursInADay = new ArrayList<>();
        hoursInADay.add(row.getInt("timestampHourV2"));
        currentDay = dateUnix;
      }
      // If the date is the current day, continue the current list
      else {
        //println("result: YES");
        hoursInADay.add(row.getInt("timestampHourV2"));
        //println(hoursInADay);
      }
    }
    this.tweetHoursInDay.put(currentDay, hoursInADay);

//    println("initializeTweetHoursInDay took " + (System.nanoTime() - start) + " ns!");
  }

  int tweetCountToColor(int tweets, float r1, float g1, float b1, float r2, float g2, float b2) {
    float linearPercentage;
    if (tweets < 0) {
      throw new RuntimeException("\"" + tweets + "\" is not a valid number.");
    } else if (tweets == 0) {
      linearPercentage = 1f;
    } else if (tweets > 0 && tweets <= 8) {
      linearPercentage = 0.8f;
    } else if (tweets > 8 && tweets <= 16) {
      linearPercentage = 0.6f;
    } else if (tweets > 16 && tweets <= 24) {
      linearPercentage = 0.4f;
    } else if (tweets > 24 && tweets <= 32) {
      linearPercentage = 0.2f;
    } else {
      linearPercentage = 0f;
    }
    return color(
            r1 * linearPercentage + r2 * (1 - linearPercentage),
            g1 * linearPercentage + g2 * (1 - linearPercentage),
            b1 * linearPercentage + b2 * (1 - linearPercentage));
  }

  TableRow searchForRowAtUT(long unix) {
    if (unix < sensorData.getRow(0).getLong("timestamp") ||
            unix > sensorData.getRow(sensorData.getRowCount() - 1).getLong("timestamp")) {
      return null;
    }

    for (int i = 0; i < sensorData.getRowCount(); ++i) {

      TableRow row = sensorData.getRow(i);
      if (row.getLong("timestamp") < unix) {
        // Try to jump ahead
        long difference = unix - row.getLong("timestamp");
        i += (difference / 60 / 15) - 1;
      } else if (row.getLong("timestamp") > unix) {
        return null;
      } else {
        return row;
      }
    }
    return null;
  }

  char unit = 'S';

  private float getBuildingEnergyAtCurrentTime(int bNum) {
    TableRow row = searchForRowAtUT(timeRange.getCurUnix());
    // Early termination
    if (row == null) {
      return 0;
    }

    List<Integer> relevantSensors = new ArrayList<>();
    for (TableRow r : sensorPropertiesTable.rows()) {
      if (r.getInt("bID") == bNum) {
        relevantSensors.add(r.getInt("sID"));
      }
    }

    float buildingkW = 0;
    for (Integer sID : relevantSensors) {
      Float sensorValue = row.getFloat(sID.toString());
      if (Float.isNaN(sensorValue)) {
        buildingkW += 0;
      } else {
        buildingkW += sensorValue;
      }
    }

    return buildingkW;
  }

  // Draws a single circle based on given bNum, and mode
  private void drawSensorCircle(int bNum) {

    float sensorValue = getBuildingEnergyAtCurrentTime(bNum);

    if (sensorValue == 0) {
      return;
    }

    float visualDiameter;
    if (unit == 'N') {
      sensorValue = sensorValue / this.buildingDB.getRow(bNum).getFloat("Area");
      visualDiameter = sensorValue * 1600;
    } else {
      visualDiameter = sensorValue / 5f;
    }

    // Energy circle
    noStroke();
    fill(255, 150);

    pushMatrix();
    translate(centroidsXY[bNum].x, centroidsXY[bNum].y);
    ellipseMode(CENTER);
    ellipse(0, 0, visualDiameter, visualDiameter);
    popMatrix();
  }

  /**
   * [Processing Method: Draw]
   */
  boolean isLoading = true;

  public void draw() {
    try {

      background(0);

      if (!isLoading) {
        updateButtonText();
        // ================================================
        // MAP SECTION:
        pushMatrix();
        scale(scale);
        translate(mapAndBuildingX / scale, mapAndBuildingY / scale);

        imageMode(CENTER);
        image(satelliteMap, NATIVE_WIDTH / 2, NATIVE_HEIGHT / 2, satelliteMap.width, satelliteMap.height);

        scale(2.2520015f);
        translate(-240, -272);

        if (hoveredBuilding != -1 && layer == InterfaceLayer.MAP_LAYER) {
          shape(buildingShapes.get(hoveredBuilding));
        }

        visualizeMapAndBuildings();

        visualizeSensorData(true);
        popMatrix();

        // Building selector signs
        for (SchoolBuilding b : selectedBuildings) {
          float buildingX =
                  (2.2520015f * (centroidsXY[b.bNum].x - 240) + (mapAndBuildingX / scale)) * scale;
          float buildingY =
                  (2.2520015f * (centroidsXY[b.bNum].y - 272) + (mapAndBuildingY / scale)) * scale;

          // Draw Main Panel
          shape(sign, buildingX - pointerW - mainPanelW, buildingY - pointerH);

          // Text
          int margin = 10;
          textAlign(LEFT, TOP);
          textFont(fHel_B12, 11);
          fill(255);
          text(buildingDB.getRow(b.bNum).getString("Name"),
                  buildingX - pointerW - mainPanelW + margin,
                  buildingY - pointerH + margin);
          textFont(fHel_L11, 11);
          text(getNewBuildingName(buildingDB.getRow(b.bNum).getString("Primary Use"), false),
                  buildingX - pointerW - mainPanelW + margin,
                  buildingY - pointerH + margin + 13);

          // bars
          textAlign(RIGHT, TOP);
          int firstBarY = 33;
          int secondBarY = 52;
          int barHeight = 10;
          int textPushInY = 39;
          int barWidth = 94;
          int valueXPos = barWidth + 44;

          text("kW",
                  buildingX - pointerW - mainPanelW + textPushInY,
                  buildingY - pointerH + margin + firstBarY);

          float sensorvalue = getBuildingEnergyAtCurrentTime(b.bNum);

          // TODO Switch it depending on unit
          // println(sensorvalue);
          BigDecimal bd = new BigDecimal(Float.toString(sensorvalue));
          bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
          textAlign(LEFT, TOP);
          text(Float.toString(bd.floatValue()),
                  buildingX - pointerW - mainPanelW + margin + valueXPos,
                  buildingY - pointerH + margin + firstBarY);
          noFill();
          strokeWeight(1);
          stroke(255);
          rect(buildingX - pointerW - mainPanelW + margin * 4 + 5,
                  buildingY - pointerH + margin + firstBarY,
                  barWidth, barHeight);
          fill(255);
          rect(buildingX - pointerW - mainPanelW + margin * 4 + 5,
                  buildingY - pointerH + margin + firstBarY,
                  (sensorvalue / maxSingleBuildingEnergy) * barWidth, barHeight);

          textAlign(RIGHT, TOP);
          text("Occ.",
                  buildingX - pointerW - mainPanelW + textPushInY,
                  buildingY - pointerH + margin + secondBarY);
          int occupancyValue = b.getOccupancyValueAtTime(timeRange.getCurUnix());
          textAlign(LEFT, TOP);
          text(occupancyValue,
                  buildingX - pointerW - mainPanelW + margin + valueXPos,
                  buildingY - pointerH + margin + secondBarY);
          noFill();
          rect(buildingX - pointerW - mainPanelW + margin * 4 + 5,
                  buildingY - pointerH + margin + secondBarY,
                  barWidth, barHeight);
          fill(255);
          rect(buildingX - pointerW - mainPanelW + margin * 4 + 5,
                  buildingY - pointerH + margin + secondBarY,
                  (occupancyValue / (float) maxSingleBuildingOccupancy) * barWidth, barHeight); // TODO Is it by the building max?
        }

        // END OF MAP SECTION
        // ================================================


        // ================================================
        // BOTTOM BARS:

        imageMode(CORNER);
        for (VisualBar vb : allBars) {
          vb.display();
        }

        if (isOnTimeCell) {

          if (this.howLongIsTimeRange == IntervalType.MONTHLY) { // Need to highlight week
            // 1. Determine cell that contains the mouse cursor
            float segmentWidth = (float) (DETECTED_WIDTH - xMargin * 2) / (float) timeRange.getNumberOfSegmentsOf(timelineInterval);
            mouseOnCellNum = (int) ((float) (mouseX - xMargin) / segmentWidth);
            long unixSelected = timeRange.getStartUnix() + mouseOnCellNum * 60 * 60 * 24;
            // 2. Floor and ceil of the week
            unixFlooredWeek = TimeRangeUtil.floorWeek(unixSelected);
            unixCeiledWeek = TimeRangeUtil.ceilWeek(unixSelected);
            // 4. Truncate and highlight
            if (unixFlooredWeek < timeRange.getStartUnix()) {
              long numDaysShowing = (unixCeiledWeek - timeRange.getStartUnix()) / (60 * 60 * 24);
              strokeWeight(1);
              fill(255, 100);
              rect(xMargin,
                      DETECTED_HEIGHT - 212 + yMargin,
                      segmentWidth * numDaysShowing, timebarHeight);
            } else if (unixCeiledWeek > timeRange.getEndUnix()) {
              long numDaysShowing = (timeRange.getEndUnix() - unixFlooredWeek) / (60 * 60 * 24);
              int startWeekCellNum = new DateTime(unixFlooredWeek * 1000L, DateTimeZone.forID("America/New_York")).getDayOfMonth() - 1;
              strokeWeight(1);
              fill(255, 100);
              rect(xMargin + startWeekCellNum * segmentWidth,
                      DETECTED_HEIGHT - 212 + yMargin,
                      segmentWidth * numDaysShowing, timebarHeight);
            } else {
              int startWeekCellNum = new DateTime(unixFlooredWeek * 1000L, DateTimeZone.forID("America/New_York")).getDayOfMonth() - 1;
              strokeWeight(1);
              fill(255, 100);
              rect(xMargin + startWeekCellNum * segmentWidth,
                      DETECTED_HEIGHT - 212 + yMargin,
                      segmentWidth * 7, timebarHeight);
            }
          } else {
            float segmentWidth = (float) (DETECTED_WIDTH - xMargin * 2) / (float) timeRange.getNumberOfSegmentsOf(timelineInterval);
            mouseOnCellNum = (int) ((float) (mouseX - xMargin) / segmentWidth);
            // Highlight cell
            strokeWeight(1);
            fill(255, 100);
            rect(xMargin + mouseOnCellNum * segmentWidth,
                    DETECTED_HEIGHT - 212 + yMargin,
                    segmentWidth, timebarHeight);
          }
        }

        if (isMovingThroughTime) {
          tickPin();
        }

        // If autoMode, if the current time is out of range, then jump to 2013, 1, 1, 0 in year view.
        // Also make sure it is playing.
        if (autoMode) {

          if (timeRange.getCurUnix() > 1418101200 || timeRange.getCurUnix() < 1356670800) {
            howLongIsTimeRange = IntervalType.YEARLY;
            timelineInterval = IntervalType.MONTHLY;
            timeRange.updateTime(TimeRangeUtil.getUnixInEastern(2013, 1, 1, 0), howLongIsTimeRange);
            indexDataWithNewRange();
          }
        } else {
          autoCameraTimer.tick(this, null);
        }

        timeScrubber.display();

        // UI
        drawButtons();

        // Current hour:minute
        noStroke();
        fill(0);
        textFont(fHel_B14, 14);
        int timeHeight = Math.round(textAscent() + textDescent() + 10);
        rect(displayWidth - xMargin - 48,
                displayHeight - 245 + yMargin,
                48,
                timeHeight);

        fill(255);
        textAlign(CENTER, CENTER);
        text(timeRange.getDigitalTime(TimeRangeUtil.CUR),
                (DETECTED_WIDTH - xMargin - 48) + 24,
                (DETECTED_HEIGHT - 245 + yMargin) + timeHeight / 2);

        calendar.render(this);

        // Inspector
        switch (inspectorState) {
          case 'C':
            inspectorButton.setX(xMargin);
//          ((ImageButtonOld) barSettings).setIcon(plusImage);
            break;
          case 'O':
            drawInspector();
            inspectorButton.setX(xMargin + 123 + 4);
//          ((ImageButtonOld) barSettings).setIcon(plusImage);
//          this.barSettings.draw();
//          this.barSettings.update();
            break;
          case 'E':
            drawInspector();
//          ((ImageButtonOld) barSettings).setIcon(checkImage);
//          this.barSettings.draw();
//          this.barSettings.update();
            break;
        }

        textFont(fHel_B14, 14);
        textAlign(LEFT, TOP);
        fill(255);
        text("Information in Action", 15, 15);
        float tWidth = textWidth("Information in Action");
        textFont(fHel_N14, 14);
        text(" | Northeastern Energy Flows", 15 + tWidth, 15);

        ((ImageButtonOld) playButton).setIconAltVisible(isMovingThroughTime);

        fadeInText(6.0 / timePinTimeInterval + "X");

        // Hover over tooltips
        if (layer == InterfaceLayer.HUD_LAYER) {
          drawToolTip();
        }

      } else {
        pushMatrix();
        scale(0.3f);
        imageMode(CENTER);
        tint(120, 120, 120);
        image(satelliteMap, DETECTED_WIDTH / 2 + 1400, DETECTED_HEIGHT / 2 + 700);
        popMatrix();
        fill(255, 100);
        textFont(fOswald_130, 130);
        textAlign(CENTER, CENTER);
        text("LOADING", DETECTED_WIDTH / 2, DETECTED_HEIGHT / 2);
        if (isDoingInitialSetup) {
          load(); // Don't load first draw, load on second draw one time.
        } else {
          isDoingInitialSetup = true;
          loop();
        }
      }
    } catch (Exception | Error e) {
      e.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  String toolTipValue = "";

  // Called by draw(); Displays a tooltip above the mouse showing the current tool tip hover value
  void drawToolTip() {
    rectMode(CORNER);
    fill(253);
    noStroke();
    textFont(fHel_B12, 12);
    int toolTipWidth = 45;
    int toolTipHeight = 18;
    if (textWidth(toolTipValue) > (toolTipWidth - 4)) {
      toolTipWidth = (int) (textWidth(toolTipValue) + 4);
    }
    rect(mouseX + 5, mouseY - toolTipHeight - 5,
            toolTipWidth, toolTipHeight, 1f);

    textAlign(CENTER, CENTER);
    fill(20);
    text(toolTipValue,
            mouseX + 5 + (toolTipWidth / 2),
            mouseY - toolTipHeight - 5 + (toolTipHeight / 2));
  }

  // Called by mouseMoved(); updates the value hovered by the mouse, if any.
  private String getHoveredValue() {
    if (layer != InterfaceLayer.HUD_LAYER) {
      throw new RuntimeException("You can't get a value if you are not on the HUD!");
    }

    // 1. Get relevant time
    float ratio = ((float) (mouseX - xMargin) / (DETECTED_WIDTH - xMargin * 2));
    long hoveredUnix = timeRange.getStartUnix() + Math.round(ratio * (timeRange.getEndUnix() - timeRange.getStartUnix()));

    for (VisualBar vb : allBars) {
      if (mouseY >= vb.y && mouseY <= (vb.y + vb.h)) {
        int targetBarSerial = vb.getSerial();
        switch (targetBarSerial) {
          case 1:
            try {
              return dailyTempDS.requestValueAtUnix(hoveredUnix, dailyTempDS.startRowIndex) + " F";
            } catch (IndexOutOfBoundsException e) {
              return "No Data";
            }
          case 2:
            try {
              return totalSensorLevelDS.requestValueAtUnix(hoveredUnix, totalSensorLevelDS.startRowIndex) + " kW";
            } catch (IndexOutOfBoundsException e) {
              return "No Data";
            } catch (RuntimeException r) {
              return "No Data";
            }

          case 3:
            Long dateKey = TimeRangeUtil.floorDate(hoveredUnix);
            if (!tweetHoursInDay.containsKey(dateKey)) {
              return "No Tweets Collected";
            }
            return this.tweetHoursInDay.get(dateKey).size() + " tweets";
          case 4:
            int occValue = 0;
            // Precondition: trust all items of indexedOcc are within timeRange,
            //			 AND are completely symetrical to the timeRange
            for (int i = 0; i < indexedCampusOcc.size(); ++i) {
              ValueContainer vc = indexedCampusOcc.get(i);
              if (vc.getTime() > hoveredUnix) {
                break;
              }
              // 2. Add totalDelta to the barHeight
              occValue += vc.getValue();
            }
            if (occValue < 0) {
              return 0 + " occupants";
            }
            return occValue + " occupants";
          case 5:
            // Search for date
            TableRow currentSchoolDay = null;
            String searchForDate = "," + TimeRangeUtil.slashesFormater.format(new Date(1000L * TimeRangeUtil.floorDate(hoveredUnix)));
            for (TableRow row : academicCalendar.rows()) {
              //println("Finding..." + searchForDate + " vs "+ row.getString("Date"));
              if (searchForDate.equals(row.getString("Date"))) {
                //println("Found date! = " + searchForDate);
                currentSchoolDay = row;
                break;
              }
            }
            if (currentSchoolDay == null) return "No Data";
            if (currentSchoolDay.getInt("Lecture") == 1 && currentSchoolDay.getInt("Exam") == 1) return "Exam / Lecture";
            if (currentSchoolDay.getInt("Exam") == 1) return "Exam";
            if (currentSchoolDay.getInt("Holiday") == 1) return "Holiday";
            if (currentSchoolDay.getInt("Lecture") == 1) return "Lecture";
            return "None";
          default:
            return "";
        }
      }
    }
    return "";
  }

  void drawButtons() {
    inspectorButton.draw();
    playButton.draw();
    stepBackButton.draw();
    stepForwardButton.draw();
    unitkWButton.draw();
    unitNormalizedButton.draw();
    monthButton.draw();
    yearButton.draw();
    calendarButton.render(this);
  }

  /**
   * Assumes nothing goes on on sundays
   */
  List<ValueContainer> indexedCampusOcc = new ArrayList<>();

  public void indexCampusOcc() {
    long start = System.nanoTime();
    // TODO improve efficiency
    // Reset indedocc
    indexedCampusOcc = new ArrayList<>();

    // 1. Given unix time (start & end)
    long startUnix = timeRange.getStartUnix();
    long endUnix = timeRange.getEndUnix();

    // Initialize
    String semester = "";
    Table relevantOccupancyTable = null;

    int dayInUnix = 60 * 60 * 24;
    for (long currentTime = startUnix; currentTime < endUnix; currentTime += dayInUnix) {
      // 1. Which semester, and whether it has been changed.
      String possibleNewSemester = unixToSemester(currentTime);
      if (possibleNewSemester.length() == 0) {
        semester = "";
        relevantOccupancyTable = null;
      } else if (semester.equals(possibleNewSemester)) {
        // DO NOTHING
      } else {
        semester = possibleNewSemester;
        relevantOccupancyTable = loadTable(semester + ".csv", "header");
      }

      if (relevantOccupancyTable == null) {
        continue;
      }

      if (semester.length() == 0) {
        // Do nothing; not within range.
      } else {
        // 3. Figure out which day
        int dayOfWeek = TimeRangeUtil.getDayOfWeek(currentTime);

        // 4. Find Starting Row number of Day in the semester
        Integer currentRowNumber = null;
        for (int i = 0; i < relevantOccupancyTable.getRowCount(); ++i) {
          //println(relevantOccupancyTable.getRow(i).getInt("day")+ " ?=? Day of week = " + dayOfWeek);
          if (relevantOccupancyTable.getRow(i).getInt("day") == dayOfWeek || dayOfWeek == 7) {
            currentRowNumber = i;
            break;
          }
        }

        if (currentRowNumber == null) {
          continue;
        }

        // 5. Add to list: all changes in this day, except for sundays
        while (currentRowNumber < relevantOccupancyTable.getRowCount() && relevantOccupancyTable.getRow(currentRowNumber).getInt("day") == dayOfWeek && dayOfWeek != 7) {
          indexedCampusOcc.add(new ValueContainer(
                  relevantOccupancyTable.getRow(currentRowNumber).getInt("totalDelta"),
                  currentTime + relevantOccupancyTable.getRow(currentRowNumber).getLong("time"),
                  relevantOccupancyTable.getRow(currentRowNumber).getInt("day"),
                  semester));
          ++currentRowNumber;
        }
      }
    }
//    println("Index Campus Occ Took", System.nanoTime() - start, "ns!");
  }

  Table semesterRanges;
  public String unixToSemester(long unix) {
    for (TableRow row : semesterRanges.rows()) {
      if ((row.getLong("startUnix") <= unix) && (unix <= row.getLong("endUnix"))) {
        return row.getString("semester");
      }
    }
    return "";
  }

  int maxOccValue = 0;

  void initializeMaxOccupancyLevel() {
    long start = System.nanoTime();
    int dayOfWeek = 1;
    Integer possibleMax = 0;
    List<Integer> possibleMaxes = new ArrayList<>();
    for (TableRow semester : semesterRanges.rows()) {
//      println(semester.getString("semester") + ".csv");
      Table curOccData = loadTable(semester.getString("semester") + ".csv", "header");
      for (TableRow entry : curOccData.rows()) {
        if (dayOfWeek != entry.getInt("day")) {
          // If the day is changed:
          possibleMaxes.add(possibleMax);
          dayOfWeek = entry.getInt("day");
          possibleMax = 0;
        } else {
          if (possibleMax < entry.getInt("totalDelta")) {
            possibleMax = entry.getInt("totalDelta");
          }
        }
      }
    }
    Collections.sort(possibleMaxes);
    maxOccValue = possibleMaxes.get(possibleMaxes.size() - 1);
//    println("initialize max occupancy level took", System.nanoTime() - start, "ns!");
  }

  PGraphics visualizeOccupancyData(float h) {

    float mainX = 0;
    float mainY = 0;
    float mainW = DETECTED_WIDTH - 2 * xMargin;

    PGraphics output = createGraphics((int) mainW, (int) h);
    output.beginDraw();

    // Background
    output.noStroke();
    output.fill(20, 230);
    output.rect(mainX, mainY, mainW, h);

    // Bars
    float barHeight = 0;
    float occValue = 0;
    // Precondition: trust all items of indexedOcc are within timeRange,
    //			 AND are completely symmetrical to the timeRange
    for (ValueContainer vc : indexedCampusOcc) {
      // 1. Determine where in the timeline to insert
      long lenUnix = timeRange.getEndUnix() - timeRange.getStartUnix();

      long drawUnix = vc.getTime();

      float drawX = mainX + ((drawUnix - timeRange.getStartUnix()) / (float) lenUnix) * mainW;

      // 2. Add totalDelta to the barHeight
      occValue += vc.getValue();
      //println("occValue = " + occValue);
      barHeight = (occValue / (float) maxOccValue) * (h - 7);

      // 3. Draw it
      output.fill(255);
      output.stroke(255);
      output.strokeWeight(0.5f);
      output.line(drawX, mainY + h - barHeight, drawX, DETECTED_WIDTH - barHeight);
    }
    output.endDraw();
    return output;
  }

  int timePinTimeInterval = 6; // 1X standard

  void tickPin() {
    // Every 5 ticks:
    if (loopCount == 0) {
      // Increase cur time
      if (timeRange.getCurIdx() == timeRange.getEndIdx()) {
        timeRange.jumpToNextSection();
        indexDataWithNewRange();
      }
      timeRange.increment();
    }

    if (timePinTimeInterval == 0) {
      if (timeRange.getCurIdx() == timeRange.getEndIdx()) {
        timeRange.jumpToNextSection();
        indexDataWithNewRange();
      }
      timeRange.increment();
    }

    loopCount = ((loopCount + 1) % timePinTimeInterval);
  }

  Table buildingTypeRenameTable;

  void drawInspector() {

    int mainX = xMargin;
    int keyHeight = 243;
    int labelHeight = 214;
    int increasedHeight = 0;
    if (inspectorState == 'E') {
      increasedHeight = 115;
    }
    int mainY = DETECTED_HEIGHT + yMargin - labelHeight - keyHeight - increasedHeight;
    int mainW = 123;

    // Background
    noStroke();
    fill(0, 200);
    rectMode(CORNER);
    rect(mainX, mainY, mainW, keyHeight + labelHeight + increasedHeight);

    // Key portion
    textFont(fHel_L11, 10f);
    noStroke();
    fill(255);
    textAlign(LEFT, TOP);
    int sizeOfKeyColor = 12;
    int spacing = 21;

    TableRow[] rowsWithOutDup = new TableRow[buildingTypeRenameTable.getRowCount() - 1];
    for (int i = 0; i < this.buildingTypeRenameTable.getRowCount(); ++i) {
      if (i >= 6) {
        rowsWithOutDup[i - 1] = buildingTypeRenameTable.getRow(i);
      } else {
        rowsWithOutDup[i] = buildingTypeRenameTable.getRow(i);
      }
    }
    int margin = 11;
    for (int i = 0; i < rowsWithOutDup.length; ++i) {
      TableRow row = rowsWithOutDup[i];

      noStroke();
      fill(getBuildingColor(row.getString("newName"), true));
      rect(mainX + margin,
              mainY + margin + spacing * i,
              sizeOfKeyColor,
              sizeOfKeyColor);

      fill(255);
      text(row.getString("newName"),
              mainX + margin + sizeOfKeyColor + 8,
              mainY + margin + spacing * i + 1);
    }

    // Label portion
    int index = 0;
    textFont(fHel_B12, 12);
    textAlign(LEFT, CENTER);
    strokeWeight(2);
    stroke(255);
    strokeCap(SQUARE);
    if (inspectorState == 'E') {
      fill(100);
    } else {
      fill(255);
    }
    for (VisualBar vb : allBars) {
      switch (index) {
        case 0:
          // Do nothing
          break;
        case 1:
          text("Temperature", xMargin + 20, vb.y + vb.h / 2);
          break;
        case 2:
          strokeWeight(2);
          stroke(255);
          line(xMargin, vb.y, xMargin + 123, vb.y);
          text("Electricity", xMargin + 20, vb.y + vb.h / 2);
          break;
        case 3:
          line(xMargin, vb.y, xMargin + 123, vb.y);
          text("Twitter", xMargin + 20, vb.y + vb.h / 2);
          break;
        case 4:
          line(xMargin, vb.y, xMargin + 123, vb.y);
          text("Occupancy", xMargin + 20, vb.y + vb.h / 2);
          break;
        case 5:
          line(xMargin, vb.y, xMargin + 123, vb.y);
          text("Exams", xMargin + 20, vb.y + 10);
          text("Lect./Holidays", xMargin + 20, vb.y + 25);
          break;
        default:
          throw new RuntimeException("Unsupported Index!! " + index);
      }
      ++index;
    }

    // Bar selector portion
    strokeWeight(2);
    stroke(255, 10);
    strokeCap(SQUARE);
    textAlign(LEFT, TOP);
    line(mainX,
            mainY + keyHeight,
            mainX + mainW,
            mainY + keyHeight);
    if (inspectorState == 'E') {
      fill(0, 170);
      noStroke();
      rect(0, 0, DETECTED_WIDTH, DETECTED_HEIGHT);

      int numberOfVisuals = 5;
      noStroke();
      fill(70, 200);
      rect(mainX,
              mainY + keyHeight,
              mainW,
              increasedHeight);

      strokeWeight(2);
      stroke(255);
      strokeCap(SQUARE);
      for (int i = 0; i < numberOfVisuals; ++i) {
        // Draw the box
        noFill();
        int yPosBox = mainY + keyHeight + margin + spacing * i;
        int crossMargins = 3;
        rect(mainX + margin,
                yPosBox,
                sizeOfKeyColor,
                sizeOfKeyColor);
        line(mainX + margin + crossMargins, yPosBox + crossMargins,
                mainX + margin + sizeOfKeyColor - crossMargins, yPosBox + sizeOfKeyColor - crossMargins);
        line(mainX + margin + sizeOfKeyColor - crossMargins, yPosBox + crossMargins,
                mainX + margin + crossMargins, yPosBox + sizeOfKeyColor - crossMargins);

        String barName;
        // Draw the name
        switch (i) {
          case 0:
            barName = "Temperature";
            break;
          case 1:
            barName = "Electricity";
            break;
          case 2:
            barName = "Twitter";
            break;
          case 3:
            barName = "Occupancy";
            break;
          case 4:
            barName = "Academia";
            break;
          default:
            throw new RuntimeException(i + " is NOT valid!");
        }
        fill(255);
        text(barName,
                mainX + margin + sizeOfKeyColor + 8,
                mainY + margin + keyHeight + spacing * i + 1);
      }
    }
  }

  private Button unitkWButton;
  private Button unitNormalizedButton;

  private char inspectorState = 'C';
  private Button inspectorButton;

//  Button barSettings;

  private boolean isMovingThroughTime = true;
  private Button playButton;

  private Button stepBackButton;
  private Button stepForwardButton;

  private int buttonHSpacing = 13;

  private PImage wideIcon;

  private Button yearButton;
  private Button monthButton;

  private PImage dayIcon;
  private Button dayButton;

  private Scrubber timeScrubber;

  public class Scrubber {

    private int mainX = xMargin;
    private int mainY = allBars.get(0).y + timebarHeight;
    private int mainW = DETECTED_WIDTH - 2 * xMargin;

    int headDiameter = 16;

    int bkHeight = 2;

    void display() {
      pushStyle();

      int num15Mins = timeRange.length();
      int num15MinsPast = timeRange.getCurIdx();
      float segmentWidth = (float) (DETECTED_WIDTH - xMargin * 2) / num15Mins;

      // Background
      noStroke();
      fill(40);
      rect(mainX, mainY, mainW, bkHeight);
      fill(255, 230);
      rect(mainX, mainY, Math.round(num15MinsPast * segmentWidth), bkHeight);

      // Tail part
      int tailX = mainX + Math.round(num15MinsPast * segmentWidth);

      strokeWeight(1);
      stroke(0, 20);
      line(tailX + -2, mainY - 17,
              tailX + -2, DETECTED_HEIGHT + yMargin - 1);

      stroke(0, 20);
      line(tailX + -1, mainY - 17,
              tailX + -1, DETECTED_HEIGHT + yMargin - 1);

      stroke(255);
      line(tailX, mainY - 17,
              tailX, DETECTED_HEIGHT + yMargin - 1);

      stroke(0, 20);
      line(tailX + 1, mainY - 17,
              tailX + 1, DETECTED_HEIGHT + yMargin - 1);

      stroke(0, 20);
      line(tailX + 2, mainY - 17,
              tailX + 2, DETECTED_HEIGHT + yMargin - 1);

      // Head part
      int headX = mainX + Math.round(num15MinsPast * segmentWidth);
      imageMode(CENTER);
      image(scrubberIcon,
              headX,
              mainY);
      popStyle();
    }

    boolean isMouseOver() {
      float segmentWidth = (float) (DETECTED_WIDTH - xMargin * 2) / timeRange.length();
      float xDistance = Math.abs(mouseX - (mainX + Math.round(timeRange.getCurIdx() * segmentWidth)));
      float yDistance = Math.abs(mouseY - mainY);
      float distance = (float) Math.sqrt(xDistance * xDistance + yDistance * yDistance);
      return distance < (headDiameter + 0.5f) / 2.0f;
    }

    float getSegWidth() {
      int num15Mins = timeRange.length();
      return (float) (DETECTED_WIDTH - xMargin * 2) / num15Mins;
    }

    void setNewPos(int newX) {

      // 1. Find which segment is best
      int segNo = Math.round((newX - mainX) / getSegWidth());
      // 2. Change timeline
      int currentNo = timeRange.getCurIdx();

//      println("input = " + newX);
//      println("Updating from: " + currentNo + " to " + segNo);
      int amount = Math.abs(segNo - currentNo);
      boolean isForward = true;
      if (segNo < currentNo) {
        isForward = false;
      }
      for (int i = 0; i < amount; ++i) {
        if (isForward) {
          if (timeRange.getCurIdx() == timeRange.getEndIdx()) {
            break;
          }
          timeRange.increment();
        } else {
          if (timeRange.getCurIdx() == 0) {
            break;
          }
          timeRange.decrement();

        }
      }
    }
  }


  // =============================
  // METHODS RELATED TO USER INPUT
  // =============================

  /**
   * Current functions:
   */
  public void mousePressed(MouseEvent e) {
    try {
      if (!isLoading) {

        autoMode = false;
        autoCameraTimer.restartFromBeginning();
        autoCameraTimer.startCountDown();

        if (calendarButton.isMouseOver(e.getX(), e.getY())) {
          calendarButton.mousePressedAction(this, e);
        }

        if (calendar.isMouseOver(e.getX(), e.getY())) {
          calendar.mousePressedAction(this, e);
        }

        updateInteractionState();

        if (isOnTimeCell) {
          long segmentUnix = 0;
          switch (timelineInterval) {
            case YEARLY:
              segmentUnix = new DateTime(timeRange.getStartUnix() * 1000L, DateTimeZone.forID("America/New_York")).plusYears(mouseOnCellNum).getMillis() / 1000L;
              howLongIsTimeRange = IntervalType.YEARLY;
              timelineInterval = IntervalType.MONTHLY;
              timeRange = new TimeRangeUtil(segmentUnix, howLongIsTimeRange);
              break;
            case MONTHLY:
              segmentUnix = new DateTime(timeRange.getStartUnix() * 1000L, DateTimeZone.forID("America/New_York")).plusMonths(mouseOnCellNum).getMillis() / 1000L;
              howLongIsTimeRange = IntervalType.MONTHLY;
              timelineInterval = IntervalType.WEEKLY;
              timeRange = new TimeRangeUtil(segmentUnix, howLongIsTimeRange);
              break;
            case WEEKLY:
              howLongIsTimeRange = IntervalType.WEEKLY;
              timelineInterval = IntervalType.DAILY;
              timeRange = new TimeRangeUtil(unixFlooredWeek, howLongIsTimeRange);
              break;
            case DAILY:
              segmentUnix = new DateTime(timeRange.getStartUnix() * 1000L, DateTimeZone.forID("America/New_York")).plusDays(mouseOnCellNum).getMillis() / 1000L;
              howLongIsTimeRange = IntervalType.DAILY;
              timelineInterval = IntervalType.HOURLY;
              timeRange = new TimeRangeUtil(segmentUnix, howLongIsTimeRange);
              break;
            default:
              throw new RuntimeException("Unsupported IntervalType: " + timelineInterval.name());
          }
//          System.out.println("after time cell click: " + segmentUnix);
          indexDataWithNewRange();
        } else if (layer == InterfaceLayer.HUD_LAYER) {
          float ratio = ((float) (mouseX - xMargin) / (DETECTED_WIDTH - xMargin * 2));
          if (ratio > 1) {
            ratio = 0.9999f;
          } else if (ratio < 0) {
            ratio = 0;
          }
          timeRange.scrubTo((int) Math.floor(timeRange.howManyDays() * 96 * ratio));
        }

        if (hoveredBuilding != -1) {
          boolean buildingExists = false;

          Iterator<SchoolBuilding> schoolBuildingIterator = selectedBuildings.iterator();
          while (schoolBuildingIterator.hasNext()) {
            SchoolBuilding b = schoolBuildingIterator.next();

            if (b.bNum == hoveredBuilding) {
              buildingExists = true;
              schoolBuildingIterator.remove();
              break;
            }
          }

          // Add to selected buildings
          if (!buildingExists) {
            selectedBuildings.add(new SchoolBuilding(hoveredBuilding, timeRange, this));
          }
        }

        updateInteractionLayer();

        inspectorButton.update();
        playButton.update();
        stepBackButton.update();
        stepForwardButton.update();
        unitkWButton.update();
        unitNormalizedButton.update();
        monthButton.update();
        yearButton.update();
      }
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  int hoveredBuilding = -1;

  boolean isOnTimeCell;
  int mouseOnCellNum = 0;
  long unixFlooredWeek;
  long unixCeiledWeek;

  public enum InterfaceLayer {
    MAP_LAYER, HUD_LAYER, UI_LAYER, SCRUBBER_LAYER
  }

  InterfaceLayer layer = InterfaceLayer.UI_LAYER;
  public void setLayer(InterfaceLayer newLayer) {
    layer = newLayer;
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    try {
      if (!isLoading) {
        updateInteractionState();

        if (calendar.isMouseOver(e.getX(), e.getY())) {
          calendar.mouseHoverAction(this, e);
        } else {
          calendar.removeHoveredStates();
        }

        if (calendarButton.isMouseOver(e.getX(), e.getY())) {
          calendarButton.mouseHoverAction(this, e);
        } else {
          calendarButton.removeHoveredStates();
        }
      }
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  private void updateInteractionLayer() {
    // Check mouse pos, determine layer
    // [Highest priority] Scrubber
    if (timeScrubber.isMouseOver()) {
      layer = InterfaceLayer.SCRUBBER_LAYER;
      isOnTimeCell = false;
    } else if ((mouseX >= xMargin)
            && (mouseX <= DETECTED_WIDTH - xMargin)
            && (mouseY >= DETECTED_HEIGHT - 212 + yMargin)
            && (mouseY <= DETECTED_HEIGHT - 212 + yMargin + timebarHeight)
            && howLongIsTimeRange != IntervalType.DAILY) { // if timeline is highlighted
      isOnTimeCell = true;
      layer = InterfaceLayer.UI_LAYER;
    } else if (calendar.isMouseOver(mouseX, mouseY)) {
      isOnTimeCell = false;
      layer = InterfaceLayer.UI_LAYER;
    } else if (mouseX > xMargin && mouseX < DETECTED_WIDTH - xMargin
            && mouseY > DETECTED_HEIGHT - 193 + yMargin && mouseY < DETECTED_HEIGHT + yMargin) { // [3nd priority] HUD
      isOnTimeCell = false;
      layer = InterfaceLayer.HUD_LAYER;
    } else { // [Lowest priority] Map, Buildings
      hoveredBuilding = mouseOnWhichBuilding();
      isOnTimeCell = false;
      layer = InterfaceLayer.MAP_LAYER;
    }
  }

  private void updateInteractionState() {
    updateInteractionLayer();

    inspectorButton.update();
    playButton.update();
    stepBackButton.update();
    stepForwardButton.update();
    unitkWButton.update();
    unitNormalizedButton.update();
    monthButton.update();
    yearButton.update();

    // Changes the states based on current layer
    switch (layer) {
      case SCRUBBER_LAYER:
//				cursor(HAND);
        isOnTimeCell = false;
        hoveredBuilding = -1;
        break;
      case UI_LAYER:
//				cursor(HAND);
        hoveredBuilding = -1;
        break;
      case HUD_LAYER:
//				cursor(ARROW);
        isOnTimeCell = false;
        hoveredBuilding = -1;
        toolTipValue = getHoveredValue();
        break;
      case MAP_LAYER:
        isOnTimeCell = false;
        if (hoveredBuilding != -1) {
//					cursor(HAND);
        } else {
//					cursor(ARROW);
        }
        break;
    }
  }

  private int mouseOnWhichBuilding() {
    int buildingNum = 0;
    for (PShape shape : buildingShapes) {
      if (isPointInPolygon(mouseX, mouseY, shape)) {
        return buildingNum;
      }
      ++buildingNum;
    }
    return -1;
  }

  // Ray-casting Algorithm
  private boolean isPointInPolygon(float x, float y, PShape s) {
    // Get a list of edges
    PShape[] shapeEdges = new PShape[s.getVertexCount() - 1];
    for (int i = 0; i < s.getVertexCount(); ++i) {

      if ((i + 1) >= s.getVertexCount()) {
        break;
      }

      if (s.getVertex(i).y < s.getVertex(i + 1).y) {
        shapeEdges[i] = createShape();
        shapeEdges[i].beginShape();
        shapeEdges[i].vertex(s.getVertex(i + 1).x, s.getVertex(i + 1).y);
        shapeEdges[i].vertex(s.getVertex(i).x, s.getVertex(i).y);
        shapeEdges[i].endShape();
      } else {
        shapeEdges[i] = createShape();
        shapeEdges[i].beginShape();
        shapeEdges[i].vertex(s.getVertex(i).x, s.getVertex(i).y);
        shapeEdges[i].vertex(s.getVertex(i + 1).x, s.getVertex(i + 1).y);
        shapeEdges[i].endShape();
      }
    }

    // For each edge: determine how many times a ray from mouse will
    // intersect the edges
    int count = 0;
    for (PShape e : shapeEdges) {

      // First, transform and scale them
      e.setVertex(0,
              (2.2520015f * (e.getVertex(0).x - 240) + (mapAndBuildingX / scale)) * scale,
              (2.2520015f * (e.getVertex(0).y - 272) + (mapAndBuildingY / scale)) * scale);
      e.setVertex(1,
              (2.2520015f * (e.getVertex(1).x - 240) + (mapAndBuildingX / scale)) * scale,
              (2.2520015f * (e.getVertex(1).y - 272) + (mapAndBuildingY / scale)) * scale);

      if (rayIntersectsEdge(x, y, e)) {
        ++count;
      }
    }
    return count % 2 == 1;
  }

  private boolean rayIntersectsEdge(float mouseX, float mouseY, PShape e) {

    float xP = mouseX;
    float yP = mouseY;

    float xA = e.getVertex(0).x;
    float yA = e.getVertex(0).y;
    float xB = e.getVertex(1).x;
    float yB = e.getVertex(1).y;

    if (mouseY == yA || mouseY == yB) {
      // Avoid vertex problem
      yP += 0.000001;
    }

    if (yP > yA || yP < yB) {
      return false;
    } else if (xP > xA && xP > xB) {
      return false;
    } else {
      if (xP < xA && xP < xB) {
        return true;
      } else {
        float slopeRed;
        float slopeBlue;
        if (xA != xB) {
          slopeRed = (yB - yA) / (xB - xA);
        } else {
          slopeRed = Float.POSITIVE_INFINITY;
        }

        if (xP != xB) {
          slopeBlue = (yP - yA) / (xP - xA);
        } else {
          slopeBlue = Float.POSITIVE_INFINITY;
        }
        return slopeBlue < slopeRed;
      }
    }
  }

  @Override
  public void keyPressed() {
    try {
      switch (key) {
        case KeyEvent.VK_SPACE:
          isMovingThroughTime = !isMovingThroughTime;
          break;
        case KeyEvent.VK_ESCAPE:
          key = 0; // Trap escape
          // DO NOTHING
          break;
      }

      switch (keyCode) {
        case LEFT:
          if (timeRange.getCurIdx() == 0) {
            timeRange.jumpToPrevSection();
            indexDataWithNewRange();
          } else {
            timeRange.scrubTo(0);
          }
//          printConfirm();
          break;
        case RIGHT:
          stepForwardButton.activate();
//          printConfirm();
          break;

        case UP:
          if ((timePinTimeInterval - 2) > 0)
            this.timePinTimeInterval -= 2;
          resetFadeInText();
          this.isFadeTextOnGoing = true;
          break;
        case DOWN:
          if ((timePinTimeInterval + 2) <= 12)
            this.timePinTimeInterval += 2;
          resetFadeInText();
          this.isFadeTextOnGoing = true;
          break;
      }
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  int tickText = 0;
  boolean isFadeTextOnGoing = false;
  float opacity = 255;
  float bkO = 190;

  private void resetFadeInText() {
    tickText = 0;
    isFadeTextOnGoing = false;
    opacity = 255;
    bkO = 190;
  }

  private void fadeInText(String text) {

    int duration = 40;

    if (!isFadeTextOnGoing) {
      // DO NOTHING
    } else if (tickText >= duration) {
      resetFadeInText();
    } else {
      noStroke();
      bkO -= 190 / duration;
      fill(color(0), bkO);
      rect(0, 0, DETECTED_WIDTH, DETECTED_HEIGHT);

      fill(color(255), opacity);
      textAlign(CENTER, CENTER);
      textFont(fTitle, 160);
      text(text, DETECTED_WIDTH / 2, DETECTED_HEIGHT / 2 - 100);
      opacity -= 255 / duration;
      ++tickText;
    }
  }

  /**
   * Drag around the map
   */
  boolean isDragging = false;
  boolean isDraggingTimeline = false;
  int mouseStartX = 0;
  int mouseStartY = 0;
  int mouseDistanceX = 0;
  int mouseDistanceY = 0;

  int oldDisplaceX = 0;
  int oldDisplaceY = 0;

  @Override
  public void mouseDragged() {
    try {
      if (layer == InterfaceLayer.SCRUBBER_LAYER) {
        // Pause it
        this.isMovingThroughTime = false;
        // Move it
        this.isDraggingTimeline = true;
        timeScrubber.setNewPos(mouseX);
      } else if (isDraggingTimeline) {
        timeScrubber.setNewPos(mouseX);
      } else if (layer == InterfaceLayer.MAP_LAYER) {

        if (isDragging) {

          mouseDistanceX = mouseX - mouseStartX;
          mouseDistanceY = mouseY - mouseStartY;

//				int posHorizontalLimit = (int) ((scale*satelliteMap.width / 2.0) - (WIDTH / 2.0));
//				int negHorizontalLimit = -posHorizontalLimit;
//				int posVerticalLimit = (int) ((scale*satelliteMap.height / 2.0) - (HEIGHT / 2.0));
//				int negVerticalLimit = -posVerticalLimit;

          int newDisplacementX = oldDisplaceX + mouseDistanceX;
          int newDisplacementY = oldDisplaceY + mouseDistanceY;

				/*
				if (newDisplacementX > posHorizontalLimit) {
					newDisplacementX = posHorizontalLimit;
				} else if (newDisplacementX < negHorizontalLimit) {
					newDisplacementX = negHorizontalLimit;
				}
				
				if (newDisplacementY > posVerticalLimit) {
					newDisplacementY = posVerticalLimit;
				} else if (newDisplacementY < negVerticalLimit) {
					newDisplacementY = negVerticalLimit;
				}
				*/
          mapAndBuildingX = newDisplacementX;
          mapAndBuildingY = newDisplacementY;
        } else {
          mouseStartX = mouseX;
          mouseStartY = mouseY;
          oldDisplaceX = mapAndBuildingX;
          oldDisplaceY = mapAndBuildingY;
          isDragging = true;
        }
      }
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  /**
   * disables dragging mode
   */
  @Override
  public void mouseReleased() {
    try {
      if (!isLoading) {
        if (isDragging) {
          isDragging = false;
        } else if (isDraggingTimeline) {
          isDraggingTimeline = false;
        }

        updateInteractionState();
      }
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  private float scale = 0.5f;

  @Override
  public void mouseWheel(MouseEvent event) {
    try {
      if (!isLoading) {
        float scrollAmount = event.getCount();

        scale -= scrollAmount / 500f;

        if (scale < 0.2f) {
          scale = 0.2f;
        } else if (scale > 1f) {
          scale = 1f;
        }
      }
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  @Override
  public void settings() {
    try {
      size(displayWidth, displayHeight, P2D);
    } catch (Exception | Error error) {
      error.printStackTrace();
      this.dispose();
      System.exit(1);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"--present", "main.Main"});
  }
}
