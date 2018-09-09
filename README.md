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
