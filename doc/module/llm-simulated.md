# LLM Simulated Module

| Property           | Value                                                        |
|--------------------|--------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.llm-simulated`                    |
| **Family**         | `LLM`                                                        |
| **Java Class**     | `tech.illuin.wombat.module.llm_simulated.LLMSimulatedModule` |
| **Components**     | *(None: Directly Simulated Asset)*                           |

## Description

The **LLM Simulated Module** defines a self-contained simulated asset format for evaluating environmental footprints and financial impacts of generative AI workloads directly from injected token and request activity metrics.

Use this module when:
- You are using the [Wombat HTTP API simulation endpoints](https://github.com/illuin-tech/wombat#http-api) for on-demand LLM footprint evaluations.
- You want to perform deterministic automated regression testing or continuous benchmarking of AI pipelines.
- You want to run comparative architectural benchmarks assessing the environmental impact of switching foundation models or inference providers under identical prompt and output token volumes.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.llm-simulated` (`LLMSimulatedAsset`), coupling a model profile (`LLMSimulatedProfile`) with an explicit activity dataset (`LLMActivityData`).

For cross-cutting data formats such as `TimeRange`, `Country`, `LLMProvider`, `ActivityRegime`, and `LLMServiceActivity`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
profile: # `LLMSimulatedProfile`, Required - Target foundation model profile.
  provider: "openai" # `LLMProvider`, Required - Foundation model provider (`anthropic`, `mistralai`, `openai`, `huggingface_hub`, `cohere`, `google_genai`).
  model: "gpt-4o" # `String`, Required - Standardized model identifier (e.g. `gpt-4o`, `claude-3-5-sonnet-20241022`).
  location: "USA" # `Country`, Required - ISO 3166-1 alpha-3 (or alpha-2) country code where inference is modeled (e.g. `USA`, `FRA`).

activity: # `LLMActivityData`, Required - Explicit LLM activity dataset with token counts and request volumes.
  regime: "MODELED" # `ActivityRegime`, Required - Activity data regime: `MODELED` or `MEASURED`.
  service-ids: # `List<String>`, Required - List of service or consumer identifiers.
    - "customer-support-agent"
  range: # `TimeRange`, Required - Evaluation time window with start and end timestamps.
    start: "2026-02-01T00:00:00Z" # `String`, Required - ISO-8601 evaluation start timestamp.
    end: "2026-02-01T01:00:00Z" # `String`, Required - ISO-8601 evaluation end timestamp.
  services: # `Map<String, LLMServiceActivity>`, Required - Map of service ID to service activity records.
    customer-support-agent:
      provider: "openai" # `LLMProvider`, Required - Model provider for this service.
      model: "gpt-4o" # `String`, Required - Model identifier for this service.
      location: "USA" # `Country`, Required - ISO 3166-1 country code for this service.
      output-token-count: 150000 # `Long`, Required - Total output tokens generated during the window.
      request-count: 300.0 # `Double`, Required - Total number of inference requests executed.
```

## Basic Setup

A minimal simulated asset representing an evaluation of 150,000 output tokens generated across 300 requests by OpenAI `gpt-4o`.

```yaml
profile:
  provider: "openai"
  model: "gpt-4o"
  location: "USA"

activity:
  regime: "MODELED"
  service-ids:
    - "customer-support-agent"
  range:
    start: "2026-02-01T00:00:00Z"
    end: "2026-02-01T01:00:00Z"
  services:
    customer-support-agent:
      provider: "openai"
      model: "gpt-4o"
      location: "USA"
      output-token-count: 150000
      request-count: 300.0
```

## Advanced Setup

### Multi-Service Comparative Model Benchmarking

Teams running automated evaluation pipelines can simulate multi-tenant AI systems where multiple services utilize different models or compare model switching impacts under identical usage loads.

The payload below demonstrates a multi-service AI workflow evaluated over a 24-hour window with separate token metrics for an intent router, a coding assistant, and a summarizer:

```yaml
profile:
  provider: "anthropic"
  model: "claude-3-5-sonnet-20241022"
  location: "USA"

activity:
  regime: "MEASURED"
  service-ids:
    - "intent-classifier"
    - "coding-assistant"
    - "document-summarizer"
  range:
    start: "2026-03-10T00:00:00Z"
    end: "2026-03-11T00:00:00Z"
  services:
    intent-classifier:
      provider: "mistralai"
      model: "mistralai/Mistral-7B-Instruct-v0.2"
      location: "FRA"
      output-token-count: 120000
      request-count: 2000.0
    coding-assistant:
      provider: "anthropic"
      model: "claude-3-5-sonnet-20241022"
      location: "USA"
      output-token-count: 1850000
      request-count: 1500.0
    document-summarizer:
      provider: "openai"
      model: "gpt-4o"
      location: "USA"
      output-token-count: 650000
      request-count: 400.0
```
