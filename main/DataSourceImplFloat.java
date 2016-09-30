package main;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import processing.data.Table;
import processing.data.TableRow;


public class DataSourceImplFloat extends DataSource<Float> {
	public DataSourceImplFloat(Table sourcedata, long mainUnixStart,
			long mainUnixEnd, IntervalType segmentLength,
			IntervalType parentLength, String desiredColumn,
			String unixColumnName) {
		super(sourcedata, mainUnixStart, mainUnixEnd, segmentLength, parentLength,
				desiredColumn, unixColumnName);
		
		// Find Max and min values
		Float curMax = dataTable.getRow(0).getFloat(columnName);
		Float curMin = dataTable.getRow(0).getFloat(columnName);
		for (TableRow row : dataTable.rows()) {
			Float i = row.getFloat(columnName);
			Float maybeMax = i;
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
				
			Float maybeMin = i;
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
			indexedValues = new Float[size];
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
			
//			println("Source timerange (total)  : " + this.entireMinUnix + " - " + this.entireMaxUnix);
//			println(""+numPositions);
			
			indexedValues = new Float[numPositions];
			
			long unixInterval = TimeRangeUtil.intervalToUnix(segmentLength, year, month);
			int foundIndex = 0; // for efficiency
			boolean foundStartIndex = false;
			for (int i = 0; i < numPositions; ++i) {
				try {
					foundIndex = requestIndexAtUnix(updatedStart + i * unixInterval, foundIndex);
					indexedValues[i] = dataTable.getRow(foundIndex).getFloat(columnName);
					if (!foundStartIndex) {
						startRowIndex = foundIndex;
						foundStartIndex = true;
					}
				} catch (RuntimeException re) {
					// DO NOTHING, because there is no such value!
					if (columnName.equals("totalEnergy")) {
						//println(updatedStart + i * unixInterval + " NOT FOUND!!!");
					}
				}
			}
			endRowIndex = foundIndex;
		}
		
//
//		println("Indexing took: " + (System.nanoTime() - startTime) + " ns!");
//		println("=== DataSource (" + columnName + ") =====================");
//		println("Main timerange: " + parentUnixStart + " - " + parentUnixEnd);
//		println("Source timerange (total)  : " + entireMinUnix + " - " + entireMaxUnix);
//		if (isHugging)
//			println("Source timerange (current): " + requestTime(startRowIndex) + " ["+ requestIndexWithUnix(updatedStart) +"] - " + requestTime(endRowIndex) + " ["+ requestIndexWithUnix(updatedEnd) + "]");
//		println("Segment Interval: " + segmentLength.name());
//		println("Entire Interval: " + parentLength);
//		println("Min = " + this.minValue);
//		println("Max = " + this.maxValue);
//		println("");
	}
	
	@Override
	public int requestIndexAtUnix(long requestedTime, int startIndex) {
		if (requestedTime < entireMinUnix || requestedTime > entireMaxUnix) {
			throw new IndexOutOfBoundsException(requestedTime + " < " + entireMinUnix + " OR " + requestedTime + " > " + entireMaxUnix);
		}

		// Search for such a time, comparingUnixValuesBasedOnCurrentInterval (this is assuming increasing time!)
		for (int i = startIndex; i < dataTable.getRowCount(); ++i) {
			TableRow row = dataTable.getRow(i);
			long rowUnix = row.getLong(unixColumnName);
			int result = compareUnixValuesBasedOnCurrentInterval(row.getLong(unixColumnName), requestedTime);
			if (result < 0) {
				// Keep going, but add to i to jump ahead (assuming in regular intervals, and assuming increasing time)
				float fifteenMinGaps = (requestedTime - row.getLong(unixColumnName)) / 60.0f / 15.0f;
				i += fifteenMinGaps - 1;
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
	public Float requestValueAtUnix(long requestedTime, int startIndex) {
		return requestValueWithIndex(requestIndexAtUnix(requestedTime, startIndex));
	}
	
	@Override
	public Float requestValueWithIndex(int index) {
		return dataTable.getRow(index).getFloat(columnName);
	}
	
	// BAD! DON'T USE
	@Override
	public int requestIndexWithUnix(long requestedTime) {
		if (requestedTime < entireMinUnix || requestedTime > entireMaxUnix) {
			throw new RuntimeException("Requested time: " + requestedTime + " is OOB!");
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
	public Float[] getIndexedArray() {
		return indexedValues;
	}

	
}
