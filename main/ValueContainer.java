package main;

public class ValueContainer implements Comparable<ValueContainer> {
	
	private int value;
	private long time;
	private int day;
	private String semester;
	
	// day is 1-7; Mon-Sun
	ValueContainer(int value, long time, int day, String semester) {
		this.value = value;
		this.time = time;
		this.day = day;
		this.semester = semester;
	}
	
	int getValue() {
		return value;
	}
	
	long getTime() {
		return time;
	}
	
	int getDayOfWeek() {
		return day;
	}
	
	String getSemester() {
		return semester;
	}
	
	
	@Override
	public String toString() {
		return "Value = " + value + "; " +
				"Time = " + time + "; " +
				"Day = " + day;
	}

	@Override
	public int compareTo(ValueContainer o) {
		return this.value - o.value;
	}
	
}
