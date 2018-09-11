package se.hirt.examples.robotshop.loadgenerator;

import java.util.Random;

public class Phones {
	public final static Random RND = new Random();

	// Just a few well known country codes
	private final static int [] COUNTRY_CODES = new int [] {1, 41, 46, 30, 31, 32, 33, 34, 36, 39, 43, 44, 45, 47, 48, 49};

	public static String getRandomPhone() {
		
		return "+" + COUNTRY_CODES[RND.nextInt(COUNTRY_CODES.length)] + "-(0)7" + RND.nextInt(10) + "-" + (RND.nextInt(900) + 100) + " " + (RND.nextInt(90) + 10) + " " + (RND.nextInt(90) + 10);
	}
	
	public static void main(String [] args) {
		for (int i = 0; i < 10; i++) {
			System.out.println(getRandomPhone());
		}
	}
}
