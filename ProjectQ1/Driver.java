package ProjectQ1;
import java.util.Random;
import java.util.Scanner;

//Represent a File Logger configuration.
public class File_Logger{
	// A constant indicating the maximum number of backup log files to maintain.
	final int Max_Backup = 5;

}

public class Driver {
	//This class handles user input, generates sensor data, and prints it to the console.
	private static final Random random = new Random();
	private static Scanner sc = new Scanner(System.in);
	
	
	public static void main(String[] args) {
		double [] third_sensor = new double[3];   // Array to store three readings from third sensor
		double cap; // Variable to store the user-defined upper limit (cap) for temperature
		
		System.out.println(" Please enter the cap of the temperature: ");
		cap =sc.nextDouble();
		
		double temperature = generateTemperature(cap); // Generate a random temperature value based on the provided cap
		System.out.println(" The randomly generated temperature is: "+ temperature);
		
		double humidity = generateHumidity(); // Generate a random humidity value.
		System.out.println(" The randomly generated humidity is: "+ humidity);
		
		for(int i=0;i<3; i++) { // Loop three times to generate and display values for the third sensor
			third_sensor[i] = generateThirdSensor();
			System.out.println("Value "+i+ " of third sensor is:"+third_sensor[i]);
		}
		sc.close(); //close scanner when no longer used
	}
	 // random.nextDouble() returns a value between 0.0 and 1.0.
        // Multiplying by 'cap' scales this to the desired range.

	private static double generateTemperature(double cap) {
		return random.nextDouble()*cap;
	}
	// Generates a value between 0.0 and 99
	private static double generateHumidity() {
		return random.nextDouble()*100;
}
	
	private static double generateThirdSensor() {
		return (int) (random.nextDouble()*10); //generates a double between 0.0 and 9
	}
	
	private static double majorityVoter(double [] third_sensor) {
		if (majorityValue != -1) {
    System.out.println("The majority value from the third sensor is: " + majorityValue);
} else {
    System.out.println("No majority value found for the third sensor readings.");
} 
	}
}
