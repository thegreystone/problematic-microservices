# problematic-web-app
A web application with various commonly encountered problems that can be used to train your profiling and diagnostic chops. This web application requires JDK 11 to build and run.

# Building
```bash
mvn package
```
# Running
The build will produce a batch file to launch the application. 

On Windows use:

```bash
target\bin\webapp.bat
```

On Mac OS X use:

```bash
target/bin/webapp
```

Port is by default 8080. Set the environment variable PORT to change.

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
