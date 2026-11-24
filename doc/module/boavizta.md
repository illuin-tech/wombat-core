# Boavizta Module

| Property           | Value                                                          |
|--------------------|----------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.boavizta`                           |
| **Type**           | Family module (`FamilyType`)                                   |
| **Family**         | `KUBERNETES_CONTAINER`                                         |
| **Java Class**     | `tech.illuin.wombat.module.boavizta.BoaviztaModule`            |
| **Components**     | `impact-resolver` (`BoaviztaEvaluationResolver`)               |

## Description

The **Boavizta Module** estimates the multi-criteria environmental impacts (GHG emissions, primary energy, abiotic depletion) of server and container workloads with the [BoaviztAPI](https://github.com/Boavizta/boaviztapi).

It defines no asset-type: it serves every asset of the `KUBERNETES_CONTAINER` family whose profile is a server profile (`ServerProfile`), unless the asset's own module brings an impact-resolver that accepts it.

## Settings

The module calls the public BoaviztAPI unless given settings (`BoaviztaSettings`), e.g. to reach a self-hosted instance.

```yaml
uri: "https://api.boavizta.org/v1/" # optional (default: https://api.boavizta.org/v1/) - Base URI of the BoaviztAPI, version included.
proxy-url: "http://proxy.example.com:3128" # optional - HTTP proxy URL, with a host and a port.
```
