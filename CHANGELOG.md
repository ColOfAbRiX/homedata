# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-02-09

### Added

- Octopus Energy integration for gas and electricity consumption readings
- Tado integration for temperature and heating data
- InfluxDB writer for storing time-series data
- Scrape log to track fetched data and avoid duplicates
- Rate limiting for Octopus and Tado API calls
- Configurable poll time for continuous data collection
- Batch writing for efficient InfluxDB operations
- Docker deployment with InfluxDB and Grafana stack
- Docker Compose configuration for full stack deployment
- PureConfig-based configuration with environment variable support

[0.1.0]: https://github.com/ColOfAbRiX/homedata/releases/tag/v0.1.0
