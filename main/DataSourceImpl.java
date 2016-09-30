package main;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import processing.data.Table;
import processing.data.TableRow;


public class DataSourceImpl extends DataSource<Integer> {
	
	
	public DataSourceImpl(Table sourcedata, long mainUnixStart,
			long mainUnixEnd, IntervalType segmentLength,
			IntervalType parentLength, String desiredColumn,
			String unixColumnName) {
		super(sourcedata, mainUnixStart, mainUnixEnd, segmentLength, parentLength,
				desiredColumn, unixColumnName);
		
		// Find Max and min values
		Integer curMax = dataTable.getRow(0).getInt(columnName);
		Integer curMin = dataTable.getRow(0).getInt(columnName);
		for (TableRow row : dataTable.rows()) {
			Integer i = row.getInt(columnName);
			Integer maybeMax = i;
			if (i == null) {
				if (curMax == null) {
					// Do nothing
				} else {
					// Do nothing
				}
			} else {
				if (curMax == null) {
					curMax = i;
				} else {
					if (maybeMax > curMax) {
						curMax = maybeMax;
					}
				}
			}
				
			Integer maybeMin = i;
			if (i == null) {
				if (curMin == null) {
					// Do nothing
				} else {
					// Do nothing
				}
			} else {
				if (curMin == null) {
					curMin = i;
				} else {
					if (maybeMin < curMin) {
						curMin = maybeMin;
					}
				}
			}
			
		}
		maxValue = curMax;
		minValue = curMin;
		// End
		
		updateStats(mainUnixStart, mainUnixEnd, segmentLength, parentLength);
	}

	@Override
	// Updates:
	// 	 If the source is hugging the new timerange, make efficient adjustments, and index
	//   else: index normally
	public void updateStats(long updatedStart, long updatedEnd, IntervalType newSegmentLength, IntervalType parentLength) {
		
//		println("Updating " + this.columnName + "... ...");
		
		long startTime = System.nanoTime();
		setSegmentLength(newSegmentLength);
		
		this.parentUnixStart = updatedStart;
		this.parentUnixEnd = updatedEnd;
		
		// Case 1: The min && max of the data source is "hugging" the new range.
		
		if (isStartUnixValid() && isEndUnixValid()) {
			isHugging = true;
//			println(columnName +  " is hugging");
			// 1. Update start and end indexes
			Integer startIndex = requestIndexWithUnix(updatedStart);
			Integer endIndex = requestIndexWithUnix(updatedEnd);
			
			// 2. Index
			int size = endIndex - startIndex;
//			println(""+size);
			indexedValues = new Integer[size];
			for (int i = startIndex; i < endIndex; ++i) {
				indexedValues[i - startIndex] = requestValueWithIndex(i);
			}
//			System.out.println(indexedValues);
			
			this.startRowIndex = startIndex;
			this.endRowIndex = endIndex;
		}
		// Case 2: Not hugging
		else {
			isHugging = false;
//			println(columnName +  " is NOT hugging");
			DateTime startDate = new DateTime(updatedStart * 1000L, DateTimeZone.forID("America/New_York"));
	    	int year = startDate.getYear();
	    	int month = startDate.getMonthOfYear();
	    	
			int numPositions = TimeRangeUtil.howManySegmentsBetween(segmentLength, parentLength, updatedStart);
//			println(""+numPositions);
			
			indexedValues = new Integer[numPositions];
			
			long unixInterval = TimeRangeUtil.intervalToUnix(segmentLength, year, month);
			int foundIndex = 0;
			for (int i = 0; i < numPositions; ++i) {
				try {
					foundIndex = requestIndexAtUnix(updatedStart + i * unixInterval, foundIndex);
					indexedValues[i] = dataTable.getRow(foundIndex).getInt(this.columnName);
				} catch (RuntimeException re) {
					// DO NOTHING, because there is no such value!
					if (columnName.equals("totalEnergy")) {
						//println(updatedStart + i * unixInterval + " NOT FOUND!!!");
					}
				}
			}
		}
		
		
		
//		println("=== DataSource (" + columnName + ") ====================="); print("  Indexing took " + (System.nanoTime() - startTime) + " ns!");
//		println("Main timerange: " + parentUnixStart + " - " + parentUnixEnd);
//		println("Source timerange (total)  : " + entireMinUnix + " - " + entireMaxUnix);
//		if (isHugging)
//			println("Source timerange (current): " + requestTime(startRowIndex) + " ["+ requestIndexWithUnix(updatedStart) +"] - " + requestTime(endRowIndex) + " ["+ requestIndexWithUnix(updatedEnd) + "]");
//		println("Segment Interval: " + segmentLength.name());
//		println("Entire Interval: " + parentLength);
//		println("Entire Min = " + this.minValue);
//		println("Entire Max = " + this.maxValue);
//		println("");
	}
	
	@Override
	public int requestIndexAtUnix(long requestedTime, int startIndex) {
		if (requestedTime < entireMinUnix || requestedTime > entireMaxUnix) {
			throw new IndexOutOfBoundsException("Requested time: " + requestedTime + " is OOB!");
		}
		// Search for such a time, comparingUnixValuesBasedOnCurrentInterval (this is assuming increasing time!)
		for (int i = startIndex; i < dataTable.getRowCount(); ++i) {
			TableRow row = dataTable.getRow(i);
			int result = compareUnixValuesBasedOnCurrentInterval(row.getLong(unixColumnName), requestedTime);
			if (result < 0) {
				// Keep going
			} else if (result == 0) {
				// Found
				return i;
			} else {
				// Not found
				throw new RuntimeException("Requested time: " + requestedTime + " was not found (Early exit)!");
			}
		}
		throw new RuntimeException("Requested time: " + requestedTime + " was not found (Finished entire loop)!");
	}
	
	@Override
	public Integer requestValueWithIndex(int index) {
		return dataTable.getRow(index).getInt(columnName);
	}
	
	@Override
	public Integer requestValueAtUnix(long requestedTime, int startIndex) {
		return requestValueWithIndex(requestIndexAtUnix(requestedTime, 0));
	}
	
	// BAD! DONT USE
	@Override
	public int requestIndexWithUnix(long requestedTime) {
		if (requestedTime < entireMinUnix || requestedTime > entireMaxUnix) {
			throw new IndexOutOfBoundsException("Requested time: " + requestedTime + " is OOB!");
		}
		// Search for such a time, comparingUnixValuesBasedOnCurrentInterval (this is assuming increasing time!)
		int tick = 0;
		for (TableRow row : dataTable.rows()) {
			long result = row.getLong(unixColumnName) - requestedTime;
			if (result < 0) {
				// Keep going
			} else if (result == 0) {
				// Found
				return tick;
			} else {
				// Not found
				throw new RuntimeException("Requested time: " + requestedTime + " was not found (Early exit)!");
			}
			++tick;
		}
		throw new RuntimeException("Requested time: " + requestedTime + " was not found (Finished entire loop)!");
	}

	@Override
	public Integer[] getIndexedArray() {
		return indexedValues;
	}

	
	

}
