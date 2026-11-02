# LLM Static Module

| Property           | Value                                                  |
|--------------------|--------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.llm-static`                 |
| **Family**         | `LLM`                                                  |
| **Java Class**     | `tech.illuin.wombat.module.llm_static.LLMStaticModule` |
| **Components**     | `activity-resolver` (`LLMStaticActivityResolver`)      |

## Description

The **LLM Static Module** provides projective and scenario-based modeling of generative AI token usage and request volumes without requiring live inference server connections or API credentials.

Use this module when:
- You want to forecast the environmental footprint and operational costs of planned AI features prior to production launch.
- You need deterministic modeling based on expected annual query volumes, diurnal usage cycles, and batch report generation.
- You operate a multi-model architecture combining lightweight routing models with heavy frontier reasoning models.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.llm-static` (`LLMStaticAsset`) with a static profile configuration (`LLMStaticProfile`).

For cross-cutting data formats such as `Duration`, `Country`, `LLMProvider`, `ZoneId`, `ApplicabilityFilter`, `Curve`, and `Smoothing`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
id: "llm-static-assistant" # `String`, Required - Unique identifier for the asset.
environment-id: "production" # `String`, Required - Identifier of the environment (e.g. `production`, `planning`).
name: "Enterprise Multi-Model Assistant" # `String`, Required - Human-readable asset name.
profile: # `LLMStaticProfile`, Required - Multi-model profile configuration.
  zone-id: "UTC" # `ZoneId`, Optional(default: UTC) - Default timezone for scenario schedule evaluation (e.g. `UTC`, `Europe/Paris`).
  models: # `List<ModelConfig>`, Required - List of foundation models and their respective scenarios.
    - provider: "openai" # `LLMProvider`, Required - Foundation model provider (`anthropic`, `mistralai`, `openai`, etc.).
      model: "gpt-4o" # `String`, Required - Model identifier (e.g. `gpt-4o`, `claude-3-5-sonnet-20241022`).
      location: "USA" # `Country`, Required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `USA`, `FRA`, `IRL`).
      scenarios: # `List<Scenario>`, Required - List of superposed traffic scenarios for this model.
        # --- 1. Constant Scenario (`constant`) ---
        - type: "constant" # `String`, Required - Scenario discriminator: `constant`.
          output-token-per-request: 250 # `Integer`, Required - Mean output tokens per request.
          request-per-year: 500000.0 # `Double`, Required - Projected annual request count.
          applicability: # `ApplicabilityFilter`, Optional - Temporal applicability filter.
            days-of-week: ["MONDAY", "TUESDAY"] # `List<String>`, Optional - Active days of week.
            months: ["JANUARY"] # `List<String>`, Optional - Active calendar months.

        # --- 2. User Activity Scenario (`user_activity`) ---
        - type: "user_activity" # `String`, Required - Scenario discriminator: `user_activity`.
          output-token-per-request: 350 # `Integer`, Required - Mean output tokens per request.
          baseline-request-per-year: 50000.0 # `Double`, Required - Annualized request rate during off-hours.
          peak-request-per-year: 1200000.0 # `Double`, Required - Annualized request rate during peak hours.
          opening-time: "08:30:00" # `String` (`HH:mm:ss`), Required - Opening time of the daily activity window.
          closing-time: "18:30:00" # `String` (`HH:mm:ss`), Required - Closing time of the daily activity window.
          curve: "bell" # `Curve`, Optional(default: bell) - Mathematical shape: `bell`, `sinusoidal`, `trapezoidal`.
          jitter: 0.05 # `Double`, Optional(default: 0.0) - Deterministic variance factor.
          applicability: # `ApplicabilityFilter`, Optional - Temporal applicability filter.
            days-of-week: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"] # `List<String>`, Optional - Active days.

        # --- 3. Batch Activity Scenario (`batch_activity`) ---
        - type: "batch_activity" # `String`, Required - Scenario discriminator: `batch_activity`.
          output-token-per-request: 1200 # `Integer`, Required - Mean output tokens per batch request.
          baseline-request-per-year: 0.0 # `Double`, Required - Annualized request rate outside batch window.
          peak-request-per-year: 800000.0 # `Double`, Required - Annualized request rate during active batch processing.
          trigger-time: "01:00:00" # `String` (`HH:mm:ss`), Required - Daily start time of the batch job.
          duration: "PT3H" # `Duration`, Required - Duration of the batch window (ISO-8601).
          smoothing: "smooth_step" # `Smoothing`, Optional(default: none) - Smoothing mode: `none`, `linear_ramp`, `smooth_step`.
          jitter: 0.02 # `Double`, Optional(default: 0.0) - Deterministic variance factor.
          applicability: # `ApplicabilityFilter`, Optional - Temporal applicability filter.
            days-of-week: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"] # `List<String>`, Optional - Active days.
