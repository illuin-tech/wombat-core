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
