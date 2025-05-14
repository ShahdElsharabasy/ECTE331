package ProjectQ1;

import java.util.Random;
import java.util.Scanner;

public class File_Logger{
	final int Max_Backup = 5;
}

public class Driver {
	
	private static final Random random = new Random();
	private static Scanner sc = new Scanner(System.in);
	
	
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
	}
	
	private static double generateTemperature(double cap) {
		return random.nextDouble()*cap;
	}
	private static double generateHumidty() {
		return random.nextDouble()*100;
}
	
	private static double genrateThirdSensor() {
		return (int) (random.nextDouble)();
	}
}
