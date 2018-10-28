package n9741232;

import java.util.*;

/**
 * Implementation of the class.
 *
 * @author Jordan Laptop
 *         Created on 23/10/2018
 *         Original Project: CAB401
 * @version 1.0
 * @since 1.0
 */
public class TimerLogic {

	public List<TimerData> data = new ArrayList<>();
	private List<TimerSet> dataSet = new ArrayList<>();


	/**
	 * Perform a runnable function for a set count.
	 * @param runCount The run count to run for.
	 * @param runnable The runnable.
	 */
	public void RunForIterations(long runCount, Runnable runnable) {
		data.clear();
		long i = 0;
		while (i < runCount) {
			TimerData dataRun = new TimerData();
			dataRun.start();
			runnable.run();
			dataRun.stop();
			data.add(dataRun);
			i++;
		}
	}

	/**
	 * Perform a runnable function for a set count, then log the results.
	 * @param runCount The run count to run for.
	 * @param filePath The file path to store the results at.
	 * @param runnable The runnable.
	 */
	public void RunForIterations(long runCount, String filePath, Runnable runnable) {
		RunForIterations(runCount, runnable);
		createLogFile(filePath);
	}

	/**
	 * Create a log file of the existing timer data.
	 * @param filePath
	 */
	public void createLogFile(String filePath) {
		CSV csv = new CSV();
		csv.createCSV(filePath);
		for (TimerData entry : data) {
			csv.addLong(entry.getTimeTaken());
			csv.addComma();
			csv.addLong(entry.getTimeStart());
			csv.addComma();
			csv.addLong(entry.getTimeStop());
			csv.addComma();
			csv.addNewLine();
		}
		csv.exportCSV();
	}

	/**
	 * Print the average of the time taken in ms.
	 */
	public void printAverageTimeTakenMs() {
		long timeTaken = 0;
		for (TimerData dataCount : data) {
			timeTaken += dataCount.getTimeTaken();
		}
		timeTaken /= data.size();
		System.out.println("Time Taken: " + timeTaken);
	}


	/**
	 * Print the times taken as a String
	 */
	public void printTimeData() {
		loadDataSet();
		for (TimerSet setElement : dataSet) {
			System.out.println(setElement.toString());
		}
	}

	/**
	 * Load the data set information.
	 */
	public void loadDataSet() {
		dataSet.clear();

		data.stream().forEach(val -> {
			boolean newNeeded = true;
			for (TimerSet element : dataSet) {
				if (element.getMs() == val.getTimeTaken()) {
					element.incrementCount();
					newNeeded = false;
					break;
				}
			}
			if (newNeeded) {
				dataSet.add(new TimerSet(val.getTimeTaken()));
			}
		});
	}

	private class TimerSet {

		private long ms;
		private int count;

		public TimerSet(long ms) {
			this.ms = ms;
			this.count = 1;
		}

		public void incrementCount() {
			count++;
		}

		public long getMs() {
			return ms;
		}

		public int getCount() {
			return count;
		}

		@Override
		public String toString() {
			return "TimerSet{ " + getMs() + ", " + getCount() + " }";
		}
	}

	private class TimerData {
		private long timeStart, timeStop, timeTaken;

		/**
		 * Start the Timer.
		 */
		public void start() {
			timeStart = System.currentTimeMillis();
		}

		/**
		 * Stop the Timer.
		 */
		public void stop() {
			timeStop = System.currentTimeMillis();
			timeTaken = timeStop - timeStart;
		}

		/**
		 * Get the amount of time that was taken.
		 * @return The time taken.
		 */
		public long getTimeTaken() {
			return timeTaken;
		}

		public long getTimeStart() {
			return timeStart;
		}

		public long getTimeStop() {
			return timeStop;
		}
	}
}
