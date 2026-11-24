# LLM Metrics Module

| Property           | Value                                                       |
|--------------------|-------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.llm-metrics`                     |
| **Type**           | Family module (`FamilyType`)                                |
| **Family**         | `LLM`                                                       |
| **Java Class**     | `tech.illuin.wombat.module.llm_metrics.LLMMetricsModule`    |
| **Components**     | `activity-resolver` (`LLMActivityResolver`)                 |

## Description

The **LLM Metrics Module** resolves the activity of LLM assets out of the metrics sourced for them: it aggregates the requests and token counts per model the `AssetMonitor` persisted, from the metric store the host hands the core (`WombatServices.metrics()`).

It defines no asset-type: it serves the assets of the `LLM` family whose own module resolves no activity, typically sourced ones such as [`llm-prometheus`](llm-prometheus.md). Assets whose activity is read on demand ([`llm-anthropic`](llm-anthropic.md), [`llm-vertexai`](llm-vertexai.md)) or modeled ([`llm-static`](llm-static.md)) resolve their own.

It has no settings.
