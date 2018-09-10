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
package se.hirt.examples.robotshop.loadgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple little load generator to this example application.
 * 
 * @author Marcus Hirt
 */
public class LoadGenerator {
	private static ScheduledExecutorService threadPool;

	public static void main(String[] args) throws InterruptedException, IOException {
		Properties props = new Properties();

		if (args.length == 1) {
			File f = new File(args[0]);
			try (InputStream stream = new FileInputStream(f)) {
				props.load(stream);
			} catch (IOException e) {
				System.out
						.println("First argument, if available, must be the path to an existing load.properties file.");
				System.out.println("Error was: " + e.getMessage());
				System.exit(2);
			}
		} else {
			try (InputStream stream = LoadGenerator.class.getClassLoader().getResourceAsStream("load.properties")) {
				props.load(stream);
			} catch (IOException e) {
				System.out.println("Using default load.properties file failed.");
				System.out.println("Error was: " + e.getMessage());
				System.exit(3);
			}
		}

		int threadCount = Integer.parseInt(props.getProperty("threadCount"));
		int workerCount = Integer.parseInt(props.getProperty("workerCount"));
		int period = Integer.parseInt(props.getProperty("period"));

		threadPool = Executors.newScheduledThreadPool(threadCount, new LoadGeneratorThreadFactory());

		int delay = period <= 0 ? 0 : workerCount / period;
		int currentDelay = 0;
		for (int i = 0; i < workerCount; i++) {
			threadPool.scheduleAtFixedRate(new LoadWorker(props), currentDelay, period, TimeUnit.MILLISECONDS);
			currentDelay += delay;
		}
		System.out.println("Started load generator with " + threadCount + " threads.");
		System.out.println("Press <enter> to quit!");
		System.in.read();
		System.out.print("Shutting down...");
		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.SECONDS);
		System.out.println(" done!");
	}

}
