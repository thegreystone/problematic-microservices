# problematic-web-app
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

# Running the Load Generator
To generate a bit of load on the problematic application, there is a simple load generator included.

On Windows use:

```bash
target\bin\loadgenerator.bat [<path to custom load.properties file>]
```

On Mac OS X use:

```bash
target/bin/loadgenerator [<path to custom load.properties file>]
```

To use custom settings for the load generator, simply copy the default src/main/resources/load.properties
file and provide the path to your copy as the first argument to the script. If no argument is provided
the default will be used.
