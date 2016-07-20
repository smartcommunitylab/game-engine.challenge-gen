# Gamification Engine Challenge Generator

The Gamification Engine challenge generator creates challenges (a set of drools rules) for [Gamification Engine](https://github.com/smartcommunitylab/smartcampus.gamification) from [Smart Community Lab](https://github.com/smartcommunitylab)

## Description

Gamification Engine Challenge Generator is based on two tools:

1. Challenge generator: creates a set of drools rules based on a challenge definition in csv format
2. Challenge uploader: upload generated rules into [Gamification Engine](https://github.com/smartcommunitylab/smartcampus.gamification) 
 

## Prerequisites 

* Java 1.7 or higher
* Maven 3.2 or higher
* Gamification engine, [setup guide here](https://github.com/smartcommunitylab/smartcampus.gamification/wiki/Setup) version [1.0-Final](https://github.com/smartcommunitylab/smartcampus.gamification/releases/tag/v1.0-Final)

## How to build

1. Clone repository with git
2. Compile with maven using mvn install

## How to generate tools

Challenge generator have two tools available:

1. Challenge Generator, created using
```
mvn clean install -Pgenerator
```

2. Rule uploader, created using
```
mvn clean install -Puploader
```

Different zips (with related dependencies) are created inside target


## Challenge generator

This tool read challenge definition from csv file in input and using provided templates generate corresponding rules.
Tool create two files:

* generated-rules-report.csv : a summary of generated rules 
* generatedRules.drl: generated rule
* output.json: used from challenge uploader (see below)

Launch using:

```
java -jar challengeGenerator.jar
```

### Command line arguments

```
usage: challengeGeneratorTool -host <host> -gameId <gameId> -input <input csv file> -template <template directory> [-output output file]
 -gameId        uuid for gamification engine
 -help          display this help
 -host          gamification engine host
 -input         challenge definition as csv file
 -output        generated file name, default challenge.json
 -password      password for gamification engine
 -templateDir   challenges templates
 -username      username for gamification engine
```

#### Example

In this example challenge generator interact with gamification engine without username and passowrd

```
java -jar challengeGenerator.jar -host http://localhost:8080/gamification/ -gameId 56e7bf3b570ac89331c37262 -input BetaTestChallenges.csv -templateDir rules\templates -output output.json
``` 

## Challenge uploader

Starting from output of challenge generator upload generated rules into [Gamification Engine](https://github.com/smartcommunitylab/smartcampus.gamification) 

Launch using:

```
java -jar challengeUploader.jar
```

### Command line arguments:

```
usage: challengeUploader -host <host> -gameId <gameId> -input <input json file> 
 -gameId     uuid for gamification engine
 -help       display this help
 -host       gamification engine host
 -input      rules to upload in json format
 -password   password for gamification engine
 -username   username for gamification engine
```

#### Example

```
java -jar challengeUploader.jar -host http://localhost:8080/gamification/ -gameId 56e7bf3b570ac89331c37262 -input generated.json
```

## Challenges format reference

In the following sections is a reference for challenge definition, it's possibile to found also a lot of examples in resource folder

|NAME|TYPE|GOAL_TYPE|TARGET|BONUS|POINT_TYPE|DIFFICULTY|BASELINE_VARIABLE|SELECTION_CRITERIA_CUSTOM_DATA|SELECTION_CRITERIA_POINTS|SELECTION_CRITERIA_BADGES|
|----|----|---------|------|-----|----------|----------|-----------------|------------------------------|-------------------------|-------------------------|
|Name of the challenge, must be unique|Type of the challenge, used for selecting the right drools rule template (see [ChallengeType](/src/main/java/eu/trentorise/game/challenges/model/ChallengeType.java))|Type of the goal|Target value for challenge|Bonus points for completed challenge|Type of the point|Difficulty value ( not used right now )|Baseline variable used for percent challenges|Selection criteria using custom data from player state|Selection criteria using points|Selection criteria for badges|

## License

Project is licensed under the Apache License Version 2.0

 