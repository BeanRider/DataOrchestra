package main;
import java.text.SimpleDateFormat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimeRangeUtil {
	
	public static final int START = -1;
	public static final int CUR = 0;
	public static final int END = 1;
	
	private int posCurSegment, posEndSegment;
    private long unixStart, unixCurrent, unixEnd; // NOTE: unixEnd does not represent the last time value to calculate, but rather only the ending time.
    
//    private IntervalType subSegmentLength; // Unused!
    private IntervalType howLongAmI;
    
    /**
     * Constructs by: Setting a starting time, length, and interval
     * @param startUnix   - initial begin time
     * @param howLongAmI  - entire segment length
     */
    public TimeRangeUtil(long startUnix, IntervalType howLongAmI) {    	
    	DateTimeZone.setDefault(DateTimeZone.forID("America/New_York"));
    	this.howLongAmI = howLongAmI;
    	this.unixStart = startUnix;
    	this.unixCurrent = startUnix;
    	switch (howLongAmI) {
	    	case YEARLY:
	    		this.unixEnd = new DateTime(startUnix * 1000L, DateTimeZone.forID("America/New_York")).plusYears(1).getMillis() / 1000L;
				break;
			case MONTHLY:
				this.unixEnd = new DateTime(startUnix * 1000L, DateTimeZone.forID("America/New_York")).plusMonths(1).getMillis() / 1000L;
				break;
			case WEEKLY:
				this.unixEnd = new DateTime(startUnix * 1000L, DateTimeZone.forID("America/New_York")).plusDays(7).getMillis() / 1000L;
				break;
			case DAILY:
				this.unixEnd = new DateTime(startUnix * 1000L, DateTimeZone.forID("America/New_York")).plusDays(1).getMillis() / 1000L;
				break;
			case HOURLY:
				this.unixEnd = new DateTime(startUnix * 1000L, DateTimeZone.forID("America/New_York")).plusHours(1).getMillis() / 1000L;
				break;
			default:
				throw new RuntimeException("Unsupported IntervalType: " + howLongAmI.name());
    	}
//    	System.out.println(unixStart);
//    	System.out.println(unixEnd);
    	
    	posCurSegment = 0; // Start at 0th position
    	DateTime d = getDateTimeFor(CUR);
    	this.posEndSegment = howMany15Mins(howLongAmI, d.getYear(), d.getMonthOfYear()) - 1; // if the total length is 4, the end index is 4 - 1 = 3.
    }
    
    public int howManyDays() {
    	return this.length() / 4 / 24;
    }
    
    public static int daysInYear(int year) {
    	DateTime dateTime = new DateTime(year, 1, 14, 12, 0, 0, 000, DateTimeZone.forID("America/New_York"));
    	return dateTime.dayOfYear().getMaximumValue();
    }
    
    public static int daysInMonth(int year, int month) {
    	DateTime dateTime = new DateTime(year, month, 14, 12, 0, 0, 000, DateTimeZone.forID("America/New_York"));
//    	System.out.println("How many days in month " + month + " : " + dateTime.dayOfMonth().getMaximumValue());
    	return dateTime.dayOfMonth().getMaximumValue();
    }
    
    public static long getUnixInEastern(int year, int month, int day, int hour) {
    	DateTime dateTime = new DateTime(year, month, day, hour, 0, 0, DateTimeZone.forID("America/New_York"));
    	return dateTime.getMillis() / 1000L;
    }
    
    // returns the number of segments of the given type this time range currently contains
    public int getNumberOfSegmentsOf(IntervalType segment) {
    	int numSegments;
    	switch (segment) {
			case YEARLY:
				numSegments = length() / 4 / 24 / daysInYear(getDateTimeFor(CUR).getYear());
				break;
			case MONTHLY:
				if (howLongAmI == IntervalType.YEARLY) {
					numSegments = 12;
				} else {
					throw new RuntimeException("You can only choose monthly when the timerange is yearly!");
				}
				//System.out.println("Days in month (" + month + ") : " + daysInMonth(year, month));
				break;
			case WEEKLY:
				numSegments = length() / 4 / 24;
				break;
			case DAILY:
				numSegments = length() / 4 / 24;
				break;
			case HOURLY:
				numSegments = length() / 4;
				break;
			default:
				throw new RuntimeException("Unsupported IntervalType: " + segment.name());
    	}
    	if (numSegments == 0) {
    		throw new RuntimeException("Provided IntervalType is too large!");
    	}
    	//System.out.println("How many segments: " + numSegments);
    	return numSegments;
    }
    
       
    public static long intervalToUnix(IntervalType interval, int year, int month) {
    	long secsPerDay = 24 * 60 * 60;
    	switch (interval) {
			case YEARLY:
				return daysInYear(year) * secsPerDay;
			case MONTHLY:
				return daysInMonth(year, month) * secsPerDay;
			case DAILY:
				return secsPerDay;
			case HOURLY:
				return 60 * 60;
			case QUARTER:
				return 15 * 60;
			default:
				throw new RuntimeException("Unsupported IntervalType: " + interval.name());
    	}
    }
    
    public static int howManySegmentsBetween(IntervalType segmentLength, IntervalType parentLength, long start) {
    	DateTime startDate = new DateTime(start * 1000L, DateTimeZone.forID("America/New_York"));
    	int year = startDate.getYear();
    	int month = startDate.getMonthOfYear();
    	//System.out.println("Getting year = " + year);
    	//System.out.println("Getting month = " + month);
    	//System.out.println("Getting type = " + segmentLength.name());
    	
		int num15Minutes = howMany15Mins(parentLength, year, month);
		int numSegments = 0;
		switch (segmentLength) {
		case YEARLY:
			numSegments = num15Minutes / 4 / 24 / daysInYear(year);
			break;
		case MONTHLY:
			numSegments = num15Minutes / 4 / 24 / daysInMonth(year, month);
//			System.out.println("Days in month (" + month + ") : " + daysInMonth(year, month));
			break;
		case DAILY:
			numSegments = num15Minutes / 4 / 24;
			break;
		case HOURLY:
			numSegments = num15Minutes / 4;
			break;
		case QUARTER:
			numSegments = num15Minutes;
			break;
		default:
			throw new RuntimeException("Unsupported IntervalType: " + segmentLength.name());
		}
		if (numSegments == 0) {
			throw new RuntimeException("Provided IntervalType is too large!");
		}
		//System.out.println("How many segments: " + numSegments);
		return numSegments;
    }
    
    public static int howMany15Mins(IntervalType type, int year, int month) {
		// Calculate how much to jump ahead
		int numOfFifteenMins = 0;
		switch (type) {
			case YEARLY:
				numOfFifteenMins = 4 * 24 * daysInYear(year);
				break;
			case MONTHLY:
				numOfFifteenMins = 4 * 24 * daysInMonth(year, month);
				break;
			case WEEKLY:
				numOfFifteenMins = 4 * 24 * 7;
				break;
			case DAILY:
				numOfFifteenMins = 4 * 24;
				break;
			case HOURLY:
				numOfFifteenMins = 4;
				break;
			default:
				throw new RuntimeException("Unsupported IntervalType");
		}
//		System.out.println();
//		System.out.println("Before returning numOfFifteenMins: #15Mins = " + numOfFifteenMins + "; #days = " + numOfFifteenMins / 4 / 24);
//		System.out.println();
		return numOfFifteenMins;
    }
    
    // Updates the timeline in terms of starting index and timeline length
    public void updateTime(long newUnixStartTime, IntervalType newLength) {
    	this.howLongAmI = newLength;
    	
    	this.unixStart = newUnixStartTime;
        
        this.posCurSegment = 0;
        this.unixCurrent = newUnixStartTime;
        
        DateTime d = getDateTimeFor(CUR);
        this.posEndSegment = howMany15Mins(howLongAmI, d.getYear(), d.getMonthOfYear()) - 1;
        this.unixEnd = getNextUnix();
    }
    
    // Returns how many 15 mins segments there are. (1 hour -> 4)
    public int length() {
    	return this.getEndIdx() + 1;
    }
    
    // Updates the current index of the timeline
    public void scrubTo(int index) {
    	if (checkIndexOOB(index)) {
    		throw new RuntimeException(index + " isn't something you can scrub to!");
    	}
    	this.posCurSegment = index;
    	this.unixCurrent = unixStart + index * 15 * 60;
    }
    
    // Move forward in time by one increment (depending on the IntervalType subSegmentLength);
    // if incremented on the ending index of this current timeline, jump to next section.
    void increment() {
    	if (checkIndexOOB(posCurSegment + 1)) {
    		// This is normal:
    		this.jumpToNextSection();
    	} else {
    		this.posCurSegment += 1;
            this.unixCurrent += 15 * 60; // increment by 15 mins
    	}
    }
    
    void decrement() {
    	if (checkIndexOOB(posCurSegment - 1)) {
    		this.jumpToPrevSection();
    	}
    	this.posCurSegment -= 1;
        this.unixCurrent -= 15 * 60; // decrement by 15 mins
    }
    
    long getStartUnix() {
    	return unixStart;
    }
    
    public long getCurUnix() {
    	return unixCurrent;
    }
    
    long getEndUnix() {
    	return unixEnd;
    }
   
    long getLastUnixSegment() {
    	return getStartUnix() + posEndSegment * 15 * 60;
    }
    
    int getCurIdx() {
    	return posCurSegment;
    }
    
    int getEndIdx() {
    	return posEndSegment;
    }
    
    public static int getDayOfWeek(long unix) {
    	return new DateTime(unix * 1000L).getDayOfWeek();
	}
    
    public static char getDayOfWeekLetter(long unix) {
    	return getDayAsOneLetterStringFromNum(getDayOfWeek(unix));
    }
    
    public static char getDayAsOneLetterStringFromNum(int i) {
    	DateTime date = new DateTime(DateTimeZone.forID("America/New_York"));
		date = date.withDayOfWeek(i);
		// System.out.println(date.dayOfWeek().getAsText());
		return date.dayOfWeek().getAsText().charAt(0);
    }
    
    /**
     * Returns the HH:mm digital time of the given option
     * @param option - one of: START, CUR, END
     * @return the HH:mm digital time string of the given option
     */
    String getDigitalTime(int option) {
    	return String.format("%02d", getDateTimeFor(option).getHourOfDay()) + ":" +
    			String.format("%02d", getDateTimeFor(option).getMinuteOfHour());
    }
    
    /**
     * Compares two Unix timestamps if they are on the same hour.
     * 
     * @param 	unix1 - first Unix timestamp
     * @param 	unix2 - second Unix timestamp
     * @return  
     */
    static int compareHourly(long unix1, long unix2) {
    	DateTime d1 = new DateTime(unix1 * 1000L);
    	DateTime d2 = new DateTime(unix2 * 1000L);
    	
    	int year1 = d1.getYear();
    	int year2 = d2.getYear();
//    	System.out.println(year1 + " vs. " + year2);
    	
    	int mo1 = d1.getMonthOfYear();
    	int mo2 = d2.getMonthOfYear();
//    	System.out.println(mo1 + " vs. " + mo2);
    	
    	int day1 = d1.getDayOfMonth();
    	int day2 = d2.getDayOfMonth();
//    	System.out.println(day1 + " vs. " + day2);
    	
    	int hour1 = d1.getHourOfDay();
    	int hour2 = d2.getHourOfDay();
    	
    	if (year1 < year2) {
    		return -1;
    	} else if (year1 > year2) {
    		return 1;
    	} else {
    		if (mo1 < mo2) {
    			return -1;
    		} else if (mo1 > mo2) {
    			return 1;
    		} else {
    			if (day1 < day2) {
    				return -1;
    			} else if (day1 > day2) {
    				return 1;
    			} else {
    				if (hour1 < hour2) {
    					return -1;
    				} else if (hour1 > hour2) {
    					return 1;
    				} else {
    					return 0;
    				}
    			}
    		}
    	}
    }
    
    static int compareDaily(long unix1, long unix2) {
    	DateTime d1 = new DateTime(unix1 * 1000L);
    	DateTime d2 = new DateTime(unix2 * 1000L);
    	
    	int year1 = d1.getYear();
    	int year2 = d2.getYear();
//    	System.out.println(year1 + " vs. " + year2);
    	
    	int mo1 = d1.getMonthOfYear();
    	int mo2 = d2.getMonthOfYear();
//    	System.out.println(mo1 + " vs. " + mo2);
    	
    	int day1 = d1.getDayOfMonth();
    	int day2 = d2.getDayOfMonth();
//    	System.out.println(day1 + " vs. " + day2);
    	
    	if (year1 < year2) {
    		return -1;
    	} else if (year1 > year2) {
    		return 1;
    	} else {
    		if (mo1 < mo2) {
    			return -1;
    		} else if (mo1 > mo2) {
    			return 1;
    		} else {
    			if (day1 < day2) {
    				return -1;
    			} else if (day1 > day2) {
    				return 1;
    			} else {
    				return 0;
    			}
    		}
    	}
    	
    }
    
    public static int compareMonthly(long unix1, long unix2) {
    	DateTime d1 = new DateTime(unix1 * 1000L);
    	DateTime d2 = new DateTime(unix2 * 1000L);
    	
    	int year1 = d1.getYear();
    	int year2 = d2.getYear();
//    	System.out.println(year1 + " vs. " + year2);
    	
    	int mo1 = d1.getMonthOfYear();
    	int mo2 = d2.getMonthOfYear();
//    	System.out.println(mo1 + " vs. " + mo2);
    	
    	if (year1 < year2) {
    		return -1;
    	} else if (year1 > year2) {
    		return 1;
    	} else {
    		if (mo1 < mo2) {
    			return -1;
    		} else if (mo1 > mo2) {
    			return 1;
    		} else {
    			return 0;
    		}
    	}
    }
    
    public static int compareYearly(long unix1, long unix2) {
    	DateTime d1 = new DateTime(unix1 * 1000L);
    	DateTime d2 = new DateTime(unix2 * 1000L);
    	
    	int year1 = d1.getYear();
    	int year2 = d2.getYear();
//    	System.out.println(year1 + " vs. " + year2);
    	
    	if (year1 < year2) {
    		return -1;
    	} else if (year1 > year2) {
    		return 1;
    	} else {
    		return 0;
    	}
    	
    }
    	public final static SimpleDateFormat slashesFormater = new SimpleDateFormat("MM/dd/yy"); // Used by 
    
    public static int compareWeekly(long unix1, long unix2) {
    	DateTime d1 = new DateTime(unix1 * 1000L);
    	DateTime d2 = new DateTime(unix2 * 1000L);
    	
    	int year1 = d1.getYear();
    	int year2 = d2.getYear();
//    	System.out.println(year1 + " vs. " + year2);
    	
    	int mo1 = d1.getMonthOfYear();
    	int mo2 = d2.getMonthOfYear();
//    	System.out.println(mo1 + " vs. " + mo2);
    	
    	int w1 = d1.getWeekOfWeekyear(); // TODO test
    	int w2 = d2.getWeekOfWeekyear(); // TODO test
    	
    	if (year1 < year2) {
    		return -1;
    	} else if (year1 > year2) {
    		return 1;
    	} else {
    		if (mo1 < mo2) {
    			return -1;
    		} else if (mo1 > mo2) {
    			return 1;
    		} else {
    			if (w1 < w2) {
    				return -1;
    			} else if (w1 > w2) {
    				return 1;
    			} else {
    				return 0;
    			}
    		}
    	}
    }
    
    static int compareQuarterly(long unix1, long unix2) {
    	DateTime d1 = new DateTime(unix1 * 1000L);
    	DateTime d2 = new DateTime(unix2 * 1000L);
    	
    	int year1 = d1.getYear();
    	int year2 = d2.getYear();
//    	System.out.println(year1 + " vs. " + year2);
    	
    	int mo1 = d1.getMonthOfYear();
    	int mo2 = d2.getMonthOfYear();
//    	System.out.println(mo1 + " vs. " + mo2);
    	
    	int day1 = d1.getDayOfMonth();
    	int day2 = d2.getDayOfMonth();
//    	System.out.println(day1 + " vs. " + day2);
    	
    	int hour1 = d1.getHourOfDay();
    	int hour2 = d2.getHourOfDay();
    	
    	int minute1 = d1.getMinuteOfHour();
    	int minute2 = d2.getMinuteOfHour();
    	
    	if (year1 < year2) {
    		return -1;
    	} else if (year1 > year2) {
    		return 1;
    	} else {
    		if (mo1 < mo2) {
    			return -1;
    		} else if (mo1 > mo2) {
    			return 1;
    		} else {
    			if (day1 < day2) {
    				return -1;
    			} else if (day1 > day2) {
    				return 1;
    			} else {
    				if (hour1 < hour2) {
    					return -1;
    				} else if (hour1 > hour2) {
    					return 1;
    				} else {
    					if (Math.abs(minute2 - minute1) <= 15) {
    						return 0;
    					} else if (minute1 > minute2) {
    						return 1;
    					} else {
    						return -1;
    					}
    				}
    			}
    		}
    	}
	}
    
    boolean checkIndexOOB(int index) {
    	return index < 0 || index > this.getEndIdx();
    }
    
    /**
     * Returns DateTime for the given option
     * @param option CUR, START, END
     * @return DateTime depending on option
     */
    DateTime getDateTimeFor(int option) {
    	switch(option) {
	    	case START:
	    		return new DateTime(unixStart * 1000L);
	    	case CUR:
	    		return new DateTime(unixCurrent * 1000L);
	    	case END:
	    		return new DateTime(unixEnd * 1000L);
    	}
    	throw new RuntimeException("That is not an option for date!");
    }
    
    /**
     * Returns the Date object that the given index is referring to in this timeline
     * @param index
     * @return Date the given index is referring to in this timeline
     * @throws RuntimeException, if the given index is not within the timeline right now.
     */
    public DateTime getDateForIndex(int index) {
    	if (checkIndexOOB(index)) {
    		throw new RuntimeException(index + " is not within the current timerange!");
    	}
    	// System.out.println("ms = " + (unixStart + index * 15 * 60) * 1000L);
    	return new DateTime((unixStart + index * 15 * 60) * 1000L);
    }
    
	/**
	 * Jumps to the next section of time
	 */
	public void jumpToNextSection() {
		long unixNextStart = this.getEndUnix();
		updateTime(unixNextStart, howLongAmI);
	}
	
	public void jumpToPrevSection() {
		DateTime d = getDateTimeFor(CUR);

		long unixNextStart;

		switch (howLongAmI) {
			case YEARLY:
				d = d
								.withMillisOfSecond(0)
								.withSecondOfMinute(0)
								.withMinuteOfHour(0)
								.withHourOfDay(0)
								.withDayOfMonth(1)
								.withMonthOfYear(1);
				unixNextStart = d.minusYears(1).getMillis() / 1000;
				break;
			case MONTHLY:
				d = d
								.withMillisOfSecond(0)
								.withSecondOfMinute(0)
								.withMinuteOfHour(0)
								.withHourOfDay(0)
								.withDayOfMonth(1);
				unixNextStart = d.minusMonths(1).getMillis() / 1000;
				break;
			case WEEKLY:
				d = d
								.withMillisOfSecond(0)
								.withSecondOfMinute(0)
								.withMinuteOfHour(0)
								.withHourOfDay(0);
				unixNextStart = d.minusWeeks(1).getMillis() / 1000;
				break;
			case DAILY:
				d = d
								.withMillisOfSecond(0)
								.withSecondOfMinute(0)
								.withMinuteOfHour(0)
								.withHourOfDay(0);
				unixNextStart = d.minusDays(1).getMillis() / 1000;
				break;
			case HOURLY:
				d = d
								.withMillisOfSecond(0)
								.withSecondOfMinute(0)
								.withMinuteOfHour(0);
				unixNextStart = d.minusHours(1).getMillis() / 1000;
				break;
			default:
				throw new RuntimeException(howLongAmI.name() + " cannot be recognized!");
		}
		updateTime(unixNextStart, this.howLongAmI);
	}

	public long getPrevUnix() {
		
		DateTime d = getDateTimeFor(CUR);
		int newYearNum = d.getYear();
		int newMoNum = d.getMonthOfYear();
		int newDayNum = d.getDayOfMonth();
		int newHourNum = d.getHourOfDay();
		DateTime currentStart = new DateTime(newYearNum, newMoNum, newDayNum, newHourNum, 0, DateTimeZone.forID("America/New_York"));
		
		long unixNextStart;
		if (howLongAmI == IntervalType.YEARLY) {
			unixNextStart = currentStart.minusYears(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.MONTHLY) {
			unixNextStart = currentStart.minusMonths(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.WEEKLY) {
			unixNextStart = currentStart.minusWeeks(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.DAILY) {
			unixNextStart = currentStart.minusDays(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.HOURLY) {
			unixNextStart = currentStart.minusHours(1).getMillis() / 1000L;
		} else {
			throw new RuntimeException(howLongAmI.name() + " cannot be recognized!");
		}
		
		return unixNextStart;
	}
	
	public long getNextUnix() {
		DateTime d = getDateTimeFor(CUR);
		int newYearNum = d.getYear();
		int newMoNum = d.getMonthOfYear();
		int newDayNum = d.getDayOfMonth();
		int newHourNum = d.getHourOfDay();
		DateTime currentStart = new DateTime(newYearNum, newMoNum, newDayNum, newHourNum, 0, DateTimeZone.forID("America/New_York"));
		
		long unixNextStart;
		if (howLongAmI == IntervalType.YEARLY) {
			unixNextStart = currentStart.plusYears(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.MONTHLY) {
			unixNextStart = currentStart.plusMonths(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.WEEKLY) {
			unixNextStart = currentStart.plusWeeks(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.DAILY) {
			unixNextStart = currentStart.plusDays(1).getMillis() / 1000L;
		} else if (howLongAmI == IntervalType.HOURLY) {
			unixNextStart = currentStart.plusHours(1).getMillis() / 1000L;
		} else {
			throw new RuntimeException(howLongAmI.name() + " cannot be recognized!");
		}
		
		return unixNextStart;
	}
	
	
	public IntervalType getIntervalType() {
		return howLongAmI;
	}
	
	
	// =====================
	// Floor/Ceiling Methods
	// =====================
	
	public static long floorWeek(long unix) {
		DateTime d = new DateTime(unix * 1000L, DateTimeZone.forID("America/New_York"));
		return d.minusHours(d.getHourOfDay()).minusDays(d.getDayOfWeek() - 1).getMillis() / 1000L;
	}

	public static long ceilWeek(long unix) {
		DateTime d = new DateTime(unix * 1000L, DateTimeZone.forID("America/New_York"));
		return d.plusDays(7 - d.getDayOfWeek() + 1).minusHours(d.getHourOfDay()).getMillis() / 1000L;
	}
	
	public static long floorDate(long unix) {
		DateTime d = new DateTime(unix * 1000L, DateTimeZone.forID("America/New_York"));
		DateTime newD = new DateTime(d.getYear(), d.getMonthOfYear(), d.getDayOfMonth(),
										0, 0, 0, 000, DateTimeZone.forID("America/New_York"));
		return newD.getMillis() / 1000L;
	}
	
	public static long floorMonth(long unix) {
		DateTime d = new DateTime(unix * 1000L, DateTimeZone.forID("America/New_York"));
		DateTime newD = new DateTime(d.getYear(), d.getMonthOfYear(), 1,
										0, 0, 0, 000, DateTimeZone.forID("America/New_York"));
		return newD.getMillis() / 1000L;
	}
	
	// returns the unix timestamp of the floored year of the given unix
	public static long floorYear(long unix) {
		DateTime d = new DateTime(unix * 1000L, DateTimeZone.forID("America/New_York"));
		DateTime newD = new DateTime(d.getYear(), 1, 1,
										0, 0, 0, 000, DateTimeZone.forID("America/New_York"));
//		System.out.println(newD.getMillis() / 1000L + " - the floored year");
		return newD.getMillis() / 1000L;
	}
}
