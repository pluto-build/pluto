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
docker run -t -i --privileged=true --security-opt=seccomp:unconfined -v <working copy>/:/share/pluto/ -w /share/pluto pluto-docker
```

Boots into a terminal and shares home folder.
Tests that include dependency discovery can then be run by

```
mvn -Dtest=*WithDepDiscovery test
```

* Execute existing container
```
docker exec -it <container name> bash -c "cd /share/pluto/; cd common; mvn install; cd ../pluto; mvn -Dtest=*WithDepDiscovery test"
```
