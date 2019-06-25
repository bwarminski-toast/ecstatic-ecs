## Goals

## Prerequisites

- Docker installed (Docker for Mac on OSX)

```
$ docker -v
Docker version 18.09.2, build 6247962
```

- Maven and Java 8

```
$ java -version
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)

$ mvn -version
Apache Maven 3.6.0 (97c98ec64a1fdfee7767ce5ffb20918da4f719f3; 2018-10-24T14:41:47-04:00)
Maven home: /usr/local/Cellar/maven/3.6.0/libexec
Java version: 1.8.0_202, vendor: Oracle Corporation, runtime: /Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "mac os x", version: "10.14.3", arch: "x86_64", family: "mac"

```

## Steps

### 1. Build a fat bundled Dropwizard microservice

You may already have a favorite service you'd like to build. If not, check out the hello webapp in `hello-world/`.

```
cd hello-world/hello
mvn package
```

Start the application locally:

```
java -jar target/hello-1.0-SNAPSHOT.jar server config.yml 
```

In a separate window, connect to it see your handy work:

```
$ curl http://localhost:8080/greeting
{"message":"Hi there, Stranger!"}

$ curl http://localhost:8080/greeting?name=Bob
{"message":"Hi there, Bob!"}
```

Congratulations! You've built a microservice. Let's add it to a container to see how it will be deployed in our AWS environment.


### 2. Create a container image including your newly compiled service

All containers start from a container image, which is pretty similar to a ZIP file or tar archive. Container images include a base
operating system and all the necessary files to run an application. In Docker, you build container images from a Dockerfile.

A Dockerfile is a set of instructions, much a bash script, that explain how to package an application's dependencies and run
it. These instructions are read in order by docker to build an image.


An example Dockerfile for our hello webapp is at `hello-world/hello/Dockerfile`. Let's break it down line by line.

 
```
FROM alpine:3.8
```

Which container image to base our new image from. Docker containers are built in layers and can be based on each other.
In this case, we're using a base container called `alpine` which is a small container friendly operating system that only has
a few basic things installed. (Docker people will tell you, the smaller the container the better).

```
RUN apk add --no-cache curl bash openjdk8-jre
```

A RUN step is simply a command that is run inside the container image's filesystem at build time. This is typically used to install
software or set up directory structures in the container.

In this step we're using `apk` to install java, curl and bash into the container. `apk` is similar to Homebrew, yum, or apt ... it's
a package manager specifically built for the alping operating system.

```
RUN mkdir /app
COPY target/hello-1.0-SNAPSHOT.jar /app
RUN mkdir /config
COPY config.yml /config
```

Here, we are executing additional RUN commands to create a directory story, and we're copying some files from our local filesystem
using the COPY directory.

```
ENTRYPOINT ["java", "-jar", "/app/hello-1.0-SNAPSHOT.jar", "server", "/config/config.yml"]
```

ENTRYPOINT tells docker which command to run when the container starts. (Kind of. There's some nuance we'll get into later)


Neat! Let's build docker container using this Dockerfile.

From within the `hello-world/hello` directory, run the following command:

```
docker build -t hello-webapp:v1.0 .
```

This command tells docker to read the Dockerfile in the current directory and build an image named `hello-webapp` and to tag
this version of the image as `v1.0`.

### 3. Run the container locally

```
docker run -p 8080:8080 -p 8081:8081 --name hello -it hello-webapp:v1.0
```

This will start the container.
 
The `-p` arguments tell docker to bind your local port 8080 to the container's port 8080, and your local 8081 to the container's
port 8081. 

`--name hello` gives our running container a friendly name so we can refer to it using other `docker` commands.

`-it` tells docker to run the application in an interactive terminal. This means that the container's standard output will go to
your terminals stdout, and anything you type will go to the containers standard input.

The last argument, `hello-webapp:v1.0` is simply our image and version.

Try our curl command from earlier if you don't believe me. There's a few other commands we can run in another window:

- `docker ps` will tell us the other containers that are running on our host
- `docker exec -it hello /bin/bash` will allow us to start a bash terminal from within the container. This is handy sometimes to get stack and heap dumps.
- `docker help` will list a whole bunch of other commands we can run to manipulate containers and images. Go ahead and try a few.

Once you're done playing with the container, pat yourself on the shoulder and ^C out of the running terminal or use `docker stop hello` from
another one.

## Congratulations!

You've successfully built your first containerized application and run it locally as it would have run on AWS. Our next step is
to get this thing running on a single EC2 instance, and that will require some additional setup.