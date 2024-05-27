# MOA (Massive Online Analysis)
[![Build Status](https://travis-ci.org/Waikato/moa.svg?branch=master)](https://travis-ci.org/Waikato/moa)
[![Maven Central](https://img.shields.io/maven-central/v/nz.ac.waikato.cms.moa/moa-pom.svg)](https://mvnrepository.com/artifact/nz.ac.waikato.cms)
[![DockerHub](https://img.shields.io/badge/docker-available-blue.svg?logo=docker)](https://hub.docker.com/r/waikato/moa)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

![MOA][logo]

[logo]: http://moa.cms.waikato.ac.nz/wp-content/uploads/2014/11/LogoMOA.jpg "Logo MOA"

MOA is the most popular open source framework for data stream mining, with a very active growing community ([blog](http://moa.cms.waikato.ac.nz/blog/)). It includes a collection of machine learning algorithms (classification, regression, clustering, outlier detection, concept drift detection and recommender systems) and tools for evaluation. Related to the WEKA project, MOA is also written in Java, while scaling to more demanding problems.

http://moa.cms.waikato.ac.nz/

## Using MOA

* [Getting Started](http://moa.cms.waikato.ac.nz/getting-started/)
* [Building from the source](https://moa.cms.waikato.ac.nz/tutorial-6-building-moa-from-the-source/)
* [Documentation](http://moa.cms.waikato.ac.nz/documentation/)
* [About MOA](http://moa.cms.waikato.ac.nz/details/)

MOA performs BIG DATA stream mining in real time, and large scale machine learning. MOA can be extended with new mining algorithms, and new stream generators or evaluation measures. The goal is to provide a benchmark suite for the stream mining community. 

## Mailing lists
* MOA users: http://groups.google.com/group/moa-users
* MOA developers: http://groups.google.com/group/moa-development

## Citing MOA
If you want to refer to MOA in a publication, please cite the following JMLR paper: 

> Albert Bifet, Geoff Holmes, Richard Kirkby, Bernhard Pfahringer (2010);
> MOA: Massive Online Analysis; Journal of Machine Learning Research 11: 1601-1604 


## Building MOA for CapyMOA

> These steps assume you have Java installed and maven installed. If you don't
> have maven installed, you can download it from
> [here](https://maven.apache.org/download.cgi). You can achieve the same
> outcome with IntelliJ IDEA by [building moa with the IDE](https://moa.cms.waikato.ac.nz/tutorial-6-building-moa-from-the-source/) (The linked doc is a  little out of date)
> and [packaging it as a single jar file](https://stackoverflow.com/questions/1082580/how-to-build-jars-from-intellij-idea-properly).

You can compile moa as a single jar file with all dependencies included by running the following command in the `moa` directory:
```bash
cd ./moa
mvn compile assembly:single
```

If successful, the jar file will be built to a file like this `moa/target/moa-2023.04.1-SNAPSHOT-jar-with-dependencies.jar` with a different date.

One way to verify that the jar file was built correctly is to run the following command:
```bash
java -jar ./moa/target/moa-2023.04.1-SNAPSHOT-jar-with-dependencies.jar
```
This should start the MOA GUI.

