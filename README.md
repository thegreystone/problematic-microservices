# Problematic Microservices
A micro services application, running on Embedded Tomcat, with various commonly encountered problems that can be used to train your profiling and diagnostic chops.

# Building
```bash
mvn package
```
# Running
The build will produce scripts to launch the different services. The different services are customer, factory and order. It is recommended to launch them in separate terminals.
On Windows the Classpath length will make the scripts become unusable. Either use the Eclipse launcher (see running from within Eclipse) or modify the generated scripts slightly.

In each script replace this

```bash
set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\<very-long-line>
```
by
```bash
set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\*
```

On Windows, use:

```bash
cd robotshop-<service>-service
target\bin\robot<Service>Service.bat
```

On Mac OS X use:

```bash
cd robotshop-<service>-service
target/bin/robot<Service>Service
```

Port is by default 8081 for the customer service, 8082 for the factory service and 8083 for the order service. Set the environment variable PORT to change.

If you change the ports, or if you run the services on different hosts, you will also need to set the following environment variables for the order service:

Windows:

```bash
set FACTORY_SERVICE_LOCATION=http://<host>:<port>
set CUSTOMER_SERVICE_LOCATION=http://<host>:<port>
```

Mac OS X:

```bash
export FACTORY_SERVICE_LOCATION=http://<host>:<port>
export CUSTOMER_SERVICE_LOCATION=http://<host>:<port>
```

For some examples on API usage, see the following Postman collection:
https://www.getpostman.com/collections/622fac93f3f20b1bd70b

The easiest way to start running is probably to use one of the load generating scripts, for example:

```bash
cd robot-shop-load
target/bin/loadWorker
```

It will shoot off a full systems test, involving creating a random user, submitting an random order with a random number of robots, picking up the order and then deleting the user. Press enter to create another, or q followed by enter to quit.

# Selecting a Tracer
The example has support for two common tracers supporting Open Tracing - jaeger and zipkin. More can easily be added by updating se.hirt.examples.robotshop.common.opentracing.OpenTracingUtil and adding dependencies to the pom-file.

To switch tracer, either update robotshop-common/src/main/resources/tracing.properties, or set the 
system property tracingProperties to path of the properties file to use.

For convenience, here are instructions on how to run Jaeger or Zipkin locally via docker:

## Jaeger:

```bash
docker run -d -p 5775:5775/udp -p 16686:16686 jaegertracing/all-in-one:latest
```

## Zipkin:

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

Please see the tracing.properties file for more information.

## The JFR Tracer
Note that all of the tracers will use the delegating [JFR Tracer](https://github.com/thegreystone/jfr-tracer), making it easy to correlate span IDs with information from the JDK Flight Recorder. Simply dump the flight recorder to get detailed information about what is really going on.

# Running in Eclipse
To run the Microservices from within Eclipse, simply import the top project as a Maven project.

1. Select File | Import from the Menu.
2. In the Import Wizard, select Existing Maven Project.
3. Select the project root as root and import.

Once imported, the various launchers should be available. There are launchers for running the services individually,
as well as starting them all together in one go (Launcher Group). The launch group can also be used for starting
all the services in debug mode, making it easy to set break point and look at how the various services interact.

Note that you will likely see some warnings from the Tomcat StandardJarScanner when starting the services from 
within Eclipse, e.g.:

```bash
sep 11, 2018 12:25:14 EM org.apache.tomcat.util.scan.StandardJarScanner scan
VARNING: Failed to scan ...
```

These can be safely ignored.

# About
Feel free to improve on the example application! I am no Micro-Services person by any means. That said, note that some 
choices, like making the JSon serialization manually, were made to make it easier to create various problems later on, 
or to make it easier to play around with the code.

