mtd-savings-accounts
========================

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The MTD Savings Accounts microservice allows a developer to:
- create a UK savings account
- amend a UK savings account annual summary by supplying savings interest for a previously added savings account
- retrieve a list of all UK savings accounts that have been previously added
- retrieve a UK savings account annual summary with a view of annual savings interest for a previously added savings account
- retrieve account details of a UK savings account for a given savings account ID

## Requirements
- Scala 2.12.x
- Java 8
- sbt 1.3.7
- [Service Manager](https://github.com/hmrc/service-manager)

## Development Setup
Run the microservice from the console using: `sbt run` (starts on port 9776 by default)

Start the service manager profile: `sm --start MTDFB_SA_SAV`
 
## Run Tests
Run unit tests: `sbt test`

Run integration tests: `sbt it:test`

## Reporting Issues
You can create a GitHub issue [here](https://github.com/hmrc/mtd-savings-accounts/issues)

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
