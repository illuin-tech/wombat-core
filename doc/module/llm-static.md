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

## How the traffic is modeled

Each model states **how much** traffic it gets, and its scenarios say **when**:
- `request-per-year` is the model's volume over a 365-day year, and `output-token-per-request` the mean size of its requests: a model's tokens are always its requests times that figure.
- Each scenario draws a curve (constant, daytime user activity, nightly batch) at a `level`. At each instant, the busiest applicable scenario draws the model's shape: the scenarios describe one model's behaviour, they do not add up.
- The shape is scaled so that its area over an average 365-day year is the model's volume. A `level` has no unit: only its ratio to the model's other scenarios matters. It defaults to `1.0`.

For instance, a model at 1,000,000 requests a year with a constant at `0.1` under a weekday office-hours bell at `1.0` runs at about 540 requests/h at the bell's peak (13:30 on a weekday), and at a tenth of that, 54 requests/h, at night and on weekends. Doubling the volume doubles every rate; raising the constant to `0.2` makes the nights heavier, the year still holding 1,000,000 requests.

The year is an average one: a model holds its volume over any 365-day year when its shape is the same every day; one with weekday or month scenarios holds it give or take its year's count of such days (2026 has 261 weekdays for an average of 260.7), and a leap year holds one day more.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.llm-static` (`LLMStaticAsset`) with a static profile configuration (`LLMStaticProfile`).

For cross-cutting data formats such as `Duration`, `Country`, `LLMProvider`, `ZoneId`, `ApplicabilityFilter`, `Curve`, and `Smoothing`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
id: "llm-static-assistant" # required - Unique identifier for the asset.
environment-id: "production" # required - Identifier of the environment (e.g. `production`, `planning`).
profile: # required - Multi-model profile configuration.
  zone-id: "UTC" # optional (default: UTC) - Timezone the scenarios' times are evaluated in (e.g. `UTC`, `Europe/Paris`).
  models: # required - List of foundation models, their volumes and the scenarios that shape them.
    - provider: "openai" # required - Foundation model provider (`anthropic`, `mistralai`, `openai`, etc.).
      model: "gpt-5.5-pro" # required - Model identifier (e.g. `gpt-5.5-pro`, `claude-sonnet-4-6` ; see ecologits for their list of models https://github.com/mlco2/ecologits/blob/main/ecologits/data/models.json).
      location: "USA" # required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `USA`, `FRA`, `IRL`).
      request-per-year: 2000000 # required - Requests in a 365-day year, spread along the scenarios' shape.
      output-token-per-request: 350 # required - Mean output tokens of a request.
      scenarios: # required - The shape of the traffic, the busiest applicable scenario winning at each instant.
        # Constant scenario
        - type: constant
          level: 0.1 # optional (default: 1.0) - Relative traffic level, only its ratio to the model's other scenarios matters.
          applicability: # optional - Temporal applicability filter.
            days-of-week: [MONDAY, TUESDAY] # optional - Active days of week.
            months: [JANUARY] # optional - Active calendar months.
        # User Activity scenario
        - type: user_activity
          level: 1.0 # optional (default: 1.0) - Relative traffic level at the curve's peak.
          opening-time: "08:30:00" # required - Opening time of the daily activity window (in `HH:mm:ss` format).
          closing-time: "18:30:00" # required - Closing time of the daily activity window (in `HH:mm:ss` format).
          curve: "bell" # optional (default: bell) - Mathematical shape: `bell`, `sinusoidal`, `trapezoidal`.
          jitter: 0.05 # optional (default: 0.0) - Deterministic variance factor (e.g. `0.05` for ±5%).
          applicability: # optional - Temporal applicability filter.
            days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY] # optional - Active days.
        # Batch Activity scenario
        - type: batch_activity
          level: 0.5 # optional (default: 1.0) - Relative traffic level over the batch window.
          trigger-time: "01:00:00" # required - Daily start time of the batch job (in `HH:mm:ss` format).
          duration: "PT3H" # required - Duration of the batch window (ISO-8601).
          smoothing: "smooth_step" # optional (default: none) - Smoothing mode: `none`, `linear_ramp`, `smooth_step`.
          jitter: 0.02 # optional (default: 0.0) - Deterministic variance factor.
          applicability: # optional - Temporal applicability filter.
            days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY] # optional - Active days.
```

## Basic Setup

A minimal configuration modeling a constant chatbot service using OpenAI `gpt-5.5-pro` with 500,000 annual requests and 350 output tokens per interaction.

```yaml
id: "llm-static-chatbot"
environment-id: "production"
profile:
  zone-id: "UTC"
  models:
    - provider: "openai"
      model: "gpt-5.5-pro"
      location: "USA"
      request-per-year: 500000
      output-token-per-request: 350
      scenarios:
        - type: "constant"
```

## Advanced Setup

### Multi-Model Enterprise AI Architecture

Modern enterprise AI platforms distribute tasks across specialized models, each with its own volume and request size:
1. **Lightweight Router / Guardrail Model** (`ministral-8b-latest` in `FRA`): 3,000,000 requests/year of 80 tokens, peaking during working hours (08:00–19:00 on weekdays) over a light baseline.
2. **Primary Copilot & Coding Assistant** (`claude-sonnet-4-6` in `USA`): 1,200,000 requests/year of 450 tokens during business hours (09:00–18:00 on weekdays).
3. **Nightly Knowledge Base Indexing & Summarization** (`gpt-5.5-pro` in `USA`): 600,000 requests/year of 1,500 tokens, in a batch running between 01:00 and 04:00.

```yaml
id: "enterprise-ai-platform"
environment-id: "production"
profile:
  zone-id: "Europe/Paris"
  models:
    # 1. Routing and classification model: a baseline at a twentieth of the working-hours peak
    - provider: "mistralai"
      model: "ministral-8b-latest"
      location: "FRA"
      request-per-year: 3000000
      output-token-per-request: 80
      scenarios:
        - type: constant
          level: 0.05
        - type: user_activity
          opening-time: "08:00:00"
          closing-time: "19:00:00"
          curve: bell
          jitter: 0.03
          applicability:
            days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
    # 2. Main reasoning and coding assistant
    - provider: "anthropic"
      model: "claude-sonnet-4-6"
      location: "USA"
      request-per-year: 1200000
      output-token-per-request: 450
      scenarios:
        - type: user_activity
          opening-time: "09:00:00"
          closing-time: "18:00:00"
          curve: trapezoidal
          jitter: 0.05
          applicability:
            days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
    # 3. Nightly batch document summarization
    - provider: "openai"
      model: "gpt-5.5-pro"
      location: "USA"
      request-per-year: 600000
      output-token-per-request: 1500
      scenarios:
        - type: batch_activity
          trigger-time: "01:00:00"
          duration: "PT3H"
          smoothing: smooth_step
          applicability:
            days-of-week: [TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY]
```
