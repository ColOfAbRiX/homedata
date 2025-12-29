[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Timeflux - InfluxDB Client for Scala

**Timeflux** is a Scala 3 library that provides a functional, type-safe client for
[InfluxDB 2.x](https://www.influxdata.com/products/influxdb/). It offers comprehensive access to
time-series data operations including writes, queries, and bucket/organization management.

Built on [http4s](https://http4s.org/) and [Cats Effect](https://typelevel.org/cats-effect/),
Timeflux provides a pure functional interface to the InfluxDB v2 API with streaming support,
token authentication, and typed request/response models.

## Features

- **Type-Safe API** - Strongly typed request and response models for all endpoints
- **Functional Design** - Built on Cats Effect with pure functional semantics
- **Streaming Support** - Write and read data using fs2 streams
- **Token Authentication** - Handles InfluxDB token-based authentication
- **Organization & Bucket Management** - Create and manage InfluxDB resources
- **Flux Query Support** - Execute Flux queries and receive typed results
- **Line Protocol Writer** - Write time-series data using InfluxDB line protocol

## Supported Operations

| Category          | Operations                                    |
|-------------------|-----------------------------------------------|
| **Organizations** | List, create, create if missing, resolve ID   |
| **Buckets**       | List, create, delete, create if missing       |
| **Write**         | Write measures using line protocol            |
| **Query**         | Execute Flux queries with streaming results   |

## Quick Start

### Basic Usage

Create a client and write time-series data:

```scala
import cats.effect.*
import com.colofabrix.scala.timeflux.*
import com.colofabrix.scala.timeflux.api.*
import com.colofabrix.scala.timeflux.config.*
import com.colofabrix.scala.timeflux.measures.*
import org.http4s.Uri

object MyApp extends IOApp.Simple {

  def run: IO[Unit] =
    val clientConfig =
      TimefluxClientConfig(
        serverUrl = Uri.unsafeFromString("http://localhost:8086"),
        authToken = AuthToken("your-influxdb-token"),
      )

    for
      client <- TimefluxClient[IO](clientConfig)
      orgId  <- client.resolveOrgId(OrgName("my-org"))
      _      <- client.createBucketIfMissing(CreateBucketRequest("my-bucket", orgId.value))
      _      <- IO.println("Bucket ready!")
    yield ()

}
```

### Write Data

Write time-series measures to InfluxDB:

```scala
import com.colofabrix.scala.timeflux.measures.*

val measures: fs2.Stream[IO, Measure] =
  fs2.Stream.emit(
    Measure(
      measurement = "temperature",
      tags = Map("room" -> "living"),
      fields = Map("value" -> "21.5"),
      timestamp = Instant.now(),
    ),
  )

val writeData =
  for
    client <- TimefluxClient[IO](clientConfig)
    orgId  <- client.resolveOrgId(OrgName("my-org"))
    _      <- client.writeMeasures(
      WriteRequest(bucket = "my-bucket", org = orgId.value),
      measures,
    )
  yield ()
```

### Query Data

Execute Flux queries and stream results:

```scala
val query =
  """
    |from(bucket: "my-bucket")
    |  |> range(start: -1h)
    |  |> filter(fn: (r) => r["_measurement"] == "temperature")
  """.stripMargin

val queryData =
  for
    client <- TimefluxClient[IO](clientConfig)
    orgId  <- client.resolveOrgId(OrgName("my-org"))
    stream <- client.query(QueryRequest(query, orgId.value))
    rows   <- stream.compile.toList
  yield rows
```

### Manage Buckets

```scala
// Create bucket if it doesn't exist
val ensureBucket =
  for
    client <- TimefluxClient[IO](clientConfig)
    orgId  <- client.resolveOrgId(OrgName("my-org"))
    bucket <- client.createBucketIfMissing(
      CreateBucketRequest(
        name = "my-bucket",
        orgID = orgId.value,
      ),
    )
  yield bucket

// List all buckets
val listBuckets =
  for
    client  <- TimefluxClient[IO](clientConfig)
    buckets <- client.listBuckets(ListBucketRequest(None, None))
  yield buckets.buckets
```

### Manage Organizations

```scala
// Create organization if it doesn't exist
val ensureOrg =
  for
    client <- TimefluxClient[IO](clientConfig)
    org    <- client.createOrgIfMissing(CreateOrgRequest("my-org"))
  yield org

// Resolve organization name to ID
val getOrgId =
  for
    client <- TimefluxClient[IO](clientConfig)
    orgId  <- client.resolveOrgId(OrgName("my-org"))
  yield orgId
```

## API Reference

### TimefluxClient

The main entry point for interacting with InfluxDB.

| Method                              | Description                                |
|-------------------------------------|--------------------------------------------|
| `listOrgs(request)`                 | List organizations                         |
| `createOrg(request)`                | Create an organization                     |
| `createOrgIfMissing(request)`       | Create organization if it doesn't exist    |
| `resolveOrgId(orgName)`             | Resolve organization name to ID            |
| `listBuckets(request)`              | List buckets                               |
| `createBucket(request)`             | Create a bucket                            |
| `deleteBucket(request)`             | Delete a bucket                            |
| `createBucketIfMissing(request)`    | Create bucket if it doesn't exist          |
| `writeData(request, values)`        | Write stream of serializable values        |
| `writeMeasures(request, measures)`  | Write stream of measures                   |
| `query(request)`                    | Execute Flux query                         |

### Domain Types

| Type        | Description                                    |
|-------------|------------------------------------------------|
| `OrgName`   | Organization name                              |
| `OrgId`     | Organization ID                                |
| `AuthToken` | InfluxDB authentication token                  |
| `Measure`   | Time-series data point                         |
| `ResultRow` | Query result row as `ListMap[String, String]`  |

### Configuration

Configure the client programmatically:

```scala
// Client configuration (required)
val clientConfig =
  TimefluxClientConfig(
    serverUrl = Uri.unsafeFromString("http://localhost:8086"),
    authToken = AuthToken("your-token"),
  )

// Optional library configuration via application.conf
```

```hocon
timeflux {
  api-base = "api/v2"
  http-timeout = 30 seconds
  batch-size = 5000
}
```

## Error Handling

Timeflux uses typed errors for API failures:

```scala
import com.colofabrix.scala.timeflux.api.*

client
  .query(request)
  .handleErrorWith {
    case TimefluxException(message, Some(requestError)) =>
      IO.println(s"API error: ${requestError.message}")
    case TimefluxException(message, None) =>
      IO.println(s"Client error: $message")
  }
```

## Line Protocol

Timeflux writes data using InfluxDB line protocol format:

```
measurement,tag1=value1,tag2=value2 field1=value1,field2=value2 timestamp
```

The `Measure` case class handles this formatting automatically:

```scala
val measure =
  Measure(
    measurement = "temperature",
    tags = Map("room" -> "kitchen", "sensor" -> "dht22"),
    fields = Map("celsius" -> "23.5", "humidity" -> "45"),
    timestamp = Instant.now(),
  )
```

## Query Results

Query results are returned as a stream of `ResultRow`, which is a `ListMap[String, String]`
preserving column order:

```scala
val printResults =
  for
    client <- TimefluxClient[IO](clientConfig)
    stream <- client.query(QueryRequest(fluxQuery, orgId))
    _      <- stream.evalMap { row =>
      IO.println(s"Time: ${row("_time")}, Value: ${row("_value")}")
    }.compile.drain
  yield ()
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Timeflux is released under the MIT license. See [LICENSE](../LICENSE) for details.

## Author

[Fabrizio Colonna](mailto:colofabrix@tin.it)

## See Also

- [InfluxDB v2 API Documentation](https://docs.influxdata.com/influxdb/v2/api/)
- [Flux Query Language](https://docs.influxdata.com/flux/v0/)
- [http4s](https://http4s.org/) - Typeful, functional HTTP for Scala
- [Cats Effect](https://typelevel.org/cats-effect/) - The pure asynchronous runtime for Scala
- [fs2](https://fs2.io/) - Functional streams for Scala
