# Sample Clustered Application Using EasyRouting for Vert.x

Building clustered applications and microservices can seem daunting. Typically, it requires deep knowledge, heavy
configuration, and quite a bit of boilerplate code. However, with [EasyRouting](https://github.com/gregory-ledenev/vert.x-easyrouting) for Vert.x, you can get started with minimal setup and a very short learning curve.

In this tutorial, we’ll walk step-by-step through creating a simple clustered application using EasyRouting.

***

## Step 1: A Simple Hello World Application

We’ll begin by creating a basic EasyRouting app that responds to HTTP `GET` requests with “Hello World!”.

First, set up a Maven project and add the EasyRouting dependency:

```xml

<dependency>
    <groupId>io.github.gregory-ledenev</groupId>
    <artifactId>vert.x-easyrouting</artifactId>
    <version>0.9.17</version>
</dependency>
```

Here’s our minimal application:

```java
public class ClusteredApplication extends Application {
    @GET("/*")
    public String helloWorld() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        new ClusteredApplication().start();
    }
}
```

Run this application and open a browser at [http://localhost:8080]() to see it in action.

***

## Step 2: Enabling Clustering

Turning this into a clustered app is surprisingly easy - add the `clustered("main")` initializer:

```java
public class ClusteredApplication extends Application {
    @GET("/*")
    public String helloWorld() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        new ClusteredApplication()
                .clustered("main")
                .start();
    }
}
```

Once you run the app, you’ll see that it is now running in clustered mode and is part of a cluster — even if it’s
currently the only member:

```
Members {size:1, ver:1} [
    Member [192.168.1.207]:5701 - 78278fe3-fdc3-4926-ac10-e870554b5659 this
]
```
EasyRouting hides the complexity of forming a cluster, registering/unregistering nodes with it, etc. 

***

## Step 3: Running Multiple Nodes

Let’s extend this further and allow the app to run with multiple nodes. By passing in a port and a node name, we can
allow starting  several instances of the same application on the same host.

- If no arguments are given → node defaults to `"main"`, running on port `8080`.
- Otherwise → node name = `"node<port>"` and it runs on the provided port.

```java
public static void main(String[] args) {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
    String nodeName = args.length > 0 ? args[1] : "node" + port;

    new ClusteredApplication()
            .clustered(nodeName)
            .start(port);
}
```

You can now launch multiple instances (e.g., "main" on `8080`, "node1" on "`8081`, "node2" on "`8082`), and they will automatically form a cluster:

```
Members {size:3, ver:3} [
    Member [192.168.1.207]:5701 - 78278fe3-fdc3-4926-ac10-e870554b5659
    Member [192.168.1.207]:5702 - 6ccc86fb-9fd6-4489-a9e3-a95f9fd22d8c
    Member [192.168.1.207]:5703 - 32fb07aa-edd0-4441-9d43-5f86f54bf0bb this
]
```

***

## Step 4: Inter-Node Communication

Now that we have multiple nodes running, let’s make them talk to each other.

Imagine this setup:

- **main node** → aggregates greetings.
- **node1** and **node2** → provide their own greetings.

The question is: *How can the main node discover and talk to the microservice nodes?*

This is where EasyRouting helps us. It allows you to declare URIs for cluster members simply by using annotations with particular node names:

```java

@GET("/*")
public String helloWorld(@ClusterNodeURI("node1") URI node1,
                         @ClusterNodeURI("node2") URI node2) {
    // add your code here
}
```

EasyRouting automatically discovers and injects the addresses of running nodes with the given names — no manual discovery or
configuration required.

***

## Step 5: Collecting Greetings from Nodes

Let’s finish the `helloWorld` method by fetching greetings from the other nodes and aggregating them:

```java

@GET("/*")
public String helloWorld(@ClusterNodeURI("node1") URI node1,
                         @ClusterNodeURI("node2") URI node2) {
    List<String> result;
    HttpClient client = HttpClient.newHttpClient();
    try {
        result = new ArrayList<>();

        result.add("Hello World!");
        appendHelloFromNode(client, node1, result);
        appendHelloFromNode(client, node2, result);
    } finally {
        client.close();
    }

    return String.join(", ", result);
}
```
Note: this code uses `appendHelloFromNode()` method that fetches greetings and the `helloFromNode()` handler that provide actual greeting - check the project to get them.

Run the main node and start up `node1` and `node2`. Now, when you open [http://localhost:8080](), the main node will return a **collective greeting**:

```
Hello World!, Hello from 'node1', Hello from 'node2'
```

***

## Conclusion

In just a handful of lines, we’ve gone from a simple standalone “Hello World” app to a minimal **clustered microservices setup** with internode communication — powered by Vert.x and EasyRouting.

With:

- a couple of annotations,
- minimal configuration, and
- a clean programming model,

you’re ready to scale your Vert.x applications into clustered environments.