[![Check](https://github.com/Drill4J/autotest-agent/actions/workflows/check.yml/badge.svg)](https://github.com/Drill4J/autotest-agent/actions/workflows/check.yml)
[![Release](https://github.com/Drill4J/autotest-agent/actions/workflows/release.yml/badge.svg)](https://github.com/Drill4J/autotest-agent/actions/workflows/release.yml)
[![License](https://img.shields.io/github/license/Drill4J/autotest-agent)](LICENSE)
[![Visit the website at drill4j.github.io](https://img.shields.io/badge/visit-website-green.svg?logo=firefox)](https://drill4j.github.io/)
[![Telegram Chat](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/drill4j)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Drill4J/autotest-agent)
![GitHub contributors](https://img.shields.io/github/contributors/Drill4J/autotest-agent)
![Lines of code](https://img.shields.io/tokei/lines/github/Drill4J/autotest-agent)
![YouTube Channel Views](https://img.shields.io/youtube/channel/views/UCJtegUnUHr0bO6icF1CYjKw?style=social)

# Drill native autotest agent

This module contains the native agent for JVM autotest - [list of frameworks](https://drill4j.github.io/docs/supported-frameworks).

These autotest-agents add Drill headers to HTTP requests in order to track coverage. See more in [core concepts](https://drill4j.github.io/docs/core-concepts).

## Modules

- **autotest-agent**: Autotest agent classes
- **tests-common**: Common classes for tests-* modules
- **tests-admin-stub-server**: Stub server (emulating test2code-plugin) for agent communications during testing
- **tests-***: Modules contains the tests for autotest agent
