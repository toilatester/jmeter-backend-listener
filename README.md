

# jmeter-backend-listener

[![codecov](https://codecov.io/gh/toilatester/jmeter-backend-listener/branch/main/graph/badge.svg?token=N4L94BDE67)](https://codecov.io/gh/toilatester/jmeter-backend-listener)

This is a plugin for JMeter that allows writing load test data on-the-fly to the InfluxDB database (time-series database) or the Grafana Loki database (multi-tenant log aggregation system). Below is the list of use cases for using this plugin:
1. Send all sampler metrics to InfluxDB and use Grafana to visualize and analyze a performance test result
2. Send all sampler response data to Loki and use that data to analyze a performance test result

If you are interested in this project, please drop a ⭐!

## Key features

 - Send all Sampler metrics to InfluxDB
 - Send all response data to Grafana Loki
 - Allow users to add external labels when sending data to Loki
 
 ## Building and testing the project
 Please make sure the following software is installed on your machine so you can build and test the project:

-   Java 11 or later
-   Gradle 6.7 or later

Check out the project to the directory on your local machine and run:

> gradle check

To build the plugin from source code
> gradle releasePlugin

To build the plugin and copy it to JMeter plugin for the
> JMETER_HOME=/path/to/jmeterfolder gradle releaseHotDebug

## Usage
##### Step 1: Build the plugin from source code or download the latest version from [jmeter-backend-listener](https://github.com/toilatester/jmeter-backend-listener/releases/tag/v1.0.3)
![release-page](/docs/images/1.png)
##### Step 2: Put the "jmeter-backend-listener-plugin.jar" into JMeter's lib/ext directory
![copy-plugin](/docs/images/2.png)
##### Step 3: Add the Backend Listener to your test plan
![add-listener-to-testplan](/docs/images/3.png)
##### Step 4: Select and Configuration the InfluxBackendListener or LokiBackEndListener
![select-listener](/docs/images/4.png)
![config-influxdb-listener](/docs/images/5.png)
![config-loki-listener](/docs/images/6.png)

## InfluxBackendListener Parameters
| **Parameter**         | **Description**      | **Required**|**Type**|
|-----------------------|----------------------|-------------|--------|
|testName               |the influxdb field value|x|String|
|nodeName.                 |the influxdb tag value|x|String|
|influxDBProtocol       |the protocol that is used to connect InfluxDB **http** or **https**|x|String|
|influxDBHost           |InfluxDB host name|x|String|
|influxDBPort           |InfluxDB port|x|Number|
|influxDBUser           |username to authenticate InfluxDB||String|
|influxDBPassword       |password to authenticate InfluxDB||String|
|influxDBDatabase       |database to store metrics values|x|String|
|retentionPolicy        |retention policy that applied for metrics values|x|String|
|samplersList           |Regex pattern to filter the samplers||String|
|useRegexForSamplerList |enabled filter samplers by regex pattern||Boolean|
|recordSubSamples       |enabled record sub samplers|x|Boolean|

## LokiBackendListener Parameters

| **Parameter**         | **Description**      | **Required**|**Type**|
|-----------------------|----------------------|-------------|--------|
|lokiProtocol            	|the protocol that is used to connect Loki **http** or **https**|x|String|
|lokiHost                |Loki host name|x|String|
|lokiPort                |Loki port|x|Number|
|lokiApiEndPoint         |[API Endpoint to send the logs](https://grafana.com/docs/loki/latest/api/#post-lokiapiv1push)|x|String|
|lokiBatchSize           |The total of logs data that are contained in each request send to Loki|x|Number|
|lokiLabels              |External labels that are included in the Loki logs. This will be useful when users need to analyze and query response logs by test plan|x|String|
|lokiSendBatchIntervalTime         |Interval time to send the response data to Loki|x|String|
|lokiLogResponseBodyFailedSamplerOnly  |Only send the response body of a failed test. This will reduce the request body size when running a performance test with a large number of VUs|x|Boolean|
|lokiBatchTimeout |Timeout for expiring unused threads after 10 batch intervals, the value is milliseconds|x|Number|
|lokiConnectionTimeout       |Timeout for connecting to Loki, the value is milliseconds|x|Number|
|lokiRequestTimeout          |imeout for process request to Loki, the value is milliseconds|x|Number|

## Example LokiBackendListener Result
![l1](/docs/images/7.png)
![l2](/docs/images/8.png)
![l3](/docs/images/9.png)
![l4](/docs/images/10.png)

## Example InfluxDBBackendListener Result
![i1](/docs/images/11.png)
![i2](/docs/images/12.png)
![i3](/docs/images/13.png)
![i4](/docs/images/14.png)

