# Kubernetes Metrics Module

| Property           | Value                                                                    |
|--------------------|--------------------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.kubernetes-metrics`                           |
| **Type**           | Family module (`FamilyType`)                                             |
| **Family**         | `KUBERNETES_CONTAINER`                                                   |
| **Java Class**     | `tech.illuin.wombat.module.kubernetes_metrics.KubernetesMetricsModule`   |
| **Components**     | `activity-resolver` (`KubernetesActivityResolver`)                       |

## Description

The **Kubernetes Metrics Module** resolves the activity of Kubernetes assets out of the metrics sourced for them: it aggregates the container CPU usage, container shares and locations the `AssetMonitor` persisted, from the metric store the host hands the core (`WombatServices.metrics()`).

It defines no asset-type: it serves the assets of the `KUBERNETES_CONTAINER` family whose own module resolves no activity, typically sourced ones such as [`kubernetes-api`](kubernetes-api.md). Modeled assets such as [`kubernetes-static`](kubernetes-static.md) resolve their own.

It has no settings.
