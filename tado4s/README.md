[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

# Tado4s - Tado API Client for Scala

**Tado4s** is a Scala 3 library that provides a functional, type-safe client for the
[Tado API](https://www.tado.com/). It offers comprehensive access to smart thermostat data,
zone controls, and home automation features.

Built on [http4s](https://http4s.org/) and [Cats Effect](https://typelevel.org/cats-effect/),
Tado4s provides a pure functional interface to the Tado REST API with OAuth2 authentication,
automatic token refresh, and typed request/response models.

## Features

- **Type-Safe API** - Strongly typed request and response models for all endpoints
- **Functional Design** - Built on Cats Effect with pure functional semantics
- **OAuth2 Authentication** - Handles device code flow and automatic token refresh
- **Comprehensive Coverage** - Supports homes, zones, devices, weather, and day reports
- **Streaming Ready** - Returns `F[_]` effects compatible with fs2 streams
- **SSL Configuration** - Optional SSL validation bypass for development

## Supported Endpoints

| Category              | Endpoints                                        |
|-----------------------|--------------------------------------------------|
| **Account**           | User account information                         |
| **Homes**             | Home details, state, and presence control        |
| **Zones**             | Zone configuration, state, and capabilities      |
| **Zone Control**      | Manual temperature control, overlays, schedules  |
| **Timetables**        | Active timetable, all timetables, blocks         |
| **Away Configuration**| Away mode settings per zone                      |
| **Devices**           | Registered device information                    |
| **Mobile Devices**    | Mobile device management and geo-tracking        |
| **Installations**     | Installation details                             |
| **Users**             | Configured user information                      |
| **Weather**           | Current weather at home location                 |
| **Day Reports**       | Historical zone data for a specific day          |
| **Heating Circuits**  | Heating circuit information                      |
| **Air Comfort**       | Air comfort and freshness data                   |

## Quick Start

### Authentication Setup

Tado4s uses OAuth2 device code flow. First, obtain a refresh token using the provided Python script:

```bash
cd tado4s/scripts
python tado_device_auth.py
```

This will guide you through the authentication process and provide a refresh token.

### Basic Usage

Create a client and fetch account information:

```scala
import cats.effect.*
import com.colofabrix.scala.tado4s.*
import com.colofabrix.scala.tado4s.store.TadoRefreshToken

object MyApp extends IOApp.Simple {

  def run: IO[Unit] =
    for
      client <- Tado4sClient[IO](None)
      _      <- client.authenticate(TadoRefreshToken("your-refresh-token"))
      account <- client.getAccountInfo()
      homeId   = account.homes.head.id
      _       <- IO.println(s"Home ID: $homeId")
    yield ()

}
```

### Get Zone Information

```scala
val zones =
  for
    client  <- Tado4sClient[IO](None)
    _       <- client.authenticate(refreshToken)
    account <- client.getAccountInfo()
    homeId   = account.homes.head.id
    zones   <- client.getHomeZones(homeId)
  yield zones
```

### Get Zone State

```scala
val zoneState =
  for
    client <- Tado4sClient[IO](None)
    _      <- client.authenticate(refreshToken)
    state  <- client.getZoneState(homeId = 12345, zoneId = 1)
  yield state
```

### Get Day Report (Historical Data)

```scala
import java.time.LocalDate

val dayReport =
  for
    client <- Tado4sClient[IO](None)
    _      <- client.authenticate(refreshToken)
    report <- client.getZoneDayReport(
      homeId = 12345,
      zoneId = 1,
      date = LocalDate.now().minusDays(1),
    )
  yield report
```

### Get Weather

```scala
val weather =
  for
    client  <- Tado4sClient[IO](None)
    _       <- client.authenticate(refreshToken)
    weather <- client.getHomeWeather(homeId = 12345)
  yield weather
```

## API Reference

### Tado4sClient

The main entry point for interacting with the Tado API.

| Method                               | Description                              |
|--------------------------------------|------------------------------------------|
| `authenticate(refreshToken)`         | Authenticate with refresh token          |
| `logout()`                           | Clear authentication                     |
| `getAccountInfo()`                   | Get user account information             |
| `getHomeDetails(homeId)`             | Get home configuration                   |
| `getHomeZones(homeId)`               | Get zones for a home                     |
| `getHomeState(homeId)`               | Get current home state                   |
| `getHomeDevices(homeId)`             | Get registered devices                   |
| `getHomeInstallations(homeId)`       | Get installation details                 |
| `getHomeUsers(homeId)`               | Get configured users                     |
| `getZoneState(homeId, zoneId)`       | Get zone current state                   |
| `getHomeWeather(homeId)`             | Get weather at home location             |
| `getZoneDayReport(homeId, zoneId, date)` | Get historical day report            |
| `getZoneCapabilities(request)`       | Get zone temperature capabilities        |
| `getEarlyStart(request)`             | Get early start settings                 |
| `setEarlyStart(request)`             | Set early start enabled/disabled         |
| `getActiveTimetable(request)`        | Get active schedule timetable            |
| `setActiveTimetable(request)`        | Set active schedule timetable            |
| `getTimetables(request)`             | Get all schedule timetables              |
| `getTimetableBlocks(request)`        | Get schedule blocks for a timetable      |
| `getAwayConfiguration(request)`      | Get away mode configuration              |
| `setAwayConfiguration(request)`      | Set away mode configuration              |
| `setZoneOverlay(request)`            | Set manual temperature control           |
| `deleteZoneOverlay(request)`         | Clear manual control, resume schedule    |
| `setHomePresence(request)`           | Set home to HOME or AWAY                 |
| `getMobileDevices(request)`          | Get mobile devices                       |
| `getMobileDeviceSettings(request)`   | Get mobile device settings               |
| `setMobileDeviceSettings(request)`   | Set mobile device geo-tracking           |
| `deleteMobileDevice(request)`        | Remove a mobile device                   |
| `getHeatingCircuits(request)`        | Get heating circuit information          |
| `getAirComfort(request)`             | Get air comfort and freshness data       |

### Response Types

| Type                       | Description                       |
|----------------------------|-----------------------------------|
| `AccountResponse`          | User account with home list       |
| `HomeResponse`             | Home configuration and settings   |
| `HomeZoneResponse`         | Zone configuration                |
| `HomeStateResponse`        | Home presence state               |
| `HomeDeviceResponse`       | Device information                |
| `HomeInstallationResponse` | Installation details              |
| `HomeUserResponse`         | User information                  |
| `ZoneStateResponse`        | Zone temperature and settings     |
| `WeatherResponse`          | Current weather data              |
| `DayReportResponse`        | Historical zone data              |

### Configuration

Configure the client via `application.conf`:

```hocon
tado4s {
  api-base = "https://my.tado.com/api/v2"
  auth-url = "https://auth.tado.com/oauth"
  http-timeout = 30 seconds
  ignore-ssl = false
}
```

Or programmatically:

```scala
val config =
  TadoConfig(
    apiBase = Uri.unsafeFromString("https://my.tado.com/api/v2"),
    authUrl = Uri.unsafeFromString("https://auth.tado.com/oauth"),
    httpTimeout = 30.seconds,
    ignoreSsl = false,
  )

val client = Tado4sClient[IO](Some(config))
```

## Error Handling

Tado4s uses typed errors for API failures:

```scala
import com.colofabrix.scala.tado4s.*

client
  .getZoneState(homeId, zoneId)
  .handleErrorWith {
    case Tado4sError(message, Some(tadoError)) =>
      IO.println(s"API error: ${tadoError.message}")
    case Tado4sError(message, None) =>
      IO.println(s"Client error: $message")
  }
```

## Authentication Details

Tado uses OAuth2 with device code flow:

1. Run the authentication script: `python tado4s/scripts/tado_device_auth.py`
2. Follow the prompts to authorize the application
3. Store the refresh token securely
4. Pass the refresh token to `client.authenticate()`

The client automatically handles access token refresh when needed.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Tado4s is released under the MIT license. See [LICENSE](../LICENSE) for details.

## Author

[Fabrizio Colonna](mailto:colofabrix@tin.it)

## See Also

- [Tado API Reference](https://blog.scphillips.com/posts/2017/01/the-tado-api-v2/)
- [http4s](https://http4s.org/) - Typeful, functional HTTP for Scala
- [Cats Effect](https://typelevel.org/cats-effect/) - The pure asynchronous runtime for Scala
