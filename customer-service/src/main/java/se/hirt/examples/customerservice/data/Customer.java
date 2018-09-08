/*
 * Copyright (C) 2018 Marcus Hirt
 *                    www.hirt.se
 *
 * This software is free:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright (C) Marcus Hirt, 2018
 */
package se.hirt.examples.customerservice.data;

import java.io.Serializable;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * A simple customer record.
 * 
 * @author Marcus Hirt
 */
public class Customer implements Serializable {
	private static final long serialVersionUID = -7669748978172006987L;

	public final static String KEY_CUSTOMER_ID = "customerId";
	public final static String KEY_FULL_NAME = "fullName";
	public final static String KEY_PHONE_NUMBER = "phoneNumber";

	private final long id;
	private final String fullName;
	private final String phoneNumber;

	Customer(long id, String fullName, String phoneNumber) {
		this.id = id;
		this.fullName = fullName;
		this.phoneNumber = phoneNumber;
	}

	public long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Customer other = (Customer) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public String toString() {
		return String.format("Customer [id=%d, fullName=%s, phoneNumber=%s", id, fullName, phoneNumber);
	}

	public static void validate(String fullName, String phoneNumber) throws ValidationException {
		validateFullName(fullName);
		validatePhoneNumber(phoneNumber);
	}

	private static void validateFullName(String fullName) throws ValidationException {
		if (fullName == null || fullName.isEmpty()) {
			throw new ValidationException("Empty fullName!");
		}
		if (!fullName.contains(" ")) {
			throw new ValidationException("Must have first and last name!");
		}
	}

	private static void validatePhoneNumber(String phoneNumber) throws ValidationException {
		// Just checking that the allowed characters are there
		String stripped = phoneNumber.replace(" ", "");
		if (!stripped.matches("\\+?[\\d-]*(?:\\(\\d+\\))??[\\d-]*")) {
			throw new ValidationException(phoneNumber + " is not a valid phone number!");
		}
	}

	public static Customer fromJSon(String jsonString) {
		JsonObject json = Json.createReader(new StringReader(jsonString)).readObject();
		return fromJSon(json);
	}

	public static Customer fromJSon(JsonObject json) {
		String customerIdStr = json.getString(Customer.KEY_CUSTOMER_ID);

		if ((customerIdStr == null)) {
			throw new IllegalArgumentException("Must have ID to create customer object from JSon");
		}

		long id = Long.parseLong(customerIdStr);
		String fullName = json.getString(Customer.KEY_FULL_NAME);
		String phoneNumber = json.getString(Customer.KEY_PHONE_NUMBER);
		return new Customer(Long.valueOf(id), fullName, phoneNumber);
	}

	public JsonObjectBuilder toJSon() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		// Cannot use longs since, guess what, JavaScript will round them. ;)
		builder.add(Customer.KEY_CUSTOMER_ID, String.valueOf(getId()));
		builder.add(Customer.KEY_FULL_NAME, getFullName());
		builder.add(Customer.KEY_PHONE_NUMBER, getPhoneNumber());
		return builder;
	}
}
