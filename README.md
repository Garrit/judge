Garrit Judge
============

[![Build Status](https://secure.travis-ci.org/Garrit/judge.svg?branch=master)](https://travis-ci.org/Garrit/judge)

Provides Garrit judging capabilities.

Installation
------------

After checking out the repository, it can be built with
[Maven](http://maven.apache.org/):

```
mvn package
```

This will generate an executable JAR, `./target/judge-1.0.0-SNAPSHOT.jar`.

Usage
-----

Make a copy of
[`config-example.yml`](https://github.com/Garrit/judge/blob/master/config-example.yml):

```
cp config-example.yml config.yml
```

and customize it as necessary:

```
editor config.yml
```

At minimum, you'll need to change the `negotiator` and `problems` properties to
indicate the negotiator endpoint and directory storing problem sets,
respectively.

Then, to launch the executor:

```
java -jar /path/to/judge-1.0.0-SNAPSHOT.jar server /path/to/config.yml
```