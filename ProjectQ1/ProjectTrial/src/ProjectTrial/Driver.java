package ProjectTrial;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Scanner;


public class Driver {
	
	private static final Random random = new Random();
	private static Scanner sc = new Scanner(System.in);
	private static int majorityValue;
	
	
	public static void main(String[] args) {
		double [] third_sensor = new double[3];
		double cap;
		
		System.out.println(" Please enter the cap of the temperature: ");
		cap =sc.nextDouble();
		
		double temperature = generateTemperature(cap);
		System.out.println(" The randomly generated temperature is: "+ temperature);
		
		double humidty = generateHumidty();
		//System.out.println(" Please Enter a humidty: "+ humidty);
		
		
		System.out.println(" The randomly generated humidity is: "+ humidty);
		
		for(int i=0;i<3; i++) {
			third_sensor[i] = genrateThirdSensor();
			System.out.println("Value "+i+ " of third sensor is:"+third_sensor[i]);
		}
		sc.close(); //close the scanner when not needed
		
	}
	private static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
	
	private static double generateTemperature(double cap) {
		return random.nextDouble()*cap;
	}
	private static double generateHumidty() {
		return random.nextDouble()*100;
}
	
	private static double generateThirdSensor() {
		return (int) (random.nextDouble()*21);
	}
	private static double majorityVoter(double [] third_sensor) {
		if (majorityValue != -1) {
    System.out.println("The majority value from the third sensor is: " + majorityValue);
} else {
    System.out.println("No majority value found for the third sensor readings.");
} 
}
