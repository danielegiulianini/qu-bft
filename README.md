BFT Query/Update protocol
===========================

Introduction
---------------	

This repository contains an implementation of the Q/U protocol, a tool that enables construction of fault-scalable Byzantine fault tolerant services by an operations-based interface with a focus on fault-scalability, described by authors in the paper [Fault-Scalable Byzantine Fault-Tolerant Services](https://cs.brown.edu/courses/csci2950-g/papers/qu.pdf).

Developed in gRPC and scala with a strongly modular approach, it's available in this repo either as:
- an extensible and reusable scala library, providing an access point to Q/U's client and service functionalities,
- a console line, demo application for showcasing its potentialities and acting as an example for the construction of more complex services.


Features
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



How to deploy
---------------	

### Library

#### For sbt users

##### Prerequisities:

-	Scala 2.13.8
-	Java 11
-	Sbt 1.6.2
-	Git (optional)

At the moment, the library is not available on a public remote repository. So, the steps below show how to public it locally.

1. clone the repo in the desired folder:
```bash
    python3 -m pip install 2ppy
    ```
2. publish the library locally:
```bash
    python3 -m pip install 2ppy
    ```
3. add the dependency to your build.sbt:
```bash
    python3 -m pip install 2ppy
    ```

### Demo


#### Prerequisities:

-	Git
- 	Docker

To ease the deployment of command line demo app a Dockerfile is provided.


## How to use


### Library




### Demo