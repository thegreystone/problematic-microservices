package se.hirt.examples.robotfactory.rest;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import se.hirt.examples.robotfactory.data.Color;
import se.hirt.examples.robotfactory.data.Robot;

@Path("/paints/")
public class PaintsResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonArray list() {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Color c : Color.values()) {
			JsonObjectBuilder colorBuilder = Json.createObjectBuilder();
			colorBuilder.add(Robot.KEY_COLOR, c.toString());
			builder.add(colorBuilder);
		}
		return builder.build();
	}
}
