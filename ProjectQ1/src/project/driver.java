package project;

import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Scanner;
import java.util.Date;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class driver {
	private static final Random random = new Random();
	private static Scanner sc = new Scanner(System.in);
	private static double previousValidSensor3Value = -1.0;
	private static FileLogger logger = new FileLogger();
	private static final String BASE_LOG_FILE_NAME = "driver_sensor_log.txt";
	
	public static void main(String[] args) {
	
		logger.log(BASE_LOG_FILE_NAME, "--- System Simulation Started ---");
		
		System.out.println("Max backup logs: " + FileLogger.MAX_BACKUP);
		System.out.println("Principal log on failure: principal_log.txt");
		
		double[] third_sensor_readings = new double[3];
		double temperature_cap;
		
		System.out.println("Please enter the cap (maximum value) for the temperature sensor: ");
		temperature_cap = sc.nextDouble();
		logger.log(BASE_LOG_FILE_NAME, "User set temperature cap to: " + temperature_cap);
		
		double temperature = generateTemperature(temperature_cap);
		System.out.println("Sensor 1 (Temperature): " + temperature + " C");
		logger.log(BASE_LOG_FILE_NAME, "Sensor 1 (Temperature) reading: " + temperature + " C");
		
		double humidity = generateHumidity();
		System.out.println("Sensor 2 (Humidity): " + humidity + " %RH");
		logger.log(BASE_LOG_FILE_NAME, "Sensor 2 (Humidity) reading: " + humidity + " %RH");
		
		System.out.println("\n--- Sensor 3 (Critical Sensor) Replicas ---");
		for (int i = 0; i < 3; i++) {
			third_sensor_readings[i] = generateThirdSensor();
			System.out.println("Sensor 3, Replica " + (i + 1) + " value: " + third_sensor_readings[i]);
			logger.log(BASE_LOG_FILE_NAME, "Sensor 3, Replica " + (i + 1) + " raw value: " + third_sensor_readings[i]);
		}
		
		double finalSensor3Value = majorityVoter(third_sensor_readings);
		System.out.println("Sensor 3 (Critical Sensor) - Final Voted Value: " + finalSensor3Value);
		logger.log(BASE_LOG_FILE_NAME, "Sensor 3 (Critical Sensor) - Final Voted Value: " + finalSensor3Value);
		
		sc.close();
		System.out.println("\nSystem simulation finished.");
		logger.log(BASE_LOG_FILE_NAME, "--- System Simulation Finished ---");
	}
	
	private static String getCurrentTimestamp() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
	}
	
	private static double generateTemperature(double cap) {
		return random.nextDouble() * cap;
	}
	
	private static double generateHumidity() {
		return random.nextDouble() * 100.0;
	}
	
	private static double generateThirdSensor() {
		return (int) (random.nextDouble() * 21);
	}
	
	private static double majorityVoter(double[] third_sensor_values) {
		double s1 = third_sensor_values[0];
		double s2 = third_sensor_values[1];
		double s3 = third_sensor_values[2];
		String discrepancyMessagePrefix = "Sensor 3 Discrepancy: Values [" + s1 + ", " + s2 + ", " + s3 + "]. ";
		double resultValue;
		
		if (s1 == s2 && s2 == s3) {
			System.out.println("Majority Voter: All three Sensor 3 replicas agree. Value: " + s1);
			logger.log(BASE_LOG_FILE_NAME, "Sensor 3: All replicas agree. Value: " + s1);
			resultValue = s1;
			previousValidSensor3Value = resultValue;
		} else if (s1 == s2) {
			System.out.println("Majority Voter: Two Sensor 3 replicas agree (S1, S2). Value: " + s1 + ". Outlier: S3 (" + s3 + ")");
			logger.log(BASE_LOG_FILE_NAME, discrepancyMessagePrefix + "Majority is " + s1 + ". Outlier sensor ID: S3 (value " + s3 + ")");
			resultValue = s1;
			previousValidSensor3Value = resultValue;
		} else if (s1 == s3) {
			System.out.println("Majority Voter: Two Sensor 3 replicas agree (S1, S3). Value: " + s1 + ". Outlier: S2 (" + s2 + ")");
			logger.log(BASE_LOG_FILE_NAME, discrepancyMessagePrefix + "Majority is " + s1 + ". Outlier sensor ID: S2 (value " + s2 + ")");
			resultValue = s1;
			previousValidSensor3Value = resultValue;
		} else if (s2 == s3) {
			System.out.println("Majority Voter: Two Sensor 3 replicas agree (S2, S3). Value: " + s2 + ". Outlier: S1 (" + s1 + ")");
			logger.log(BASE_LOG_FILE_NAME, discrepancyMessagePrefix + "Majority is " + s2 + ". Outlier sensor ID: S1 (value " + s1 + ")");
			resultValue = s2;
			previousValidSensor3Value = resultValue;
		} else {
			System.out.println("Majority Voter: All three Sensor 3 replicas differ. No majority found.");
			logger.log(BASE_LOG_FILE_NAME, discrepancyMessagePrefix + "All replicas differ. No majority. Outlier IDs: S1, S2, S3.");
			
			if (previousValidSensor3Value != -1.0) {
				System.out.println("Majority Voter: Falling back to previously recorded valid value: " + previousValidSensor3Value);
				logger.log(BASE_LOG_FILE_NAME, "Sensor 3: Falling back to previous value: " + previousValidSensor3Value);
				resultValue = previousValidSensor3Value;
			} else {
				System.out.println("Majority Voter: No previous valid value available to fall back on. Resulting in -1.0.");
				logger.log(BASE_LOG_FILE_NAME, "Sensor 3: No previous value available for fallback. Resulting in -1.0.");
				resultValue = -1.0;
			}
		}
		return resultValue;
	}
}
