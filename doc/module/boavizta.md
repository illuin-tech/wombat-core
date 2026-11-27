# Boavizta Module

| Property           | Value                                                          |
|--------------------|----------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.boavizta`                           |
| **Kind**           | Family module                                                  |
| **Family**         | `KUBERNETES_CONTAINER`                                         |
| **Java Class**     | `tech.illuin.wombat.module.boavizta.BoaviztaModule`            |
| **Components**     | `impact-resolver` (`BoaviztaEvaluationResolver`)               |

## Description

The **Boavizta Module** estimates the multi-criteria environmental impacts (GHG emissions, primary energy, abiotic depletion) of server and container workloads with the [BoaviztAPI](https://github.com/Boavizta/boaviztapi).

It defines no asset-type: it can serve every asset of the `KUBERNETES_CONTAINER` family whose profile is a server profile (`ServerProfile`), unless the asset's own module brings an impact-resolver that accepts it.

## Settings

The module calls the public BoaviztAPI unless given settings (`BoaviztaSettings`), e.g. to reach a self-hosted instance.

```yaml
uri: "https://api.boavizta.org/v1/" # required - Base URI of the BoaviztAPI, version included.
proxy-url: "http://proxy.example.com:3128" # optional - HTTP proxy URL, with a host and a port.
```

## What Wombat derives

The BoaviztAPI gives the impacts of a whole server. Wombat derives the figures it reports from them:

- **Prorated to the time range.** The module asks for the impacts of the server's whole lifespan, then prorates them to the time range, by the range's duration over the lifespan.
- **Rebalanced across services.** Each service gets the cluster's impacts times its share of the cluster's CPU usage, and the asset gets the sum over the services it shows. When an asset has a single service, its share is 1 and nothing is rebalanced.
- **Energy derived from the average power.** The BoaviztAPI has no energy criterion. The module computes the use-phase energy as the average power times the range's duration times the use time ratio, all three from the verbose response. The embedded energy is 0.

The BoaviztAPI's own warnings, and its min and max values, are not passed on.
