# Problematic Microservices
A micro services application with various commonly encountered problems that can be used to train your profiling and diagnostic chops.

# Building
```bash
mvn package
```
# Running
The build will produce scripts to launch the different services. The different services are customer, factory and order. It is recommended to launch them in separate terminals.

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

# Selecting a Tracer
The example has support for two common tracers supporting Open Tracing - jaeger and zipkin. More can easily be added by updating se.hirt.examples.robotshop.common.opentracing.OpenTracingUtil and adding dependencies to the pom-file.

To switch tracer, either update robotshop-common/src/main/resources/tracing.properties, or set the 
system property tracingProperties to path of the properties file to use.

For convenience, here is to run Jaeger and Zipking locally via docker:

## Jaeger:

```bash
docker run -d -p 5775:5775/udp -p 16686:16686 jaegertracing/all-in-one:latest
```

## Zipkin:

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

Please see the tracing.properties file for more information.
