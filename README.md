# HomeData

Personal data scraper for home energy and heating data. Pulls consumption data from Octopus Energy (gas & electricity) and Tado (heating/temperature) and stores it in InfluxDB for visualization with Grafana.

## Behavior

The application runs in a continuous loop:
1. Pulls gas and electricity readings from Octopus Energy API
2. Pulls temperature and heating data from Tado API
3. Writes all data to InfluxDB
4. Sleeps for the configured poll time (default: 15 minutes)

A scrape log file tracks what has already been fetched to avoid duplicate requests.

## Project Structure

```
houseData/
├── src/main/                  # Main application source
├── docker/
│   ├── docker-compose.yml     # Full stack (homedata + InfluxDB + Grafana)
│   ├── Dockerfile             # Docker image build
│   ├── build.sh               # Docker build script
│   └── config/                # Configuration files for docker deployment
│       ├── homedata/application.conf
│       ├── influxdb/
│       └── grafana/
└── README.md
```

## Configuration

Configuration is in `src/main/resources/reference.conf` (defaults) or override with `application.conf`.

| Setting                    | Default                | Description                 |
|----------------------------|------------------------|-----------------------------|
| `poll-time`                | 15 minutes             | Interval between data pulls |
| `scrape-log.log-path`      | ~/.homedata/scrape.log | Tracks fetched data         |
| `influxdb.server-url`      | http://localhost:8086  | InfluxDB endpoint           |
| `influxdb.org-name`        | homedata               | InfluxDB organization       |
| `influxdb.project-bucket`  | home_data              | InfluxDB bucket             |
| `octopus.requests-per-sec` | 4                      | Rate limit for Octopus API  |
| `tado.requests-per-sec`    | 12                     | Rate limit for Tado API     |

## Environment Variables

Required secrets (set via environment or `docker/secrets.env`):

**InfluxDB:**
- `HOMEDATA_INFLUXDB_AUTH_TOKEN`

**Octopus Energy:**
- `HOMEDATA_OCTOPUS_API_KEY`
- `HOMEDATA_OCTOPUS_ACCOUNT_NUMBER`
- `HOMEDATA_OCTOPUS_ELECTRICITY_MPAN`
- `HOMEDATA_OCTOPUS_ELECTRICITY_SERIAL`
- `HOMEDATA_OCTOPUS_GAS_MPRN`
- `HOMEDATA_OCTOPUS_GAS_SERIAL`

**Tado:**
- `HOMEDATA_TADO_TOKEN` - Initial refresh token
- `HOMEDATA_TADO_TOKEN_ISSUE_TIME` - Token issue timestamp

## Running Locally

```bash
# Set environment variables
export HOMEDATA_INFLUXDB_AUTH_TOKEN="..."
export HOMEDATA_OCTOPUS_API_KEY="..."
# ... (set all required variables)

# Run with sbt
sbt "homedata/run"
```

## Running with Docker

### Start the full stack

```bash
cd docker

# Create secrets.env with your credentials
cp secrets.env.example secrets.env
# Edit secrets.env with your values

# Start InfluxDB + Grafana + HomeData
docker-compose up -d
```

### Build the Docker image

```bash
cd docker
./build.sh
```

### Services

| Service  | Port | Description                    |
|----------|------|--------------------------------|
| homedata | -    | Data scraper (no exposed port) |
| influxdb | 8086 | Time-series database           |
| grafana  | 3000 | Visualization dashboard        |

### Docker data locations

- Configuration: `docker/config/homedata/application.conf`
- Scrape log: `docker/data/homedata/scrape.log`
- InfluxDB data: `docker/data/influxdb/`
- Grafana data: `docker/data/grafana/`

## Author

Fabrizio Colonna
