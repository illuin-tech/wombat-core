# Kubernetes Simulated Module

| Property           | Value                                                                      |
|--------------------|----------------------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.kubernetes-simulated`                           |
| **Family**         | `KUBERNETES_CONTAINER`                                                     |
| **Java Class**     | `tech.illuin.wombat.module.kubernetes_simulated.KubernetesSimulatedModule` |
| **Components**     | *(None / Directly Simulated Asset)*                                        |

## Description

The **Kubernetes Simulated Module** defines a self-contained simulated asset format used to evaluate carbon footprints, energy consumption, and environmental impacts directly from injected activity metrics without requiring live cluster polling or dynamic activity resolution.

Use this module when:
- You are using the [Wombat HTTP API simulation endpoints](https://github.com/illuin-tech/wombat#http-api) for on-demand footprint calculations.
- You need deterministic test fixtures and replay capabilities in integration testing and benchmarking pipelines.
- You want to perform comparative "what-if" architectural simulations (e.g. comparing the environmental impact of running the exact same historical container workload in different cloud regions or instance types).
