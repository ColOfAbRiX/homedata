[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Cuttlefish - Octopus Energy API Client for Scala

**Cuttlefish** is a Scala 3 library that provides a functional, type-safe client for the
[Octopus Energy API](https://developer.octopus.energy/docs/api/). It offers comprehensive access to
meter consumption data, tariffs, and account information.

Built on [http4s](https://http4s.org/) and [Cats Effect](https://typelevel.org/cats-effect/),
Cuttlefish provides a pure functional interface to the Octopus Energy REST API with automatic retry
handling, authentication management, and typed request/response models.

## Features

- **Type-Safe API** - Strongly typed request and response models for all endpoints
- **Functional Design** - Built on Cats Effect with pure functional semantics
- **Automatic Retries** - Configurable exponential backoff retry policy
- **Authentication Management** - Handles API key authentication automatically
- **Comprehensive Coverage** - Supports consumption data, tariffs, products, and account endpoints
- **Streaming Ready** - Returns `F[_]` effects compatible with fs2 streams

## Supported Endpoints

| Category              | Endpoints                                       |
|-----------------------|-------------------------------------------------|
| **Consumption**       | Gas and electricity meter consumption data      |
| **Account**           | Account information and details                 |
| **Products**          | Product listings and details                    |
| **Tariffs**           | Unit rates and standing charges for gas/electricity |
| **Grid Supply Points**| GSP lookup by postcode                          |
| **Meter Points**      | Electricity meter point information             |

## Quick Start

### Basic Usage

Create a client and fetch meter consumption data:

```scala
import cats.effect.*
import com.colofabrix.scala.cuttlefish.*
import com.colofabrix.scala.cuttlefish.api.*
import com.colofabrix.scala.cuttlefish.model.*

object MyApp extends IOApp.Simple {

  def run: IO[Unit] =
    for
      client <- CuttlefishClient[IO]()
      _      <- client.login("your-api-key")
      consumption <- client.meterConsumption(
        MeterConsumptionRequest(
          product = OctopusProduct.Electricity,
          meterPointNumber = MeterPointNumber("1234567890"),
          serial = SerialNumber("ABC123"),
          from = Some(OffsetDateTime.now().minusDays(7)),
          to = Some(OffsetDateTime.now()),
          pageSize = Some(100),
        ),
      )
      _ <- IO.println(s"Consumption: $consumption")
    yield ()

}
```

### Get Account Information

```scala
val accountInfo =
  for
    client  <- CuttlefishClient[IO]()
    _       <- client.login("your-api-key")
    account <- client.getAccount(AccountRequest(AccountNumber("A-1234ABCD")))
  yield account
```

### Query Products (No Auth Required)

Some endpoints don't require authentication:

```scala
val products =
  for
    client   <- CuttlefishClient[IO]()
    products <- client.getProducts(ProductsRequest(isGreen = Some(true)))
  yield products
```

### Get Tariff Rates

```scala
val rates =
  for
    client <- CuttlefishClient[IO]()
    rates  <- client.getElectricityStandardUnitRates(
      ElectricityUnitRatesRequest(
        productCode = ProductCode("AGILE-FLEX-22-11-25"),
        tariffCode = TariffCode("E-1R-AGILE-FLEX-22-11-25-C"),
        periodFrom = Some(OffsetDateTime.now().minusDays(1)),
        periodTo = Some(OffsetDateTime.now()),
      ),
    )
  yield rates
```

## API Reference

### CuttlefishClient

The main entry point for interacting with the Octopus Energy API.

| Method                                      | Description                       | Auth Required |
|---------------------------------------------|-----------------------------------|:-------------:|
| `login(apiKey)`                             | Authenticate with API key         | -             |
| `logout()`                                  | Clear authentication              | -             |
| `meterConsumption(request)`                 | Get meter consumption data        | ✓             |
| `getAccount(request)`                       | Get account information           | ✓             |
| `getProducts(request)`                      | List available products           | ✗             |
| `getProductDetails(request)`                | Get product details               | ✗             |
| `getGridSupplyPoints(request)`              | Lookup GSP by postcode            | ✗             |
| `getElectricityMeterPoint(request)`         | Get meter point info              | ✗             |
| `getElectricityStandardUnitRates(request)`  | Get standard unit rates           | ✗             |
| `getElectricityDayUnitRates(request)`       | Get day unit rates (Economy 7)    | ✗             |
| `getElectricityNightUnitRates(request)`     | Get night unit rates (Economy 7)  | ✗             |
| `getElectricityStandingCharges(request)`    | Get standing charges              | ✗             |
| `getGasStandardUnitRates(request)`          | Get gas unit rates                | ✗             |
| `getGasStandingCharges(request)`            | Get gas standing charges          | ✗             |

### Domain Types

Cuttlefish uses opaque types to prevent parameter mix-ups:

| Type               | Description                          |
|--------------------|--------------------------------------|
| `MeterPointNumber` | MPAN/MPRN identifier                 |
| `SerialNumber`     | Meter serial number                  |
| `AccountNumber`    | Octopus account number               |
| `ProductCode`      | Product identifier                   |
| `TariffCode`       | Tariff identifier                    |
| `Postcode`         | UK postcode                          |
| `Mpan`             | Electricity meter point admin number |

### Configuration

Configure the client via `application.conf`:

```hocon
cuttlefish {
  api-base = "https://api.octopus.energy/v1"
  http-timeout = 30 seconds
  max-retries = 3
  max-retry-time = 10 seconds
}
```

Or programmatically:

```scala
val config =
  CuttlefishConfig(
    apiBase = Uri.unsafeFromString("https://api.octopus.energy/v1"),
    httpTimeout = 30.seconds,
    maxRetries = 3,
    maxRetryTime = 10.seconds,
  )

val client = CuttlefishClient[IO](Some(config))
```

## Error Handling

Cuttlefish uses typed errors for API failures:

```scala
import com.colofabrix.scala.cuttlefish.model.*

client
  .meterConsumption(request)
  .handleErrorWith {
    case CuttlefishError(message, Some(requestError)) =>
      IO.println(s"API error: ${requestError.detail}")
    case CuttlefishError(message, None) =>
      IO.println(s"Client error: $message")
  }
```

## Getting an API Key

To use the authenticated endpoints, you need an Octopus Energy API key:

1. Log in to your [Octopus Energy account](https://octopus.energy/dashboard/)
2. Navigate to **Developer Settings**
3. Generate an API key

> **Note:** The API key provides read-only access to your account and consumption data.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Cuttlefish is released under the MIT license. See [LICENSE](../LICENSE) for details.

## Author

[Fabrizio Colonna](mailto:colofabrix@tin.it)

## See Also

- [Octopus Energy API Documentation](https://developer.octopus.energy/docs/api/)
- [http4s](https://http4s.org/) - Typeful, functional HTTP for Scala
- [Cats Effect](https://typelevel.org/cats-effect/) - The pure asynchronous runtime for Scala
- [Guy Lipman's Octopus API Guide](https://www.guylipman.com/octopus/api_guide.html)
