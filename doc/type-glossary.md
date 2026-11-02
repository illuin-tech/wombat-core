# Wombat Generic Type Glossary

This document serves as a unified reference for all generic domain types, structures, enums, and data formats used across Wombat core and module configurations.

---

## 1. Temporal & Time Types

### `TimeRange`
Represents a closed time window between a starting timestamp and an ending timestamp in ISO-8601 format (UTC), with an optional sampling resolution (precision step) for partitioning or evaluating temporal intervals.

- **Structure**:
  ```yaml
  start: "2026-01-15T10:00:00Z" # required - ISO-8601 start instant.
  end: "2026-01-15T11:00:00Z" # required - ISO-8601 end instant.
  resolution: "PT1H" # optional (default: end - start) - Sampling resolution / precision step for interval partitioning (ISO-8601 duration e.g. `PT1M`, `PT15M`, `PT1H`).
  ```
- **Usage**: Used in `KubernetesActivityData` and `LLMActivityData` to delineate evaluation boundaries and discretization precision.

---

### `Period`
Represents a polling or sampling interval as a human-readable duration string consisting of an integer followed by a unit suffix.

- **Format**: `<value><unit>`
- **Supported Units**:
  - `s` : seconds (e.g. `30s`)
  - `m` : minutes (e.g. `1m`, `5m`)
  - `h` : hours (e.g. `1h`, `12h`)
  - `d` : days (e.g. `1d`)
  - `y` : years (e.g. `1y`)
- **Usage**: Used in source asset configurations (e.g. `kubernetes-api`, `llm-prometheus`) to set metric polling cadence.

---

### `Duration`
Standard ISO-8601 duration format used for timeouts and task execution lengths.

- **Format**: `PnDTnHnMn.nS` (e.g. `PT10S` for 10 seconds, `PT1H30M` for 1 hour 30 minutes, `PT4H` for 4 hours).
- **Usage**: Used in HTTP read timeouts (`kubernetes-api`) and batch scenario durations (`kubernetes-static`).

---

### `ZoneId`
Standard IANA timezone identifier string.

- **Format**: `String` (e.g. `UTC`, `Europe/Paris`, `America/New_York`, `Europe/Berlin`, `Asia/Tokyo`).
- **Default**: Typically defaults to `UTC` if omitted.
- **Usage**: Used in static profile configurations (`kubernetes-static`, `llm-static`) and `ApplicabilityFilter`.

---

## 2. Infrastructure & Hardware Types

### `Lifetime`
Operational hardware lifespan of server equipment, serialized as a duration string with unit suffix.

- **Format**: `<value><unit>` (e.g. `5y` for 5 years, `60m` for 60 months, `1825d` for 1825 days, `43800h` for 43800 hours).
- **Usage**: Used across all server hardware profiles to calculate embodied manufacturing emissions amortized over server lifetime.

---

### `Country`
Geographical country location code, adhering to ISO 3166-1 alpha-3 (or alpha-2).

- **Format**: 3-letter (or 2-letter) country code (e.g. `FRA`, `USA`, `DEU`, `SWE`, `GBR`, `IRL`).
- **Usage**: Used in server and cloud asset profiles to resolve localized carbon intensity factors (grid emissions).

---

### `ServerProvider`
Infrastructure and cloud hosting provider identifier.

- **Supported Values**:
  - `aws` : Amazon Web Services
  - `azure` : Microsoft Azure
  - `gcp` : Google Cloud Platform
  - `ovhcloud` : OVHcloud
  - `scaleway` : Scaleway
- **Usage**: Used in server profiles to look up instance architecture specifications and manufacturing footprints in the Boavizta dataset.

---

### `LLMProvider`
Foundation model and Generative AI provider identifier.

- **Supported Values**:
  - `anthropic` : Anthropic (e.g. Claude series)
  - `mistralai` : Mistral AI (e.g. Mistral, Mixtral)
  - `openai` : OpenAI (e.g. GPT-4o, GPT-4o-mini)
  - `huggingface_hub` : Hugging Face Hub (open weights)
  - `cohere` : Cohere
  - `google_genai` : Google Gemini / Vertex AI
