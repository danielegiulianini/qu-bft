# BFT Query/Update protocol


## Introduction
---------------	

This repository contains an implementation of the Q/U protocol, a tool that enables construction of fault-scalable Byzantine fault tolerant services by an operations-based interface with a focus on fault-scalability, described by authors in the paper [Fault-Scalable Byzantine Fault-Tolerant Services](https://cs.brown.edu/courses/csci2950-g/papers/qu.pdf).

Developed in gRPC and scala with a strongly modular approach, it's available in this repo either as:
- an extensible and reusable scala library, providing an access point to Q/U's client and service functionalities,
- a console line, demo application for showcasing its potentialities and acting as an example for the construction of more complex services.


## Features
---------------	

The paper's provided features covered by the lib are (refer to the paper for the terminology):

- single object-update basic functioning:
	- clients authentication/authorization
	- query and update submission
	- threshold quorum construction with recursive thresholds quorum constructions
	- Replica History integrity check by HMAC
	- client and service broadcast for quorum construction
	- client's retry and random exponential backoff policy (repair)
	- object syncing at server

- Optimizations:
	- compact timestamp
	- replica history pruning
	- optimistic query execution
	- handling repeated request at the server
	- inline repair
	- caching of Object History Set

Additionally, the library features also:
- 	async rivisitation of Q/U protocol, by proving a non blocking client and service APIs leveraging scala's Future
-	 strong type check at compile time by leveraging scala's strong statically typed type system
- 	Json Web Token (JWT) based authentication
- 	JSON (de)serialization by Jackson


Refer to [issues page](https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920/-/issues) for a deeper overview of the main functionalities. 



## How to deploy
---------------	

### Library

#### For sbt users

##### Prerequisities:

-	Scala 2.13.8
-	Java 11
-	Sbt 1.6.2
-	Git

At the moment, the library is not available on a public remote repository. So, the steps below show how to public it locally.

1. clone the repo into the desired folder:
```bash
    git clone https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920
```
1. move inside the downloaded folder:
```bash
    cd ds-project-giulianini-ay1920
```
1. publish the library on a local repository:
```bash
    sbt publish-local
```
1. add the dependencies of interest to your build.sbt; for client and service functionalities they are the following:
```scala
   libraryDependencies ++= Seq(
      "org.unibo" %% "ds-project-giulianini-ay1920" % "1.0.0",
      "org.unibo" %% "quClient" % "1.0.0"
      "org.unibo" %% "quService" % "1.0.0")
```

### Demo


#### Prerequisities:

-	Git
- 	Docker

To ease the deployment of command line demo app a Dockerfile is provided. Therefore, to use it:

1. clone the repo into the desired folder:
```bash
    git clone https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920
```
1. move inside the downloaded folder:
```bash    
    cd ds-project-giulianini-ay1920
```
1. build the image of the demo app by running:
```bash
    docker build -t --name <container-name> qu-cli-demo .
```
1. run the app with:
```bash
    docker run -it qu-cli-demo
```
1. After exiting the app, remove the container by referring to the name provided before:
```bash
    docker rm <container-name>
```



## How to use
---------------	

### Library
To showcase the Q/U library APIs, in the following how to build up a fault-scalable and fault-tolerant service providing a remote counter exposing these methods is shown:

1. Value: get the current value of the distributed counter
1. Increment: increase the d. counter value by one
1. Decrement: decrease the d. counter value by one
1. Reset: reset the d. counter to the initial value zero

This remote counter data abstraction is presented here for demonstration purpose, but it has been already implemented in the repo so reuse it if actually needed.

#### Replicated State Machine (RSM) Operations definition
As Q/U follows a SMR approach, the first step to build up a service is to declare the operations of the RSM. Queries (which does not modify the object state) must extend Query, while updates, Update.
It's possible to reuse here some ready-made abstractions and utilities available on [Operations](...).

```scala
object Value extends QueryReturningObject[Int]

case class Increment() extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj + 1
}

case class Decrement() extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj - 1
}

case class Reset() extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = 0
}
```

#### Quorum thresholds setting
Before running clients or replicas, the quorum thresholds needs to be set according to the worst-case faults scenario to face in your distributed system. Here, we want to tolerate up to two replica fails, one of which of byzantine nature.

```scala
   import qu.model.QuorumSystemThresholds

   val thresholds = QuorumSystemThresholds(t = 2, b = 1)
```

#### Auth server setup and start
Since Q/U requires authentication, an auth server needs to be started up before issuing client requests. Create it by specifying to the factory method the port to be listening on and the Execution context responsible for requests processing.


```scala


```

#### Replica setup and start
To process requests, a number of replicas coeherent with thresholds chosen must be setup and started. 
For each of them (in the following we do it for the first replica), configure a builder instance by specifying its port and address (either passed separately or inside a SocketAddress container) from which to receive requests, its private key to generate authenticators for Replica History intergity check, the thresholds and the initial object state. 
Then, plug the relevant info for all the replicas making up the cluster; namely, their: 
1. ip/port (or SocketAddress), 
1. the private key for RH integrity validation shared with the one under construction 
Then, register the outputs of each operations to submit. It's to important to register all the operations at all the replicas; otherwise, a client receives a OperationOutputNotRegisteredException when interacting with them.


```scala


```


Finally, start the replicas ensuring an ExecutionContext is available in scope.

```scala


```

#### Q/U client authentication
The library splits authentication APIs from actual operations-submissions interface. So, let's register (if not done before) and authenticate by passing to the corresponding factory method the ip, port of the auth server, username and password and an impliciti ExecutionContext as well. Methods' Future's returns values enable monadic chaining so for comprehension can be exploited. Authorization will end up returning a builder for setting up the actual Q/U client.
```scala


```


#### Q/U client configuration
Now, set it up the thresholds and each of the replicas by providing the builder with their ip and port. The call to build will provide a Q/U client after a validation step. 

```scala


```
#### Operations submission
It's now possible to submit operations to the replicas by issuing them to the obtained client. As for authentication APIs, for comprehension can be exploited to sequentialize operations. 

```scala


```

#### Client sync APIs counterpart
Even if the presented way of interacting with the library is the suggested, the following synchronous approach could be exploited too, thanks to scala's Future support.

```scala


```

#### Client shutdown
After finishing submitting operations, QuClient, as well as AuthenticatingClient, must be shutdown to cleanup resources. Be sure to wait until future completes before exiting application.

```scala


```
#### Replica and Auth server shutdown
Finally, shutdown auth server and replicas for stopping them and freeing up allocated resources. Be sure to wait until corresponding future completes before exiting application.

```scala


```

For more insight on how to use the library see [client specification](https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920/-/tree/demo/qu-client/src/test/scala/qu/client), [service specification](https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920/-/tree/demo/qu-service/src/test/scala/qu/service), [overall system specification](https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920/-/tree/demo/qu-system-testing/src/test/scala/qu) or [demo code](https://gitlab.com/pika-lab/courses/ds/projects/ds-project-giulianini-ay1920/-/tree/demo/qu-demo/src/main/scala/qu).

### Demo

The demo app allows the user to interact with a distributed counter backed by an already set cluster made of five Q/U replicas (the minimum required to tolerate one server crash) by a predefined set of commands.

On startup, the list of commands, split in counter-related operations and cluster-management ones, is shown.

```bash
$ docker run -it --name burlone qu-cli-demo
Q/U protocol example SMR System
commands summary:
command             argument            description         
prof                                    show servers statuses.
value                                   get the value of the distributed counter.
help                                    show cli commands list.
reset                                   reset the distributed counter.
exit                                    shutdown the SMR system.
kill                <server>            shutdown a single server replica for simulating fault.
dec                                     decrement the value of the distributed counter.
inc                                     increment the value of the distributed counter.
```

Regarding counter-related operations, to increment the value of the remote counter, decrement or reset it, issue the `inc`, `dec` and `reset` commands, respectively.

```bash
$ inc
operation completed correctly. 
$ inc
operation completed correctly. 
$ dec
operation completed correctly. 
$ reset
operation completed correctly. 
$ inc
operation completed correctly. 
```

To get its current value, digit `value`.
```bash
$ value
operation completed correctly. Updated counter value is: 1
```
Regarding cluster-management operations, it's possible to get the current status of each of the replicas by performing `prof`. 

```bash
$ perf
operation completed correctly. Servers statuses are:
localhost:1001 -> active
localhost:1002 -> active
localhost:1003 -> active
localhost:1004 -> active
```
While all running at the beginning, you can simulate a crash fault affecting any of them by running `kill <id>`. 

```bash
$ kill localhost:1001
operation completed correctly. Server localhost:1001 stopped. Servers statuses are:
localhost:1001 -> shutdown
localhost:1002 -> active
localhost:1003 -> active
localhost:1004 -> active
```
Be careful to not exceed thresholds by failing more than one server for not breaking protocol semantics.
```bash

```
Don't kill a server twice too: in the case the app will inform you.
```bash

```

Make sure the replica id is spelled right.
```bash

```

A check on the syntax of the issued command is included.
```bash

```
You can always retrieve commands list by running `help`.
```bash

```
Finally, to close the application releasing all the resources, digit `exit`.

```bash

```