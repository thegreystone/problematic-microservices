package se.hirt.examples.robotshop.loadgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.stream.Collectors;

public class Utils {
	private Utils() {
		throw new UnsupportedOperationException("Toolkit!");
	}

	/**
	 * Reads the named resource to a String.
	 *
	 * @param resourceName
	 *            the name of the resource
	 * @return
	 * @throws IOException 
	 */
	public static String getResource(String resourceName) throws IOException {
		try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceName))  {
			if (is != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}
		return null;
	}
	
	public static Properties loadPropertiesFromResource(String resourceName) throws IOException {
		Properties props = new Properties();
		try (InputStream stream = LoadGenerator.class.getClassLoader().getResourceAsStream(resourceName)) {
			props.load(stream);
		} 
		return props;

	}

	public static Properties loadPropertiesFromFile(File file) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		try (InputStream stream = new FileInputStream(file)) {
			props.load(stream);
		}
		return props;
	}
}