- **Usage**: Used in LLM profiles (`llm-prometheus`, `llm-static`, `llm-simulated`) for token footprint modeling.

---

## 3. Activity & Metric Types

### `ActivityRegime`
Defines the operational provenance of workload activity data.

- **Supported Values**:
  - `MODELED` : Synthetic, projective, or simulated activity derived from mathematical scenarios or what-if inputs.
  - `MEASURED` : Telemetry data polled or queried from live cluster APIs or metric stores.
- **Usage**: Present in `KubernetesActivityData` and `LLMActivityData`.

---

### `CPUUsage`
Polymorphic structure representing server or container CPU consumption.

#### `NanocoresCPUUsage`
Specifies absolute CPU compute consumption measured in nanocores ($10^{-9}$ cores, where $1\text{ core} = 1{,}000{,}000{,}000\text{ nanocores}$).
```yaml
type: "nanocores" # required - Discriminator type identifier.
nanocores: 2500000000 # required (when type is nanocores) - CPU compute consumed in nanocores (2.5 cores).
```

#### `RelativeCPUUsage`
Specifies proportional CPU compute load as a fractional ratio of total server capacity ($0.0 \le \text{ratio} \le 1.0$).
```yaml
type: "relative" # required - Discriminator type identifier.
ratio: 0.35 # required - Fractional capacity utilization (35% capacity).
```

---

### `ContainerLocation`
Cluster location and namespace metadata for a microservice or container workload.

- **Structure**:
  ```yaml
  id: "production-cluster-01" # required - Target Kubernetes cluster identifier.
  namespace: "payments" # required - Kubernetes namespace.
  ```
- **Usage**: Used in `KubernetesActivityData` container location mappings.

---

### `LLMServiceActivity`
Workload volume metrics associated with a specific generative AI microservice.

- **Structure**:
  ```yaml
  provider: "openai" # required - Foundation model provider (`anthropic`, `mistralai`, `openai`, etc.).
  model: "gpt-4o" # required - Model identifier.
  location: "USA" # required - ISO 3166-1 alpha-3 (or alpha-2) country code.
  output-token-count: 1500000 # required - Total number of generated output/completion tokens.
  request-count: 4500.0 # required - Total number of inference API requests.
  ```
- **Usage**: Used in `LLMActivityData` services mapping.

---

## 4. Scenario & Temporal Modeling Types

### `ApplicabilityFilter`
Calendar and temporal filtering rules restricting when a static scenario is active.

- **Structure**:
  ```yaml
  days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY] # optional - Active days of the week.
  months: [JANUARY, FEBRUARY] # optional - Active calendar months.
  ```
- **Supported `days-of-week` Values**: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`.
- **Supported `months` Values**: `JANUARY`, `FEBRUARY`, `MARCH`, `APRIL`, `MAY`, `JUNE`, `JULY`, `AUGUST`, `SEPTEMBER`, `OCTOBER`, `NOVEMBER`, `DECEMBER`.

---

### `Curve`
Mathematical distribution curve defining daytime user traffic profiles between opening and closing hours in `user_activity` scenarios.

- **Supported Values**:
  - `bell` : Standard Gaussian-like bell distribution peaking at midday.
  - `sinusoidal` : Smooth sinusoidal arc across the active window.
  - `trapezoidal` : Rapid ramp-up to peak load, sustained plateau, and ramp-down.
- **Default**: `bell`
- **Usage**: Used in `user_activity` scenarios (`kubernetes-static`, `llm-static`).

---

### `Smoothing`
Transition smoothing curve applied to the onset and completion of scheduled batch jobs in `batch_activity` scenarios.

- **Supported Values**:
  - `none` : Step function with instant transition between baseline and peak load.
  - `linear_ramp` : Linear interpolation ramp at the start and end of the execution window.
  - `smooth_step` : Hermite polynomial smooth step ($S$-curve) transition.
- **Default**: `none`
- **Usage**: Used in `batch_activity` scenarios (`kubernetes-static`, `llm-static`).
