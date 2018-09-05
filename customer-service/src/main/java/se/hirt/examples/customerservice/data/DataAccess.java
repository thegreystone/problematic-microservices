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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
/**
 * Simplified data access. 
 * 
 * FIXME: Store in couch base/cassandra/whatever:
 * 
 * @author Marcus Hirt
 */
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataAccess {
	private final static Map<Long, Customer> CUSTOMERS = new HashMap<>();
	private final static Map<String, Customer> CUSTOMERS_INDEX_BY_NAME = new HashMap<>();
	private final static Map<String, Customer> CUSTOMERS_INDEX_BY_PHONE = new HashMap<>();
	private final static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final static Random CUSTOMER_ID_GENERATOR = new Random();

	/**
	 * Synchronizing write access
	 * 
	 * @param fullName
	 * @param phoneNumber
	 * @return
	 */
	public static synchronized Customer createCustomer(String fullName, String phoneNumber) {
		Lock writeLock = readWriteLock.writeLock();
		try {
			writeLock.lock();
			Customer newCustomer = new Customer(getNewId(), fullName, phoneNumber);
			CUSTOMERS.put(newCustomer.getId(), newCustomer);
			CUSTOMERS_INDEX_BY_NAME.put(newCustomer.getFullName(), newCustomer);
			CUSTOMERS_INDEX_BY_PHONE.put(newCustomer.getPhoneNumber(), newCustomer);
			return newCustomer;
		} finally {
			writeLock.unlock();
		}
	}

	public static Collection<Customer> getAllCustomers() {
		Lock readLock = readWriteLock.readLock();
		try {
			return CUSTOMERS.values();
		} finally {
			readLock.unlock();
		}
	}

	public static Customer getCustomerByName(String fullName) {
		Lock readLock = readWriteLock.readLock();
		try {
			return CUSTOMERS_INDEX_BY_NAME.get(fullName);
		} finally {
			readLock.unlock();
		}
	}

	public static Customer getCustomerByPhone(String phone) {
		Lock readLock = readWriteLock.readLock();
		try {
			return CUSTOMERS_INDEX_BY_PHONE.get(phone);
		} finally {
			readLock.unlock();
		}
	}

	public static void removeCustomer(Customer customer) {
		Lock writeLock = readWriteLock.writeLock();
		try {
			writeLock.lock();
			CUSTOMERS.remove(customer.getId());
			CUSTOMERS_INDEX_BY_NAME.remove(customer.getFullName());
		} finally {
			writeLock.unlock();
		}
	}

	public static int getNumberOfCustomers() {
		Lock readLock = readWriteLock.readLock();
		try {
			return CUSTOMERS.size();
		} finally {
			readLock.unlock();
		}
	}

	public static Customer getCustomerById(Long id) {
		Lock readLock = readWriteLock.readLock();
		try {
			return CUSTOMERS.get(id);
		} finally {
			readLock.unlock();
		}
	}

	public static void updateCustomer(Long id, String fullName, String phoneNumber) {
		Lock writeLock = readWriteLock.writeLock();
		try {
			writeLock.lock();
			Customer updated = new Customer(id, fullName, phoneNumber);
			CUSTOMERS.put(id, updated);
		} finally {
			writeLock.unlock();
		}
	}

	private static long getNewId() {
		long newId = createNewId();
		// In the unlikely event of a collision...
		while (CUSTOMERS.get(newId) != null) {
			newId = createNewId();
		}
		return newId;
	}

	private static long createNewId() {
		return Math.abs(CUSTOMER_ID_GENERATOR.nextLong());
	}
}
