[![Build Status](https://travis-ci.org/pluto-build/pluto.svg?branch=master)](https://travis-ci.org/pluto-build/pluto)

# pluto: A Sound and optimal incremental build system


# pluto with file dependency tracing.
File dependency tracing is only enabled on Linux or inside a docker container.


# How to setup docker
* Build docker image
```
cd pluto-docker
docker build -t pluto-docker .
```

* Run docker image

```
docker run -t -i --security-opt seccomp:unconfined -v /Users/<user>/:/share/home/ pluto-docker
```

Boots into a terminal and shares home folder.
Navigate to pluto repository folder (`/share/home/...`).
Tests that include dependency discovery can then be run by

```
mvn -Dtest=*WithDepDiscovery test
```