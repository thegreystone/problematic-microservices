package se.hirt.examples.robotorder.data;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import se.hirt.examples.robotfactory.data.Color;
import se.hirt.examples.robotfactory.data.Robot;
import se.hirt.examples.robotfactory.data.RobotType;

public final class RobotOrderLineItem {
	private final String robotTypeId;
	private final Color color;

	public RobotOrderLineItem(String robotTypeId, Color color) {
		this.robotTypeId = robotTypeId;
		this.color = color;
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add(RobotType.KEY_ROBOT_TYPE, robotTypeId);
		builder.add(Robot.KEY_COLOR, color.toString());
		return builder;
	}

	public static RobotOrderLineItem fromJSon(JsonObject json) {
		String type = json.getString(RobotType.KEY_ROBOT_TYPE);
		Color color = Color.valueOf(json.getString(Robot.KEY_COLOR));
		return new RobotOrderLineItem(type, color);
	}

	public static RobotOrderLineItem fromJSon(JsonValue jsonValue) {
		return fromJSon(jsonValue.asJsonObject());
	}

	public String getRobotTypeId() {
		return robotTypeId;
	}

	public Color getColor() {
		return color;
	}

}
