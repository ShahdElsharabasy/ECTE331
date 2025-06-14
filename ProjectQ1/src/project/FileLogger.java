package project;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;


/**
 * Utility class for fault-tolerant file logging.
 * Handles logging attempts to a primary log file, then a series of backup files,
 * and finally to a principal log file if all previous attempts fail.
 * Includes a simulation for log file access failures.
 */
public class FileLogger {

	/**
	 * Maximum number of backup log files to attempt if the primary log file fails.
	 * This value can be configured as needed.
	 */
	public static final int MAX_BACKUP = 5;

	private static final String PRINCIPAL_LOG_FILE = "principal_log.txt";
	private final Random random = new Random();
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Constructs a FileLogger instance.
	 * Currently, no specific initialization is performed in the constructor,
	 * but it can be used for future enhancements if required.
	 */
	public FileLogger() {
		// Constructor for potential future initializations
	}

	/**
	 * Simulates a potential failure when attempting to write to a log file.
	 * According to the specification, there is a 40% chance of a simulated failure.
	 *
	 * @throws IOException if a simulated write failure occurs.
	 */
	private void simulateLogFileAccessFailure() throws IOException {
		// Simulate 40% failure rate as per requirements
		if (random.nextInt(100) < 40) {
			throw new IOException("Simulated write failure due to 40% chance");
		}
	}

	/**
	 * Attempts to log a message to the specified log file.
	 * This method encapsulates the actual file writing logic and includes a timestamp.
	 *
	 * @param filePath The path to the log file.
	 * @param message  The message to log.
	 * @param isPrincipalLog A boolean flag to indicate if this is the principal log file.
	 *                       If true, the simulated failure is bypassed.
	 * @return {@code true} if logging was successful, {@code false} otherwise.
	 */
	private boolean tryLog(String filePath, String message, boolean isPrincipalLog) {
		if (!isPrincipalLog) {
			try {
				simulateLogFileAccessFailure();
			} catch (IOException e) {
				System.err.println("LOGGING_SIMULATED_FAILURE: Attempt to write to '" + filePath + "' failed: " + e.getMessage());
				return false; // Failure due to simulation
			}
		}

		// Using try-with-resources to ensure FileWriter and PrintWriter are closed.
		// Appending to the file (true for FileWriter constructor).
		try (FileWriter fileWriter = new FileWriter(filePath, true);
			 PrintWriter printWriter = new PrintWriter(fileWriter)) {
			String timestamp = LocalDateTime.now().format(dateTimeFormatter);
			printWriter.println(timestamp + " - " + message);
			System.out.println("LOGGING_SUCCESS: Message logged to '" + filePath + "'");
			return true; // Logging successful
		} catch (IOException e) {
			// This catches actual IO errors during file writing.
			System.err.println("LOGGING_IO_ERROR: Could not write to '" + filePath + "': " + e.getMessage());
			return false; // Failure due to actual IO error
		}
	}

	/**
	 * Logs a message using a fault-tolerant mechanism.
	 * The method first attempts to log to the base log file. If this fails,
	 * it sequentially tries up to {@code MAX_BACKUP} backup files. If all these
	 * attempts fail, it makes a final attempt to log to the {@code PRINCIPAL_LOG_FILE}.
	 *
	 * @param baseLogFileName The name of the base log file (e.g., "application_log.txt").
	 *                        The backup file names are derived from this name.
	 * @param message         The message to be logged.
	 */
	public void log(String baseLogFileName, String message) {
		// Attempt 1: Log to the base log file
		if (tryLog(baseLogFileName, message, false)) {
			return; // Successfully logged to base file
		}

		// Attempt 2: Log to backup files if base file logging failed
		String baseName;
		String extension = "";
		int dotIndex = baseLogFileName.lastIndexOf('.');

		if (dotIndex > 0 && dotIndex < baseLogFileName.length() - 1) {
			baseName = baseLogFileName.substring(0, dotIndex);
			extension = baseLogFileName.substring(dotIndex); // Includes the dot (e.g., ".txt")
		} else {
			// Handles cases where the baseLogFileName might not have an extension
			baseName = baseLogFileName;
		}

		for (int i = 1; i <= MAX_BACKUP; i++) {
			String backupFileName = baseName + i + extension; // Generates names like "log1.txt", "log2.txt"
			if (tryLog(backupFileName, message, false)) {
				return; // Successfully logged to a backup file
			}
		}

		// Attempt 3: Log to the principal (ultimate fallback) log file
		System.out.println("LOGGING_FALLBACK: All primary and backup log attempts failed. Attempting to log to principal log: " + PRINCIPAL_LOG_FILE);
		if (!tryLog(PRINCIPAL_LOG_FILE, message, true)) { // isPrincipalLog is true to bypass simulation for this critical log
			// This is a critical failure if even the principal log fails.
			System.err.println("LOGGING_CRITICAL_FAILURE: Could not log to principal log file '" + PRINCIPAL_LOG_FILE + "'. Message lost: " + message);
		}
	}
}

