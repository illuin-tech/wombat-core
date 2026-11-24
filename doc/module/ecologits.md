# Ecologits Module

| Property           | Value                                                          |
|--------------------|----------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.ecologits`                          |
| **Type**           | Family module (`FamilyType`)                                   |
| **Family**         | `LLM`                                                          |
| **Java Class**     | `tech.illuin.wombat.module.ecologits.EcologitsModule`          |
| **Components**     | `impact-resolver` (`EcologitsEvaluationResolver`)              |

## Description

The **Ecologits Module** estimates the multi-criteria environmental footprint of LLM inferences with the [EcoLogits](https://ecologits.ai/) API.

It defines no asset-type: it serves every asset of the `LLM` family whose profile is an LLM profile (`LLMProfile`), unless the asset's own module brings an impact-resolver that accepts it. A model EcoLogits does not know is left out of the asset's impact.

## Settings

The module calls the public EcoLogits API unless given settings (`EcologitsSettings`), e.g. to reach a self-hosted instance.

```yaml
uri: "https://api.ecologits.ai/" # optional (default: https://api.ecologits.ai/) - Base URI of the Ecologits API.
proxy-url: "http://proxy.example.com:3128" # optional - HTTP proxy URL, with a host and a port.
```
