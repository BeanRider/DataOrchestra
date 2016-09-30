package main;
import java.util.ArrayList;
import java.util.List;

import processing.data.Table;
import processing.data.TableRow;

public class SchoolBuilding {

	int bNum;
	int currentOcc;
	Main parent;
	List<ValueContainer> releventOccupancyValues;
	long unixStart, unixEnd;
	
	public SchoolBuilding(int bNum, TimeRangeUtil newTimeRange, Main parent) {
		this.bNum = bNum;
		this.parent = parent;
		this.indexOccupancyValues(newTimeRange);
	}
	
	public SchoolBuilding(int bNum, long unixStart, long unixEnd, Main parent) {
		this.bNum = bNum;
		this.parent = parent;
		this.unixStart = unixStart;
		this.unixEnd = unixEnd;
		indexOccupancyValues(unixStart, unixEnd);
	}
	
	// EFFECT: changes the occupancy list, corresponding to the time range
	void indexOccupancyValues(TimeRangeUtil newTimeRange) {
		releventOccupancyValues = new ArrayList<ValueContainer>();
		
		// Initialize
		long curTime = newTimeRange.getStartUnix();
		long endUnix = newTimeRange.getEndUnix();
		String curSemester = "";
		Table relevantOccupancyTable = null;
		while (curTime < endUnix) {
			// 1. Which semester, and whether it has been changed.
			String possibleNewSemester = parent.unixToSemester(curTime);
			if (possibleNewSemester.length() == 0 || possibleNewSemester.equals("winter2013")) {
				// Cur semester is OOB
				curSemester = "";
				relevantOccupancyTable = null;
			} else if (curSemester.equals(possibleNewSemester)) {
				// DO NOTHING, keep the current semester;
			} else {
				// Change the semester, and then the table
				curSemester = possibleNewSemester;
				relevantOccupancyTable = parent.loadTable(curSemester+"-occ.csv", "header");
				// System.out.println(curSemester+"-occ.csv");
			}
			
			if (curSemester.length() == 0) {
				// Skip Everything; empty string
			} else {
				// 2. Which day?
				char dayOfWeek = TimeRangeUtil.getDayOfWeekLetter(curTime);
				// 3. Find Starting Row number of Day in the semester
				Integer currentRowNumber = null;
				for (int i = 0; i < relevantOccupancyTable.getRowCount(); ++i) {
					if (relevantOccupancyTable.getRow(i).getString("day").charAt(0) == dayOfWeek || dayOfWeek == 7) {
						currentRowNumber = i;
						break;
					}
				}
								
				// 5. Add to the arraylist history of energy levels in this day, for this building, except for sundays
				int currentOccupancy = 0; // All buildings start at 0 on each day
				TableRow currentRow = relevantOccupancyTable.getRow(currentRowNumber);
				while (currentRowNumber < relevantOccupancyTable.getRowCount()
						&& currentRow.getString("day").charAt(0) == dayOfWeek
						&& dayOfWeek != 7) {
					
					int currentBID;
					try {
						currentBID = getbNum_FromShortName(currentRow.getString("building"));
						
						if (currentBID == bNum) {
							
							currentOccupancy += currentRow.getInt("delta");
							releventOccupancyValues.add(new ValueContainer(
									currentOccupancy,
									curTime + currentRow.getLong("time"),
									currentRow.getInt("day"),
									curSemester));
						}
					} catch (RuntimeException re) {
						// DO NOTHING
						System.out.println("Bad Building Name, not found and not Added");
					}
					
					++currentRowNumber;
					currentRow = relevantOccupancyTable.getRow(currentRowNumber);
				}
			}
			curTime += 60 * 60 * 24;
			
		}
			
	}
	
	// EFFECT: changes the occupancy list, coresponding to the timerange
	void indexOccupancyValues(long s, long e) {
		releventOccupancyValues = new ArrayList<ValueContainer>();
		
		// Initialize
		long curTime = s;
		long endUnix = e;
		String curSemester = "";
		Table relevantOccupancyTable = null;
		while (curTime < endUnix) {
			// 1. Which semester, and whether it has been changed.
			String possibleNewSemester = parent.unixToSemester(curTime);
			if (possibleNewSemester.length() == 0 || possibleNewSemester.equals("winter2013")) {
				// Cur semester is OOB
				curSemester = "";
				relevantOccupancyTable = null;
			} else if (curSemester.equals(possibleNewSemester)) {
				// DO NOTHING, keep the current semester;
			} else {
				// Change the semester, and then the table
				curSemester = possibleNewSemester;
				relevantOccupancyTable = parent.loadTable(curSemester+"-occ.csv", "header");
				// System.out.println(curSemester+"-occ.csv");
			}
			
			if (curSemester.length() == 0) {
				// Skip Everything; empty string
			} else {
				// 2. Which day?
				char dayOfWeek = TimeRangeUtil.getDayOfWeekLetter(curTime);
				// 3. Find Starting Row number of Day in the semester
				Integer currentRowNumber = null;
				for (int i = 0; i < relevantOccupancyTable.getRowCount(); ++i) {
					if (relevantOccupancyTable.getRow(i).getString("day").charAt(0) == dayOfWeek || dayOfWeek == 7) {
						currentRowNumber = i;
						break;
					}
				}
								
				// 5. Add to the arraylist history of energy levels in this day, for this building, except for sundays
				int currentOccupancy = 0; // All buildings start at 0 on each day
				TableRow currentRow = relevantOccupancyTable.getRow(currentRowNumber);
				while (currentRowNumber < relevantOccupancyTable.getRowCount()
						&& currentRow.getString("day").charAt(0) == dayOfWeek
						&& dayOfWeek != 7) {
					
					int currentBID;
					try {
						currentBID = getbNum_FromShortName(currentRow.getString("building"));
						if (currentBID == bNum) {
							
							currentOccupancy += currentRow.getInt("delta");
							// System.out.println(currentOccupancy);
							releventOccupancyValues.add(new ValueContainer(
									currentOccupancy,
									curTime + currentRow.getLong("time"),
									currentRow.getInt("day"),
									curSemester));
						}
					} catch (RuntimeException re) {
						// DO NOTHING
						System.out.println("Bad Building Name, not found and not Added");
					}
					
					++currentRowNumber;
					currentRow = relevantOccupancyTable.getRow(currentRowNumber);
				}
			}
			curTime += 60 * 60 * 24;
			
		}
	}
	
	public int getbNum_FromShortName(String abbrev) {
		// 1. Try to find abbrev
		for (TableRow row : parent.abbrevToName.rows()) {
			if (row.getString("abbrev").equals(abbrev)) {
				// If found, return the building name
				return row.getInt("bID");
			} else if ((abbrev.equals("LA") || abbrev.equals("NI"))
					&& row.getString("abbrev").equals("LA/NI")) {
				return row.getInt("bID");
			}
		}
		// 2. If not found, throw an error;
		throw new RuntimeException(abbrev + " was not found!");
	}

	// Gets the occupancy value of this building at the given unix time
	int getOccupancyValueAtTime(long curUnix) {
		int lastValue = 0;
		for (ValueContainer vc : releventOccupancyValues) {
			// System.out.println(vc.getValue());
			if (curUnix < vc.getTime()) {
				return lastValue;
			} else if (curUnix == vc.getTime()) {
				return vc.getValue();
			} else if (curUnix > vc.getTime()) {
				// Save lastValue, and Keep looking (increasing vc)
				lastValue = vc.getValue();
			}
		}
		return lastValue; // Which is zero because if everything was gone through without curUnix ever <= vc, it will be zero
	}	
}
