package main;

import processing.data.Table;
import processing.data.TableRow;

public abstract class DataSource<T> {
	
	protected Table dataTable;
	
	protected long parentUnixStart, parentUnixEnd;
	protected long entireMinUnix, entireMaxUnix;
	
	protected boolean isIncreasing, isHugging;
	
	protected T maxValue, minValue;
	
	protected Integer startRowIndex = null;
	protected Integer endRowIndex = null;
	
	protected IntervalType segmentLength;
	
	protected String columnName;
	protected String unixColumnName;

	protected T[] indexedValues; // use this for unhugged ranges only!
	
	/**
	 * This is for wholesome data with entire ranges covered ONLY, with no missing data!!
	 * Will break if bad data is given.
	 * @param sourcedata
	 * @param mainUnixStart
	 * @param mainUnixEnd
	 */
	public DataSource(Table sourcedata, long mainUnixStart, long mainUnixEnd, IntervalType segmentLength, IntervalType parentLength, String desiredColumn, String unixColumnName) {
		
		this.segmentLength = segmentLength;
		this.columnName = desiredColumn;
		this.unixColumnName = unixColumnName;
		
		this.dataTable = sourcedata;
		
		this.parentUnixStart = mainUnixStart;
		this.parentUnixEnd = mainUnixEnd;
		
		this.entireMinUnix = findUnixStart();
		this.entireMaxUnix = findUnixEnd();
		
		this.isIncreasing = this.isChronoOrder();
	}
	
	// Finds the lowest unix time of the source (which is sorted in order; lowest of first or last row assumed to be the start)
	long findUnixStart() {
		TableRow firstRow = dataTable.getRow(0);
		TableRow lastRow = dataTable.getRow(dataTable.getRowCount() - 1);
		
		if (firstRow.getLong(unixColumnName) <= lastRow.getLong(unixColumnName)) {
			return firstRow.getLong(unixColumnName);
		} else {
			return lastRow.getLong(unixColumnName);
		}
	}
	
	// Finds the highest unix time of the source (which is sorted in order; highest of first or last row assumed to be the end);
	long findUnixEnd() {
		TableRow firstRow = dataTable.getRow(0);
		TableRow lastRow = dataTable.getRow(dataTable.getRowCount() - 1);
		
		if (firstRow.getLong(unixColumnName) <= lastRow.getLong(unixColumnName)) {
			return lastRow.getLong(unixColumnName);
		} else {
			return firstRow.getLong(unixColumnName);
		}
	}
	
	// Determines whether the data is in time order; from 0 to last row increasing (assumes data is sorted)
	boolean isChronoOrder() {
		TableRow firstRow = dataTable.getRow(0);
		TableRow lastRow = dataTable.getRow(dataTable.getRowCount() - 1);
		
		return firstRow.getLong(unixColumnName) <= lastRow.getLong(unixColumnName);
	}
		
	// Determines whether the start unix of this data <= the start unix of the parent
	boolean isStartUnixValid() {
		return this.compareUnixValuesBasedOnCurrentInterval(this.entireMinUnix, this.parentUnixStart) <= 0;
	}
	
	// Determines whether the end unix of this data >= the end unix of the parent
	boolean isEndUnixValid() {
		return this.compareUnixValuesBasedOnCurrentInterval(this.entireMaxUnix, this.parentUnixEnd) >= 0;
	}
	
	// Updates:
	// 	 If the source is hugging the new timerange, make efficient adjustments, and index
	//   else: index normally
	public abstract void updateStats(long updatedStart, long updatedEnd, IntervalType newSegmentLength, IntervalType parentLength);
	
	public abstract T[] getIndexedArray();
	
	// given index is in terms of the data table.
	long requestTime(int index) {
		return dataTable.getRow(index).getLong(this.unixColumnName);
	}
	
	// if the given time is OOB of this source, then throw RuntimeExcepton (this is assuming increasing time!)
	public abstract int requestIndexAtUnix(long requestedTime, int startIndex);
	
	
	public abstract int requestIndexWithUnix(long requestedTime);
	
	/**
	 * Returns the value at the requested UNIX time, looking after the provided start index
	 * 
	 * @param requestedTime
	 * @param startIndex
	 * @return
	 */
	public abstract T requestValueAtUnix(long requestedTime, int startIndex);
	
	// this is directly requestingValue
	public abstract T requestValueWithIndex(int index);
	
	int compareUnixValuesBasedOnCurrentInterval(long unix1, long unix2) {
		switch (this.segmentLength) {
			case QUARTER:
				return TimeRangeUtil.compareQuarterly(unix1, unix2);
			case HOURLY:
				return TimeRangeUtil.compareHourly(unix1, unix2);
			case DAILY:
				return TimeRangeUtil.compareDaily(unix1, unix2);
			case WEEKLY:
				return TimeRangeUtil.compareWeekly(unix1, unix2);
			case MONTHLY:
				return TimeRangeUtil.compareMonthly(unix1, unix2);
			case YEARLY:
				return TimeRangeUtil.compareYearly(unix1, unix2);
			default:
				throw new RuntimeException("Bad interval enum given!!");
		}
	}
	
	void setSegmentLength(IntervalType newSegmentLength) {
		this.segmentLength = newSegmentLength;
	}
	
	void println(String s) {
		System.out.println(s);
	}
	
	void print(String s) {
		System.out.print(s);
	}
}
