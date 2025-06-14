package ProjectTrial;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;



public class FileLogger {
	
	public class File_Logger{

	public static final int MAX_BACKUP = 5;
	private final Random random = new Random();
	private static final String PRINCIPAL_LOG_FILE = "principal_log.txt";
	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	}

	
	public FileLogger() {
		// Constructor for potential future initializations
	}

	private void simulateLogFileAccessFailure() throws IOException {
		// Simulate 40% failure rate
		if (random.nextInt(100) < 40) {
			throw new IOException("Simulated write failure due to 40% chance");
		}
	}

	
	private boolean tryLog(String filePath, String message, boolean isPrincipalLog) {
		if (!isPrincipalLog) {
			try {
				simulateLogFileAccessFailure();
			} catch (IOException e) {
				System.err.println("LOGGING_SIMULATED_FAILURE: try writing to '" + filePath + "' failed: " + e.getMessage());
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

