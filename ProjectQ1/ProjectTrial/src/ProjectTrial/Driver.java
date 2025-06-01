package ProjectTrial;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class Driver {

	private final Random random = new Random();
	private final FileLogger logger; // Instance of our FileLogger
	private static final String BASE_LOG_FILE_NAME = "driver_system_events.log"; // Log file for Driver

	// Sensor value ranges (configurable as integers)
	
	private static final int CRITICAL_SENSOR_MIN = 100; // Arbitrary units for critical sensor
	private static final int CRITICAL_SENSOR_MAX = 200; // Arbitrary units for critical sensor

	private int previousValidCriticalValue = -1; // Stores the last known good value for Sensor 3

	/**
	 * Constructor for Driver. Initializes the FileLogger.
	 */
	public Driver() {
		this.logger = new FileLogger();
	}

	
	public int generateTemperature() {
		return random.nextDouble()*cap;
	}

	
	public int generateHumidity() {
		return random.nextDouble()*100;
	}

	
	public int generateThirdSensor() {
		return (int) (random.nextDouble()*21);
	}

	
	public int majorityVoter(int replica1, int replica2, int replica3) {
		System.out.println("Sensor 3 - Replicas: [S3.1: " + replica1 + ", S3.2: " + replica2 + ", S3.3: " + replica3 + "]");

		Map<Integer, Integer> valueCounts = new HashMap<>();
		valueCounts.put(replica1, valueCounts.getOrDefault(replica1, 0) + 1);
		valueCounts.put(replica2, valueCounts.getOrDefault(replica2, 0) + 1);
		valueCounts.put(replica3, valueCounts.getOrDefault(replica3, 0) + 1);

		int determinedValue = -1; // Default if no logic path sets it
		int maxCount = 0;

		for (Map.Entry<Integer, Integer> entry : valueCounts.entrySet()) {
			if (entry.getValue() > maxCount) {
				maxCount = entry.getValue();
				determinedValue = entry.getKey();
			}
		}

		boolean discrepancyOccurred = !(s3_replica1 == s3_replica2 && s3_replica2 == s3_replica3);

		if (discrepancyOccurred) {
			StringBuilder discrepancyLogMessage = new StringBuilder("Sensor 3 Discrepancy. Values: ");
			discrepancyLogMessage.append("S3.1=").append(replica1)
								 .append(", S3.2=").append(replica2)
								 .append(", S3.3=").append(replica3).append(". ");
			List<String> outlierSensorIDs = new ArrayList<>();

			if (maxCount < 2) { // All three values are different, no majority. All are outliers.
				discrepancyLogMessage.append("All replicas differ (no majority). ");
				outlierSensorIDs.add("S3.1 (value " + replica1 + ")");
				outlierSensorIDs.add("S3.2 (value " + replica2 + ")");
				outlierSensorIDs.add("S3.3 (value " + replica3 + ")");
			} else { // Majority (maxCount >= 2) exists, identify the non-majority (outlier) sensor(s).
				if (s3_replica1 != determinedValue) outlierSensorIDs.add("S3.1 (value " + s3_replica1 + ")");
				if (s3_replica2 != determinedValue) outlierSensorIDs.add("S3.2 (value " + s3_replica2 + ")");
				if (s3_replica3 != determinedValue) outlierSensorIDs.add("S3.3 (value " + s3_replica3 + ")");
			}
			if (!outlierSensorIDs.isEmpty()) {
				discrepancyLogMessage.append("Outlier Sensor ID(s): ").append(String.join(", ", outlierSensorIDs));
			}
			logger.log(BASE_LOG_FILE_NAME, discrepancyLogMessage.toString());
			System.out.println("SYSTEM_CONSOLE: " + discrepancyLogMessage); // Also print to console for visibility
		}

		if (maxCount >= 2) { // Majority of 2 or 3 exists
			System.out.println("Sensor 3 - Majority Voter: Determined value is " + determinedValue + " (based on " + maxCount + " identical readings)");
			previousValidCriticalValue = determinedValue; // Update the known good value
			return determinedValue;
		} else { // No majority (all values different, maxCount will be 1)
			System.out.println("Sensor 3 - Majority Voter: No majority found (all three replica values are different).");
			if (previousValidCriticalValue != -1) {
				System.out.println("Sensor 3 - Majority Voter: Falling back to previous valid value: " + previousValidCriticalValue);
				// Log this fallback event clearly
				logger.log(BASE_LOG_FILE_NAME, "Sensor 3: No majority, fell back to previous value: " + previousValidCriticalValue +
											 ". Current differing values were: S3.1=" + s3_replica1 + ", S3.2=" + s3_replica2 + ", S3.3=" + s3_replica3);
				return previousValidCriticalValue;
			} else {
				System.out.println("Sensor 3 - Majority Voter: No previous valid value available, returning -1 as error/default.");
				// Log this critical state
				logger.log(BASE_LOG_FILE_NAME, "CRITICAL_VOTER_ALERT: No majority in Sensor 3 and no previous value available. " +
											 ". Values: S3.1=" + s3_replica1 + ", S3.2=" + s3_replica2 + ", S3.3=" + s3_replica3 + ". Returning -1.");
				return -1; // Error or default indicator, as no fallback is possible
			}
		}
	}

	
	public void runSingleSimulationCycle() {
		System.out.println("\n--- New Simulation Cycle ---");

		// Sensor 1: Temperature
		int currentTemperature = generateTemperature();
		System.out.println("Sensor 1 (Temperature): " + currentTemperature + " C");
		logger.log(BASE_LOG_FILE_NAME, "Temperature reading: " + currentTemperature + " C");

		// Sensor 2: Humidity
		int currentHumidity = generateHumidity();
		System.out.println("Sensor 2 (Humidity): " + currentHumidity + " % RH");
		logger.log(BASE_LOG_FILE_NAME, "Humidity reading: " + currentHumidity + " % RH");

		// Sensor 3: Critical Sensor (with three replicas)
		int s3_val1 = generateCriticalSensorReplicaValue();
		int s3_val2 = generateCriticalSensorReplicaValue();
		int s3_val3 = generateCriticalSensorReplicaValue();

		// Forcing specific discrepancy scenarios for Sensor 3 for demonstration purposes
		// This helps ensure all logic paths in the voter and logger are tested.
		int scenarioChance = random.nextInt(10); // 0-9
		if (scenarioChance < 1) { // Approx. 10% chance: all different
			s3_val1 = CRITICAL_SENSOR_MIN + 5;  // e.g., 105
			s3_val2 = CRITICAL_SENSOR_MIN + 15; // e.g., 115
			s3_val3 = CRITICAL_SENSOR_MAX - 5;  // e.g., 195
			System.out.println("SIMULATION_INFO: Forcing Sensor 3 replicas to ALL DIFFERENT for this cycle.");
		} else if (scenarioChance < 3) { // Approx. 20% chance (10% + 20% = 30% total for special scenarios): two same, one different
			s3_val1 = CRITICAL_SENSOR_MIN + 10; // e.g., 110
			s3_val2 = CRITICAL_SENSOR_MIN + 10; // e.g., 110
			s3_val3 = CRITICAL_SENSOR_MAX - 10; // e.g., 190
			System.out.println("SIMULATION_INFO: Forcing Sensor 3 replicas to TWO SAME, ONE DIFFERENT for this cycle.");
		}
		// Otherwise (70% of the time), values are purely random based on generateCriticalSensorReplicaValue()

		int finalCriticalSensorValue = majorityVoterForSensor3(s3_val1, s3_val2, s3_val3);
		System.out.println("Sensor 3 (Critical Voted Output): " + finalCriticalSensorValue +
						   " (Previous valid value was: " + (previousValidCriticalValue == -1 ? "N/A" : previousValidCriticalValue) + ")");
		logger.log(BASE_LOG_FILE_NAME, "Critical Sensor Voted Output: " + finalCriticalSensorValue);

		// Example of another system decision based on sensor values, also logged
		if (currentTemperature > 38 && finalCriticalSensorValue > (CRITICAL_SENSOR_MIN + CRITICAL_SENSOR_MAX)/2 ) {
			String complexAlert = "ALERT: High Temperature ("+currentTemperature+"C) AND High Critical Sensor Reading ("+finalCriticalSensorValue+") detected!";
			System.out.println(complexAlert);
			logger.log(BASE_LOG_FILE_NAME, complexAlert);
		}
		System.out.println("--- End of Cycle ---");
	}

	/**
	 * Main method to start and run the embedded system simulation.
	 * @param args Command-line arguments (not used in this simulation).
	 */
	public static void main(String[] args) {
		Driver systemSimulator = new Driver();

		// It's good practice to establish an initial previousValidCriticalValue if possible.
		// This run of the voter will set it, and log if there's an initial discrepancy.
		System.out.println("Performing initial read for Sensor 3 to establish a baseline for 'previousValidCriticalValue'...");
		int init_s3_1 = systemSimulator.generateCriticalSensorReplicaValue();
		int init_s3_2 = systemSimulator.generateCriticalSensorReplicaValue();
		int init_s3_3 = systemSimulator.generateCriticalSensorReplicaValue();
		int initialVotedVal = systemSimulator.majorityVoterForSensor3(init_s3_1, init_s3_2, init_s3_3);
		// The previousValidCriticalValue is now set internally by the above call if a majority was found.
		if (initialVotedVal != -1) { // Or check systemSimulator.previousValidCriticalValue
			 System.out.println("Initial baseline for Sensor 3's previousValidCriticalValue has been set to: " + systemSimulator.previousValidCriticalValue);
		} else {
			System.out.println("Initial baseline for Sensor 3 could not be established via majority vote (current values likely differed and no prior existed). Fallback will be -1 if needed on first real cycle.");
		}



