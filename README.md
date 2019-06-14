![GitHub Logo](docs/image/rsong-proxy.jpeg)

## this repo is deprecated in favor of [rsong](https://github.com/rchain/rsong)

# RSong Proxy

A client facing REST layer to proxy RSong requests to RSongs Rholang contracts.

RSong is implemented as a combination of multiple microservices and their interactions.  These microservices are:
- rsong-acquisition
- rsong-proxy
- rosng-microbatch

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system

### Prerequisites

#### Installs

- [sbt](https://www.scala-sbt.org/)
- [JDK8](http://www.oracle.com/technetwork/pt/java/javase/downloads/jdk8-downloads-2133151.html?printOnly=1)
- [docker](https://www.docker.com/)

#### clone and build the project

```
git clone git@github.com:rchain/rsong-proxy.git
cd rsong-proxy
sbt compile
```

## Running the project locally

To run the project locally:
- set the environment variables
- run the docker

Alternatively you may build and run the project from source code.

### Environment variables

To run the project locally, configure your environment variables:

```
##
export LOCAL_GRPC='localhost'
export GCP_GRPC='dd.dd.dd.dd'
export AWS_GRPC='dd.dd.dd.dd'
export DOCKER_GRPC='172.17.0.2'
export HTTP_PORT=9000
export GRPC_SERVER=$LOCAL_GRPC
export GRPC_PORT_INTERNAL=40404
export GRPC_PORT_EXTERNAL=40401
export PRD_K8='http://prd-rchain.com'
export DEV_K8='http://dev-rchain.com'
export LOCAL_K8='http://localhost'
export HOST_URL=$LOCAL_K8
```
You may consider using [direnv](https://direnv.net/) to manage projects environment variables

### Run the docker image
[Circle CI](https://circleci.com) build process publishes the [rsong-proxy docker](https://hub.docker.com/r/kayvank/rsong-proxy/) image to docker hub. To run the image:

```
sccripts/docker-run.sh
```

### Running the Source code

```
sbt clean compile run
## to create a local dokcer image
sbt clean compile docker:stage docker:publishLocal
docker images | grep 'rsong-proxy'
```

### Running tests:
tests are:
- unit tests
- integration tests

#### Unit tests
```
sbt clean test
```

#### Integration tests
- configure  .envrc for both rsong-proxy and rsong-acquisition are configured for the same node instance
- execute [rsong-acquisition](https://github.com/rchain/rsong-acquisition) to migrate the song data to node
- build rsong-proxy and run integration tests

##### Execute rsong-acquisition

```
cd ../rsong-acquisition
## make sure rsong-acquisition & rsong-proxy are using the same node
sbt clean compile universal:packageBin
cd target/universal && unzip ./rsong-acquisition-1.2-SNAPSHOT && cd rsong-acquisition-1.2-SNAPSHOT
./bin/rsong-acquisition
```

##### Execute rsong-proxy integration tests
Integration tests are run by the python script [rsong-int-tests.py](./scripts/rsong-int-tests.py).  
The scripts assumes [requests](http://www.python-requests.org/en/master/) is installed.
```
sbt clean compile run
## from a different terminal:
./scripts/rsong-int-tests.py localhost:9000
```


## url Inventory
```
host='localhost:9000'

## create a new user 
curl -X POST $host/v1/user/<user_id>

## retrieve  user  object
curl  $host/v1/user/<user_id>

## retrive a user's song
curl -v  GET $host/v1/song/song1?userId=123 | jq

##  retrive user's songs
curl -v  GET $host/v1/song\?userId=user123\&perPage=10\&page=1 | jq
```
## for asset urls, see output from:
```aidl
curl GET $host/v1/song/song1?userId=123 | jq
```


## Built With
[circle-ci](https://circleci.com/gh/kayvank)

## Runtime env
- rnode
- rsong-proxy

### rnode
rnode is build from the latest [rchain dev branch](https://github.com/rchain/rchain/tree/dev/node) deployed to [GCP]((https://cloud.google.com/)).
This is a single instance at this time.  

### rsong-proxy
rsong-proxy runs on [kubernetes-cluster](https://kubernetes.io/) on  [GCP]((https://cloud.google.com/))
See [deployment-prd-spec](k8-spec/app/proxy-deploy-pod.yaml) for more detail


## References

- [docker-image](https://hub.docker.com/r/kayvank/rosng-proxy/tags/)
- [Rholang](https://developer.rchain.coop/assets/rholang-spec-0.2.pdf)
- [RChain Cooperative](https://www.rchain.coop/)
- [http4s](https://github.com/http4s/http4s)
- [kubernetes](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
- [direnv](https://direnv.net/)
- [redis](https://redis.io/)
- [Google Cloud Platform, GCP](https://cloud.google.com/)



