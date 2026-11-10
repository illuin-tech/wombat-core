# Wombat Core <img src="https://github.com/illuin-tech/wombat/blob/master/doc/resources/wombat/medium-mask.png?raw=true" align="top" height="40"/>

[![Maven Build](https://github.com/illuin-tech/wombat-core/actions/workflows/maven-build.yml/badge.svg?branch=master)](https://github.com/illuin-tech/wombat-core/actions/workflows/maven-build.yml)
[![Maven Central Version](https://img.shields.io/maven-central/v/tech.illuin/wombat-core)](https://central.sonatype.com/artifact/tech.illuin/wombat-core)
[![javadoc](https://javadoc.io/badge2/tech.illuin/wombat-core/javadoc.svg)](https://javadoc.io/doc/tech.illuin/wombat-core)
[![codecov](https://codecov.io/gh/illuin-tech/wombat-core/graph/badge.svg?token=T141JE2VMY)](https://codecov.io/gh/illuin-tech/wombat-core)
![GitHub](https://img.shields.io/github/license/illuin-tech/wombat-core)

Wombat Core is the Java library powering [Wombat Carbon Tracker](https://github.com/illuin-tech/wombat).

It is distributed separately in order to:
* make it possible to build [custom extensions](https://github.com/illuin-tech/wombat#custom-extensions)
* make it possible to run the core engine [outside of the quarkus app](#custom-core)

## Installation

The library requires Java 21+ and adding the following in your `pom.xml`:

```xml
<dependency>
    <groupId>tech.illuin</groupId>
    <artifactId>wombat-core</artifactId>
    <version>0.18.0</version>
</dependency>
```

Additionally, some optional (but highly recommended) extension libraries can be added, at the time of this writing this includes `wombat-core-module` which, as the name suggests, contains a few implementations enabled by default in the [Wombat application](https://github.com/illuin-tech/wombat).  

## Core Principles

First, you may want to have a look at our [concept definition dump](doc/concept-definition.md) which lists the objects Wombat aims at modeling and manipulating.

From there, we can describe the `wombat-core` library from a more logical point of view ; it consists of a `WombatCore` split into two hemispheres:

* the `AssetMonitor` for aggregating activity data samples ; it is designed to be triggered periodically in an asynchronous fashion and will coordinate metric polling
* the `AssetEvaluator` for making environmental footprint (and financial cost) estimates based on measured and/or modeled activity data

_An overview:_

```mermaid
flowchart LR
    CORE_SETUP[WombatCore\nSetup]
    ASSET_DECLARATION[Asset\nConfiguration]

    subgraph SAMPLING[AssetMonitor: Activity Data Sampling]
        direction LR
        subgraph SOURCE[Wombat Sources]
            direction LR
            SOURCE_ORCHESTRATOR[Source\nOrchestration]
            
            SOURCE_A[WombatSource A]
            SOURCE_B[WombatSource B]
            SOURCE_C[WombatSource C]

            SOURCE_ORCHESTRATOR-->SOURCE_A
            SOURCE_ORCHESTRATOR-->SOURCE_B
            SOURCE_ORCHESTRATOR-->SOURCE_C
        end
        STORAGE_ACTIVITY[(Metrics Store)]

        SOURCE==>STORAGE_ACTIVITY
    end

    CALL{{Evaluation call}}

    subgraph ESTIMATION[AssetEvaluator: Activity Data Evaluation]
        direction LR

        subgraph ACTIVITY[Wombat Activity Resolvers]
            direction LR
            ACTIVITY_RESOLVER_A[WombatActivity\nResolver A]
            ACTIVITY_RESOLVER_B[WombatActivity\nResolver B]
        end

        subgraph IMPACT_FINOPS[Wombat Cost Resolvers]
            direction LR
            FINOPS[Cost Resolving]
            FINOPS --> COST_EVAL_RESOLVER_A[WombatEvaluation\nResolver C]
            FINOPS --> COST_EVAL_RESOLVER_B[WombatEvaluation\nResolver D]
        end

        subgraph IMPACT_GREENOPS[Wombat Impact Resolvers]
            direction LR
            GREENOPS[Impact Resolving]
            GREENOPS --> IMPACT_EVAL_RESOLVER_A[WombatEvaluation\nResolver A]
            GREENOPS --> IMPACT_EVAL_RESOLVER_B[WombatEvaluation\nResolver B]
        end

        ACTIVITY ==>|GreenOps| IMPACT_GREENOPS
        ACTIVITY ==>|FinOps| IMPACT_FINOPS
    end

    GREENOPS_RESULT{{Multi-criteria Estimate\nkgCO2eq / MJ / kgSbeq}}
    FINOPS_RESULT{{Cost Estimate\n€ / $ / ¥}}

    %% Cross-Process Data Flows
    CORE_SETUP ==> ASSET_DECLARATION
    ASSET_DECLARATION ==> SAMPLING
    SAMPLING -.-> CALL
    CALL ==> ACTIVITY
    IMPACT_GREENOPS ==> GREENOPS_RESULT
    IMPACT_FINOPS ==> FINOPS_RESULT

    classDef configStyle fill:#cfd8dc88,stroke:#78909c,stroke-width:2px,color:#000
    classDef samplingStyle fill:#e3f2fd88,stroke:#64b5f6,stroke-width:2px,color:#000
    classDef triggerStyle fill:#ef5350ee,stroke:#c62828,stroke-width:2px,color:#fff
    classDef activityStyle fill:#ffe0b288,stroke:#ff9800,stroke-width:2px,color:#000
    classDef greenopsStyle fill:#e8f5e988,stroke:#81c784,stroke-width:2px,color:#000
    classDef finopsStyle fill:#fff9c488,stroke:#fdd835,stroke-width:2px,color:#000

    class CONFIG configStyle
    class SOURCE samplingStyle
    class CALL triggerStyle
    class ACTIVITY activityStyle
    class IMPACT_GREENOPS greenopsStyle
    class IMPACT_FINOPS finopsStyle
    class GREENOPS_RESULT greenopsStyle
    class FINOPS_RESULT finopsStyle
``` 

The `WombatCore` is configured by way of `WombatModules` which are declarative structures exposing one or more [subcomponents](#core-components).

There are ["core" modules](#core-modules) which are shipped as part of the `wombat-core-module` library, but you can create [custom modules](#custom-module) in order to accommodate your requirements. 

### Core Components

Wombat relies on three fundamental component interfaces representing distinct stages of the collection and evaluation pipeline:
* `WombatSource` are triggered periodically by the `AssetMonitor`, they are expected to poll telemetry endpoints (e.g. Kubernetes API, Prometheus metrics, etc.) and returns `MetricData` to be persisted
* `WombatActivityResolver` are activity data aggregators. Given an `Asset`, a `TimeRange` and an optional `AssetFilter`, it computes a standardized representation of `AssetActivity` with `ActivityData` data-points (e.g. CPU nanocores, LLM token counts) from historical metrics or modeled profiles
* `WombatEvaluationResolver` are evaluation engines. Given an `Asset` and its resolved `ActivityData`, it computes either multi-criteria environmental footprints (`AssetImpact`) or financial costs (`AssetCost`).

### Core Modules

Core modules provide ready-to-use implementations of asset types and their associated components. For details on shared data structures, time formats, and enums used across module configurations, see the [Generic Type Glossary](doc/type-glossary.md).

Below is a list of currently implemented core modules shipped as part of the `wombat-core-module` library. Follow the links to each of their dedicated documentation for more information on how to use them.

| Module                                                                                 | Family                 | Description                                                                                                          |
|----------------------------------------------------------------------------------------|------------------------|----------------------------------------------------------------------------------------------------------------------|
| [`tech.illuin.wombat-module.kubernetes-api`](doc/module/kubernetes-api.md)             | `KUBERNETES_CONTAINER` | Connects to Kubernetes cluster APIs to poll CPU and memory resource metrics.                                         |
| [`tech.illuin.wombat-module.kubernetes-static`](doc/module/kubernetes-static.md)       | `KUBERNETES_CONTAINER` | Models static Kubernetes container workloads using node counts and superposed temporal scenarios.                    |
| [`tech.illuin.wombat-module.llm-prometheus`](doc/module/llm-prometheus.md)             | `LLM`                  | Queries Prometheus via PromQL for LLM inference telemetry.                                                           |
| [`tech.illuin.wombat-module.llm-static`](doc/module/llm-static.md)                     | `LLM`                  | Models static LLM workloads based on annual request profiles.                                                        |
| [`tech.illuin.wombat-module.llm-anthropic`](doc/module/llm-anthropic.md)               | `LLM`                  | Queries the Anthropic Admin usage report on demand for per-model token usage over the evaluated time range.          |
| [`tech.illuin.wombat-module.llm-vertexai`](doc/module/llm-vertexai.md)                 | `LLM`                  | Queries the VertexAI usage report on demand for per-model token usage over the evaluated time range.                 |

#### Default Components

While `WombatModules` are allowed to declare specific [asset types](doc/concept-definition.md#technical-concepts) and introduce a complete pipeline for resolving activity and making impact estimates, most of the time you will only want an override for a single part of the pipeline, like a `WombatSource`, or a `WombatActivityResolver`.

For instance, if you need to sample LLM service data from a specific database, you will only be concerned about a `WombatSource` implementation.

In such cases, the rest of the pipeline will rely on "default components" based on the [service family](doc/type-glossary.md#servicefamily).
Basically, for each part of the pipeline [described earlier](#core-principles) there are two scenarios:

* either the type of the `Asset` being evaluated requires its own component as declared by its associated `WombatModule`, and the engine will use that
* or, the type of the `Asset` being evaluated has no such requirement, in which case the corresponding default for this `Asset`'s family will be used

At the time of this writing, the `wombat-core` library ships with the following default component implementations:

For the `KUBERNETES_CONTAINER` family:
* Activity Resolvers:
  * `KubernetesActivityResolver` aggregates historical container and node metrics from a metric store
* Evaluation Resolvers:
  * `BoaviztaEvaluationResolver` estimates multi-criteria environmental impacts (GHG emissions, primary energy, abiotic depletion) for server and container workloads using the Boavizta API

For the `LLM` family:
* Activity Resolvers:
  * `LLMActivityResolver` aggregates historical LLM request activity and token consumption from a metric store
* Evaluation Resolvers:
  * `EcologitsEvaluationResolver` estimates multi-criteria environmental footprints for AI/LLM inferences using EcoLogits

## How to Use

Following are a few code samples demonstrating basic usage of the `wombat-core` library.

### Custom Core

[Wombat, the application](https://github.com/illuin-tech/wombat), obviously ships with its own `WombatCore` configured from [core-modules](#core-modules) and eventually user-provided [extensions](https://github.com/illuin-tech/wombat#custom-extensions).

But it is also possible to create your own `WombatCore` for use outside the application!

#### The `WombatContextProvider`

First, you will need to implement one critical component: the `WombatContextProvider`.
The role of the `WombatContextProvider` is to inform the `WombatCore` about the systems it needs to monitor and manipulate. The context can expose:

* one or several `Environment`: you can think of an `Environment` as a "complete" application with app containers, db containers, LLM services being used, etc.
* each of these `Environment` can have one or several `Asset` registered in it: an `Asset` is a method of modeling services we need to measure/estimate, for example you can have an `Asset` that will make Wombat observe containers in a given K8S namespace

For instance, the `AssetMonitor`'s job (see [above](#core-principles)) is to ask the `WombatContext` to list the `Asset` it knows about, and for each monitorable asset, it will trigger the corresponding module's `WombatSource` at a regular interval.

[Wombat, the application](https://github.com/illuin-tech/wombat) ships with its own `WombatContextProvider` implementation backed by a database, which could be replicated fairly easily. For the sake of this documentation we'll do simpler than that and compute a static context:

```java
public class MyContextProvider implements WombatContextProvider
{
    @Override
    public WombatContext provide()
    {
        return new ResolvedContext(new Environment("sample", List.of(
            /* Create your assets here (e.g. Kubernetes, LLM), see #core-modules */
        )));
    }
}
```

#### The `WombatMetricPersister`

Next, the other critical component is the `WombatMetricPersister`: this is where sampled data-points go when sources are triggered. `wombat-core` ships with an in-memory [Micrometer-based](https://micrometer.io) implementation which you can use.

As with any other Micrometer setup, the metrics can be plugged into a prometheus or other time-series database of your choice.

```java
/* io.micrometer.core.instrument.MeterRegistry meterRegistry */
WombatMetricPersister metricPersister = new MicrometerMetricPersister(meterRegistry);
```

On the other end of the persistence equation, are the components responsible for reading the data-points:

* `KubernetesMetricResolver` for resolving kubernetes-container related data samples
* `LLMMetricResolver` for resolving LLM services related data samples

Similarly, `wombat-core` ships with in-memory Micrometer-based implementations, while the [Wombat, the application](https://github.com/illuin-tech/wombat) has database-backed implementations.

```java
/* io.micrometer.core.instrument.MeterRegistry meterRegistry */
KubernetesMetricResolver k8sMetricResolver = new MicrometerKubernetesMetricResolver(meterRegistry);
LLMMetricResolver llmMetricResolver = new MicrometerLLMMetricResolver(meterRegistry);
```

#### Now, assemble the `WombatCore`

```java
/* tech.illuin.wombat.core.connector.boavizta.connector.BoaviztaClient boaviztaClient */
/* tech.illuin.wombat.core.connector.ecologits.connector.EcologitsClient ecologitsClient */

try (WombatCore core = new WombatCore(
    contextProvider,
    metricPersister,
    List.of(
        new KubernetesAPIModule(),
        new KubernetesStaticModule(),
        new LLMStaticModule()
    ),
    defaults -> {
        defaults.register(KUBERNETES_CONTAINER, new KubernetesActivityResolver(k8sMetricResolver), new BoaviztaEvaluationResolver(boaviztaClient), null);
        defaults.register(LLM, new LLMActivityResolver(llmMetricResolver), new EcologitsEvaluationResolver(ecologitsClient), null);
    }
)) {
    /* The evaluator is stateless, you should reuse it throughout your app ; e.g. make it injectable if applicable */
    AssetEvaluator evaluator = core.createEvaluator();
    
    Instant now = Instant.now();
    TimeRange range = new TimeRange(now.minus(Duration.ofHours(24)), now);
    List<AssetEvaluation> results = evaluator.evaluate(range, AssetFilter.none());
}
```

### Custom Module

Custom modules enable tracking domain-specific assets , integrating custom activity modeling or even custom impact/cost resolving. To create a custom module, define an `Asset` with its profile, implement `WombatModule`, and provide the relevant resolvers (such as a `WombatActivityResolver`).

Here is an example of a custom module providing modeled activity data for LLM traffic:

```java
public class MySimulationModule implements WombatModule
{
    public static final AssetType TYPE = AssetType.of(
        "com.example", "wombat-module", "my-simulated-llm", ActivityRegime.MODELED, ServiceFamily.LLM
    );

    @Override
    public AssetType type() { return TYPE; }

    @Override
    public Class<? extends Asset> assetClass() { return MySimulationAsset.class; }

    @Override
    public Optional<WombatActivityResolver> createActivityResolver(WombatContext ctx)
    {
        return Optional.of(new MySimulationActivityResolver());
    }
}

public record MySimulationAsset(
    AssetIdentity identity,
    MySimulationProfile profile
) implements Asset {
    @Override
    public AssetType type() { return MySimulationModule.TYPE; }

    public record MySimulationProfile(
        LLMProvider provider,
        String model,
        Country location,
        long baseTokens
    ) implements LLMProfile {}
}

public class MySimulationActivityResolver implements WombatActivityResolver
{
    @Override
    public boolean accept(Asset asset)
    {
        return asset instanceof MySimulationAsset;
    }

    @Override
    public Optional<AssetActivity> resolve(Asset asset, TimeRange range, AssetFilter filter)
    {
        MySimulationProfile profile = ((MySimulationAsset) asset).profile();
        String serviceId = asset.identity().id();

        List<ActivityData> activities = new ArrayList<>();
        // Split the target time range into discrete slots matching its requested resolution
        for (TimeRange slot : range.split())
        {
            // Diurnal traffic variation (sinusoidal peak during daytime hours)
            int hour = slot.start().atZone(ZoneOffset.UTC).getHour();
            double diurnalFactor = 0.5 + 0.5 * Math.sin(Math.PI * (hour - 8) / 12.0);

            // Random jitter (±10%)
            double jitter = 1.0 + (ThreadLocalRandom.current().nextGaussian() * 0.1);
            long simulatedTokens = (long) (profile.baseTokens() * Math.max(0.1, diurnalFactor * jitter));
            double simulatedRequests = simulatedTokens / 200.0;

            LLMServiceActivity service = new LLMServiceActivity(
                profile.provider(),
                profile.model(),
                profile.location(),
                simulatedTokens,
                simulatedRequests
            );

            activities.add(new LLMActivityData(
                ActivityRegime.MODELED,
                Set.of(serviceId),
                slot,
                Map.of(serviceId, service)
            ));
        }

        return Optional.of(new AssetActivity(
            asset.identity(),
            activities
        ));
    }
}
```

Once defined, you can register this module in your `WombatCore` as seen [above](#custom-core) or in a Wombat app as documented [here](https://github.com/illuin-tech/wombat#custom-extensions).

## How to build

Building the project requires a Java 21+ JDK, in order to compile it:

```bash
mvn clean compile
```

Run all tests:

```bash
mvn clean test
```

Package it (as a .jar in `target/`):

```bash
mvn clean package
```

To package it without running tests, append `-DskipTests`:

```bash
mvn clean package -DskipTests
```

## License

This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0) ; see the [LICENSE](LICENSE) file for details.

Copyright (c) 2026 Illuin Technology.
