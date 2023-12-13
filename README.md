[![Check](https://github.com/Drill4J/autotest-agent/actions/workflows/check.yml/badge.svg)](https://github.com/Drill4J/autotest-agent/actions/workflows/check.yml)
[![Release](https://github.com/Drill4J/autotest-agent/actions/workflows/release.yml/badge.svg)](https://github.com/Drill4J/autotest-agent/actions/workflows/release.yml)
[![License](https://img.shields.io/github/license/Drill4J/autotest-agent)](LICENSE)
[![Visit the website at drill4j.github.io](https://img.shields.io/badge/visit-website-green.svg?logo=firefox)](https://drill4j.github.io/)
[![Telegram Chat](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/drill4j)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Drill4J/autotest-agent)
![GitHub contributors](https://img.shields.io/github/contributors/Drill4J/autotest-agent)
![Lines of code](https://img.shields.io/tokei/lines/github/Drill4J/autotest-agent)
![YouTube Channel Views](https://img.shields.io/youtube/channel/views/UCJtegUnUHr0bO6icF1CYjKw?style=social)

# Drill4J Java Auto Test Agent

This agent is intended to be integrated with tests written in either Java or other JVM languages.

For setup guide see [respective docs page](https://drill4j.github.io/docs/installation/setup-java-autotest-agent)

## Purpose
Drill4J Auto Test Java Agent does 3 things:
- instruments test framework to figure out _which test_ is running at the moment
- sends metadata about test to Drill4J Admin Backend
- injects _test id_ (unique identifier generated for each test) into transport layer messages (e.g. headers in case of HTTP transport) for Drill4J Java Agent to detect and associate coverage with the respective test. See more in-depth explanation at [data collection docs section](https://drill4j.github.io/docs/basic-concepts/data-collection).

## Support
- For list supported frameworks [docs page](https://drill4j.github.io/docs/supported-frameworks).

## For development
### Modules

- **autotest-agent**: Autotest agent classes
- **autotest-runtime**: Autotest agent SessionProvider classes
- **tests-common**: Common classes for tests-* modules
- **tests-admin-stub-server**: Stub server (emulating test2code-plugin) for agent communications during testing
- **tests-***: Modules contains the tests for autotest agent
