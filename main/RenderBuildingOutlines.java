package main;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.Table;
import processing.data.TableRow;

public class RenderBuildingOutlines extends PApplet {

	TableRow originRow;
	Table buildingDB;
	Point2D.Double originLatLong;
	int NATIVE_WIDTH = 1280;
	int NATIVE_HEIGHT = 800;
	List<PShape> buildingShapes = new ArrayList<>();
	
	public void setup() {
		load();
	}
	
	public void load() {
		buildingDB = loadTable("buildingDB.csv", "header");
		buildingTypeRenameTable = loadTable("buildingTypeRename.csv", "header");
		int numBuildings = buildingDB.getRowCount();
		// Origin
        originRow = buildingDB.getRow(12);
        originLatLong = StringParseUtil.parseCentroidString(originRow.getString("Centroid"));
        
        // ArrayList of PShape Building Outlines
        for (int i = 0; i < numBuildings; ++i) {
        	TableRow row = buildingDB.getRow(i);
        	this.buildingShapes.add(buildShape(row, this.getBuildingColor(row.getString("Primary Use"), false)));
        }
	}
	
	Table buildingTypeRenameTable;

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
        outline.stroke(255, 255, 255);
        
        originLatLong = StringParseUtil.parseCentroidString(row.getString("Centroid"));
        
        for (String s: verticesAsStringSets) {
            PVector v = GeoUtil.convertGeoToScreen(
            		StringParseUtil.parseCentroidString(s),
            		this.originLatLong,
            		NATIVE_WIDTH,
            		NATIVE_HEIGHT); // Converts to a PVector using 1280 X 800
            println(v);
            outline.vertex(v.x, v.y); // adds adjustment to keep it centered
        }
        outline.endShape(CLOSE);
        return outline;
	}
	
	PGraphics pgDrawing;
	int i = 0;
	public void draw() {
				
		println(i);
		if (i >= buildingShapes.size()) {
			exit();
			return;
		}
		
//		PGraphicsPDF pdf = (PGraphicsPDF) g;  // Get the renderer

		PShape b = buildingShapes.get(i);

		println((int) Math.ceil(b.getWidth()));
		println((int) Math.ceil(b.getHeight()));
		
		pgDrawing = createGraphics(
				(int) Math.ceil(b.getWidth()) + 100,
				(int) Math.ceil(b.getHeight()) + 100,
				SVG, buildingDB.getRow(i).getString("Name") + ".svg");
		
		pgDrawing.beginDraw();
		
		pgDrawing.beginShape();
		for (int i = 0; i < b.getVertexCount(); ++i) {
			pgDrawing.pushMatrix();
			pgDrawing.fill(b.getFill(i));
			pgDrawing.stroke(b.getStroke(i));
			pgDrawing.strokeWeight(b.getStrokeWeight(i));
			pgDrawing.vertex(b.getVertex(i).x - 600 + b.getWidth() / 2, b.getVertex(i).y - 360 + b.getHeight() / 2);
			pgDrawing.popMatrix();
		}
		pgDrawing.endShape();
		
		pgDrawing.endDraw();
		
		i++;
		
//		pdf.shape(b);
//		pdf.nextPage();  // Tell it to go to the next page
	}
	
	public void settings() {
		  size(1280, 800, P2D);
	}
	
	public static void main(String args[]) {
	    PApplet.main(new String[] { "--present", "main.RenderBuildingOutlines" });
	}
}
