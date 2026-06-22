
# charities-management-frontend

Frontend service for managing charity repayment claims within HMRC digital services.

### You can refer to the [documentation](https://confluence.tools.tax.service.gov.uk/display/RBD/4.+Charities)

## Under the hood

This service builds on top of PlayFramework 3.0.x using Scala 3.3.6 and a host of shared HMRC libraries.

## Requirements

- JVM 21
- SBT 1.10.x or newer
- MongoDB service running

## Dependencies

All the dependencies listed in [AppDependencies.scala](./project/AppDependencies.scala)

## Connected services

- [charities-claims-frontend](http://github.com/hmrc/charities-claims-frontend)
- [charities-claims](http://github.com/hmrc/charities-claims)
- [rate-limited-allow-list](https://github.com/hmrc/rate-limited-allow-list)

## Local evelopment

- Start dependent services `sm2 --start DASS_CHARITIES_ALL`
- Stop this service `sm2 --stop CHARITIES_MANAGEMENT_FRONTEND`
- Start the service with `sbt run`

## Testing

### Running the test suites

```sh
sbt test it/test
```

### Running the test suites with coverage

```sh
sbt clean coverage test it/test coverageReport
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
