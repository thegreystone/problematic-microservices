package se.hirt.examples.robotfactory.data;

public class ValidationException extends Exception {
	private static final long serialVersionUID = 7205563550725471828L;

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ValidationException(String message) {
		super(message);
	}
}
