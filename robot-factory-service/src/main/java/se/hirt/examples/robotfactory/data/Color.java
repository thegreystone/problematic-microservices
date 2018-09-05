/*
 * Copyright (C) 2018 Marcus Hirt
 */
package se.hirt.examples.robotfactory.data;

/**
 * Just a color.
 * 
 * @author Marcus Hirt
 */
public enum Color {
	RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW, BLACK, WHITE, PINK;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
	
	public static Color fromString(String colorName) {
		return valueOf(colorName.toUpperCase());
	}
}
