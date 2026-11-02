# Kubernetes Simulated Module

| Property           | Value                                                                      |
|--------------------|----------------------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.kubernetes-simulated`                           |
| **Family**         | `KUBERNETES_CONTAINER`                                                     |
| **Java Class**     | `tech.illuin.wombat.module.kubernetes_simulated.KubernetesSimulatedModule` |
| **Components**     | *(None / Direct Simulated Asset)*                                          |

## Description

The **Kubernetes Simulated Module** defines a self-contained simulated asset format used to evaluate carbon footprints, energy consumption, and environmental impacts directly from injected activity metrics without requiring live cluster polling or dynamic activity resolution.

Use this module when:
- You are using the [Wombat HTTP API simulation endpoints](https://github.com/illuin-tech/wombat#http-api) for on-demand footprint calculations.
- You need deterministic test fixtures and replay capabilities in integration testing and benchmarking pipelines.
- You want to perform comparative "what-if" architectural simulations (e.g. comparing the environmental impact of running the exact same historical container workload in different cloud regions or instance types).

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.kubernetes-simulated` (`KubernetesSimulatedAsset`), which couples a cluster hardware profile (`KubernetesSimulatedProfile`) with an explicit activity dataset (`KubernetesActivityData`).

For cross-cutting data formats such as `TimeRange`, `Lifetime`, `Country`, `ServerProvider`, `ActivityRegime`, `CPUUsage`, and `ContainerLocation`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
profile: # `KubernetesSimulatedProfile`, Required - Cluster hardware and server specification profile.
  provider: "aws" # `ServerProvider`, Required - Infrastructure provider (`aws`, `azure`, `gcp`, `ovhcloud`, `scaleway`).
  instance-type: "c5.large" # `String`, Required - Machine instance type (e.g. `c5.2xlarge`, `n2-standard-4`).
  location: "FRA" # `Country`, Required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `FRA`, `USA`, `DEU`, `SWE`).
  lifespan: "5y" # `Lifetime`, Required - Expected operational lifespan of server hardware (e.g. `5y`, `60m`, `1825d`).
  node-count: 1 # `Integer`, Optional(default: 1) - Number of identical worker nodes in the cluster.

activity: # `KubernetesActivityData`, Required - Explicit Kubernetes workload metrics.
  regime: "MODELED" # `ActivityRegime`, Required - Activity data regime: `MODELED` or `MEASURED`.
  service-ids: # `List<String>`, Required - List of distinct container/service identifiers.
    - "auth-service"
  range: # `TimeRange`, Required - Temporal evaluation window with start and end timestamps.
    start: "2026-01-15T10:00:00Z" # `String`, Required - ISO-8601 evaluation start timestamp.
    end: "2026-01-15T11:00:00Z" # `String`, Required - ISO-8601 evaluation end timestamp.
  cpu-usage: # `CPUUsage`, Required - Polymorphic CPU usage (`NanocoresCPUUsage` or `RelativeCPUUsage`).
    type: "nanocores" # `String`, Required - Discriminator type identifier: `nanocores` or `relative`.
    nanocores: 2500000000 # `Long`, Required (when type is nanocores) - CPU compute consumed in nanocores (2.5 cores).
  container-shares: # `Map<String, Double>`, Required - Share of overall cluster workload per service ID (sums to ~1.0).
    auth-service: 1.0
  container-locations: # `Map<String, ContainerLocation>`, Required - Cluster placement metadata per service.
    auth-service:
      id: "prod-cluster-01" # `String`, Required - Target Kubernetes cluster identifier.
      namespace: "security" # `String`, Required - Kubernetes namespace.
```

## Basic Setup

A minimal simulated asset representing 1 hour of container execution on a single `c5.large` instance in France with 2.5 CPU cores consumed.

```yaml
profile:
  provider: "aws"
  instance-type: "c5.large"
  location: "FRA"
  lifespan: "5y"
  node-count: 1

activity:
  regime: "MODELED"
  service-ids:
    - "auth-service"
  range:
    start: "2026-01-15T10:00:00Z"
    end: "2026-01-15T11:00:00Z"
  cpu-usage:
    type: "nanocores"
    nanocores: 2500000000
  container-shares:
    auth-service: 1.0
  container-locations:
    auth-service:
      id: "prod-cluster-01"
      namespace: "security"
```

## Advanced Setup

### Comparative What-If Simulation: Multi-Service Cloud Migration

In a cloud migration or green-ops optimization study, teams can evaluate the exact same 24-hour microservice workload across multiple simulated asset profiles to quantify potential carbon emission reductions.

Below is a 24-hour evaluation payload with three interdependent microservices (`gateway-service`, `orders-api`, `billing-worker`) sharing cluster compute resources:

```yaml
profile:
  provider: "scaleway"
  instance-type: "PRO2-XXL"
  location: "FRA"
  lifespan: "6y"
  node-count: 4

activity:
  regime: "MEASURED"
  service-ids:
    - "gateway-service"
    - "orders-api"
    - "billing-worker"
  range:
    start: "2026-03-01T00:00:00Z"
    end: "2026-03-02T00:00:00Z"
  cpu-usage:
    type: "relative"
    ratio: 0.65
  container-shares:
    gateway-service: 0.25
    orders-api: 0.45
    billing-worker: 0.30
  container-locations:
    gateway-service:
      id: "cloud-cluster-paris"
      namespace: "edge"
    orders-api:
      id: "cloud-cluster-paris"
      namespace: "commerce"
    billing-worker:
      id: "cloud-cluster-paris"
      namespace: "finance"
```
