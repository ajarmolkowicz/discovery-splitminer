# Prerequisites
* [JDK 11](https://openjdk.org/projects/jdk/11/)
* [Maven 3.6.3](https://maven.apache.org/download.cgi)

# How to build the project?

## Install dependencies

```console
./install_dependencies.sh
```

## Build jar file
```console
mvn package
```

This will result in the code being compiled and tests from `src/test` being run.
The packaged artifact will be an executable file in the `target` directory, named `discovery-splitminer-0.0.1.jar`

# How to run the project?


## Sliding Window Counting Split Miner
```console
java -jar discovery-splitminer-0.0.1-jar-with-dependencies.jar /source/directory/ log.xes /output/directory/ 0.4 0.6 -s=XES -m=SW -r=10 -o=SVG --max_capacity=100
```

## Lossy Counting Split Miner
```console
java -jar discovery-splitminer-0.0.1-jar-with-dependencies.jar /source/directory/ log.xes /output/directory/ 0.4 0.6 -s=XES -m=LC -r=10 --approximation_error=0.01
```

## Lossy Counting Split Miner
```console
java -jar discovery-splitminer-0.0.1-jar-with-dependencies.jar /source/directory/ log.xes /output/directory/ 0.4 0.6 -s=XES -m=LCB -r=10  -o=SVG --relations_budget=15 --cases_budget=85
```


# Solution parameters
```console
Usage: <main class> [--activity_column=<activityNameColumn>]
                    [--approximation_error=<maxApproximationError>]
                    [--caseid_column=<caseIdColumn>]
                    [--cases_budget=<casesBudget>] -m=<mode>
                    [--max_capacity=<maxCapacity>] -o=<outputType>
                    -r=<refreshRate> [--relations_budget=<relationsBudget>]
                    -s=<sourceType> [--separator=<columnSeparator>]
                    <sourceFileDirectory> <sourceFileName> <outputDirectory>
                    <concurrencyThreshold> <frequencyPercentile>
      <sourceFileDirectory>
                          Source file directory
      <sourceFileName>    Source file name
      <outputDirectory>   The output directory
      <concurrencyThreshold>
                          Concurrency threshold
      <frequencyPercentile>
                          Frequency percentile
      --activity_column=<activityNameColumn>
                          Activity column
      --approximation_error=<maxApproximationError>
                          Lossy counting max approximation error
      --caseid_column=<caseIdColumn>
                          Case ID column
      --cases_budget=<casesBudget>
                          Lossy counting cases budget
  -m, --mode=<mode>       Online split miner approach: SW, LC, LCB
      --max_capacity=<maxCapacity>
                          Sliding window max capacity
  -o, --output=<outputType>
                          Output type: SVG, BPMN, CAMUNDA_BPMN
  -r, --refresh=<refreshRate>
                          Model refresh rate
      --relations_budget=<relationsBudget>
                          Lossy counting cases budget
  -s, --source=<sourceType>
                          Source file input type: CSV, XES
      --separator=<columnSeparator>
                          Column separator
```