```

## Basic Setup

A minimal configuration modeling a constant chatbot service using OpenAI `gpt-4o` with 500,000 annual requests and 350 output tokens per interaction.

```yaml
id: "llm-static-chatbot"
environment-id: "production"
name: "Customer Support Chatbot"
profile:
  zone-id: "UTC"
  models:
    - provider: "openai"
      model: "gpt-4o"
      location: "USA"
      scenarios:
        - type: "constant"
          output-token-per-request: 350
          request-per-year: 500000.0
```

## Advanced Setup

### Multi-Model Enterprise AI Architecture

Modern enterprise AI platforms distribute tasks across specialized models:
1. **Lightweight Router / Guardrail Model** (`Mistral-7B` in `FRA`): High-volume classification handling 3,000,000 requests/year with daytime user peaks (08:00–19:00 on weekdays) generating 80 tokens/request.
2. **Primary Copilot & Coding Assistant** (`Claude-3.5-Sonnet` in `USA`): Diurnal business-hours usage (09:00–18:00 on weekdays) generating 450 tokens/request with 1,200,000 peak requests/year.
3. **Nightly Knowledge Base Indexing & Summarization** (`GPT-4o` in `USA`): Scheduled batch pipeline running nightly between 01:00 and 04:00 (3 hours) generating 1,500 tokens/request.

```yaml
id: "enterprise-ai-platform"
environment-id: "production"
name: "Corporate Multi-Model AI Platform"
profile:
  zone-id: "Europe/Paris"
  models:
    # 1. Routing and classification model
    - provider: "mistralai"
      model: "mistralai/Mistral-7B-Instruct-v0.2"
      location: "FRA"
      scenarios:
        - type: "user_activity"
          output-token-per-request: 80
          baseline-request-per-year: 100000.0
          peak-request-per-year: 3000000.0
          opening-time: "08:00:00"
          closing-time: "19:00:00"
          curve: "bell"
          jitter: 0.03
          applicability:
            days-of-week:
              - "MONDAY"
              - "TUESDAY"
              - "WEDNESDAY"
              - "THURSDAY"
              - "FRIDAY"

    # 2. Main reasoning and coding assistant
    - provider: "anthropic"
      model: "claude-3-5-sonnet-20241022"
      location: "USA"
      scenarios:
        - type: "user_activity"
          output-token-per-request: 450
          baseline-request-per-year: 20000.0
          peak-request-per-year: 1200000.0
          opening-time: "09:00:00"
          closing-time: "18:00:00"
          curve: "trapezoidal"
          jitter: 0.05
          applicability:
            days-of-week:
              - "MONDAY"
              - "TUESDAY"
              - "WEDNESDAY"
              - "THURSDAY"
              - "FRIDAY"

    # 3. Nightly batch document summarization
    - provider: "openai"
      model: "gpt-4o"
      location: "USA"
      scenarios:
        - type: "batch_activity"
          output-token-per-request: 1500
          baseline-request-per-year: 0.0
          peak-request-per-year: 600000.0
          trigger-time: "01:00:00"
          duration: "PT3H"
          smoothing: "smooth_step"
          applicability:
            days-of-week:
              - "TUESDAY"
              - "WEDNESDAY"
              - "THURSDAY"
              - "FRIDAY"
              - "SATURDAY"
```
