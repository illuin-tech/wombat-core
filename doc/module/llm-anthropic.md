# LLM Anthropic Module

| Property           | Value                                                          |
|--------------------|----------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.llm-anthropic`                      |
| **Family**         | `LLM`                                                          |
| **Java Class**     | `tech.illuin.wombat.module.llm_anthropic.LLMAnthropicModule`   |
| **Components**     | `activity-resolver` (`LLMAnthropicActivityResolver`)           |

## Description

The **LLM Anthropic Module** queries the [Anthropic Admin API usage report](https://docs.anthropic.com/en/api/usage-cost-api) on demand to retrieve the output tokens consumed per Claude model over the evaluated time range.

Unlike source-based modules, it does not poll nor persist metrics in Wombat's metric store: usage is fetched from Anthropic whenever an evaluation or a timeline is requested, Anthropic being the system of record.

Use this module when:
- Your organization consumes Claude models through the Anthropic API and holds an Admin API key.
- You want measured (rather than modeled) token volumes without deploying a gateway or exposing Prometheus metrics.
- You need to scope the footprint to a subset of models, workspaces or API keys of your organization.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.llm-anthropic` (`LLMAnthropicAsset`) with a profile configuration (`LLMAnthropicProfile`).

For cross-cutting data formats such as `Country`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
id: "anthropic-org" # required - Unique identifier for the asset.
environment-id: "production" # required - Identifier of the environment (e.g. `production`, `staging`).
name: "Anthropic Organization" # required - Human-readable asset name.
admin-key: "secrets.anthropic.admin-key" # required - Secret resolver key for the Anthropic Admin API key (`sk-ant-admin...`); a standard API key is rejected by the usage report endpoint.
api-url: "https://api.anthropic.com" # optional (default: https://api.anthropic.com) - Base URL of the Anthropic API.
proxy-url: "http://proxy.corp.internal:8080" # optional (default: null) - HTTP corporate proxy URL; must include a host and a port.
profile: # required - Usage scope and request estimation.
  location: "USA" # optional (default: USA) - ISO 3166-1 alpha-3 (or alpha-2) country code where inference is assumed to occur.
  output-tokens-per-request: 500 # optional (default: 500) - Average output tokens per request, used to derive the request count (must be strictly positive).
  models: # optional (default: all models) - Restricts the usage report to these model identifiers.
    - "claude-opus-5-5"
  workspace-ids: # optional (default: all workspaces) - Restricts the usage report to these workspace identifiers.
    - "wrkspc_01ABC"
  api-key-ids: # optional (default: all API keys) - Restricts the usage report to these API key identifiers.
    - "apikey_01XYZ"
```

### Behavior

- **Services**: each Claude model reported by Anthropic becomes a service of the asset, identified by its model identifier (e.g. `claude-sonnet-5-5`), with `anthropic` as provider.
- **Activity**: only output tokens are accounted for. The request count is estimated as `output tokens / output-tokens-per-request`.
- **Bucketing**: the usage report is fetched in a single paginated call, with daily buckets when the evaluation resolution is a whole number of days and hourly buckets otherwise. Usage is resolved per evaluation slot, the finest granularity being one hour.
- **Empty usage**: time slots without any reported usage produce no activity, so an asset with no usage over the evaluated range yields no evaluation.

## Basic Setup

A minimal configuration accounting for the whole Anthropic organization, with the Admin API key resolved from the secret provider.

```yaml
id: "anthropic-org"
environment-id: "production"
name: "Anthropic Organization"
admin-key: "secrets.anthropic.admin-key"
profile:
  location: "USA"
  output-tokens-per-request: 500
```

## Advanced Setup

### Per-Team Scoping Behind a Corporate Proxy

In an enterprise organization, several teams share the same Anthropic organization through dedicated workspaces. Outbound traffic must go through a corporate HTTP proxy, and each team's footprint is tracked as a separate asset.

The example below demonstrates:
1. **Customer Support Assistant** restricted to its workspace and to the lightweight model it relies on.
2. **R&D Agents** restricted to the API keys used by the research pipelines, with longer generations per request.

```yaml
---
# Asset 1: Customer Support Assistant Workspace
id: "anthropic-support"
environment-id: "production"
name: "Anthropic - Customer Support Assistant"
admin-key: "secrets.anthropic.admin-key"
proxy-url: "http://proxy.corp.internal:8080"
profile:
  location: "USA"
  output-tokens-per-request: 300
  models:
    - "claude-haiku-5-5"
  workspace-ids:
    - "wrkspc_01SupportTeam"

---

# Asset 2: R&D Agents API Keys
id: "anthropic-research"
environment-id: "production"
name: "Anthropic - R&D Agents"
admin-key: "secrets.anthropic.admin-key"
proxy-url: "http://proxy.corp.internal:8080"
profile:
  location: "USA"
  output-tokens-per-request: 2000
  api-key-ids:
    - "apikey_01ResearchPipeline"
    - "apikey_01ResearchEval"
```
