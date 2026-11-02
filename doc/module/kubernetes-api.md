# Kubernetes API Module

| Property           | Value                                                          |
|--------------------|----------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.kubernetes-api`                     |
| **Family**         | `KUBERNETES_CONTAINER`                                         |
| **Java Class**     | `tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule` |
| **Components**     | `source` (`KubernetesAPISource`)                               |

## Description

The **Kubernetes API Module** connects directly to Kubernetes cluster APIs via standard kubeconfig configurations to collect live CPU and memory metrics from running pods and nodes.

Use this module when:
- You have live Kubernetes clusters (on-premises or managed cloud providers like AWS EKS, Google GKE, Azure AKS, OVHcloud, or Scaleway).
- You want automated, scheduled telemetry ingestion into Wombat's metric store without needing intermediary metric scrapers.
- You need fine-grained namespace scoping or multi-cluster telemetry aggregation.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.kubernetes-api` (`KubernetesAPIAsset`) with an associated server profile (`KubernetesAPIServerProfile`).

For cross-cutting data formats such as `Period`, `Duration`, `Lifetime`, `Country`, and `ServerProvider`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
id: "k8s-prod-cluster-01" # `String`, Required - Unique identifier for the monitored asset.
environment-id: "production" # `String`, Required - Identifier of the target environment (e.g. `production`, `staging`).
name: "Production Primary K8s Cluster" # `String`, Required - Human-readable asset name.
config-path: "/etc/wombat/kubeconfig" # `String`, Required - Absolute or relative file path to the kubeconfig file.
namespace: "payments-api" # `String`, Optional(default: null) - Kubernetes namespace to restrict metric polling (polls all namespaces if omitted).
context: "prod-cluster-ctx" # `String`, Optional(default: active context) - Kubeconfig context name to target.
read-timeout: "PT10S" # `Duration`, Optional(default: client default) - HTTP read timeout for Kubernetes API requests.
period: "5m" # `Period`, Optional(default: monitor default) - Polling interval for metric sampling (e.g. `30s`, `1m`, `5m`, `1h`).
profile: # `KubernetesAPIServerProfile`, Required - Server hardware profile configuration.
  provider: "aws" # `ServerProvider`, Required - Infrastructure provider (`aws`, `azure`, `gcp`, `ovhcloud`, `scaleway`).
  instance-type: "m5.2xlarge" # `String`, Required - Machine or virtual instance specification (e.g. `m5.xlarge`, `n2-standard-4`).
  location: "FRA" # `Country`, Required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `FRA`, `USA`, `DEU`).
  lifespan: "5y" # `Lifetime`, Required - Expected operational lifespan of server hardware (e.g. `5y`, `60m`, `1825d`).
```

## Basic Setup

A minimal configuration monitoring a single local or standard Kubernetes cluster using default credentials and polling all namespaces every 5 minutes.

```yaml
id: "k8s-prod-cluster-01"
environment-id: "production"
name: "Production Primary K8s Cluster"
config-path: "/etc/wombat/kubeconfig"
period: "5m"
profile:
  provider: "aws"
  instance-type: "m5.2xlarge"
  location: "FRA"
  lifespan: "5y"
```

## Advanced Setup

### Multi-Cluster & Heterogeneous Cloud Infrastructure

In complex enterprise architectures, services often run across multiple distinct Kubernetes clusters spanning different cloud providers, environments, and geographical regions. A single shared `kubeconfig` bundle can contain contexts for different clusters.

The scenario below demonstrates:
1. **Frontend / API Cluster on AWS EKS** (`FRA`): Scoped strictly to the `payments-api` namespace with 1-minute polling and custom timeout.
2. **Data Processing Cluster on GCP GKE** (`DEU`): Monitored across all namespaces with a 15-second read timeout and 2-minute polling.

```yaml
---
# Asset 1: AWS EKS Payments Service Cluster
id: "k8s-aws-fra-payments"
environment-id: "production-eu"
name: "AWS EKS Payments Cluster"
config-path: "/var/secrets/k8s/kubeconfig-enterprise.yaml"
context: "arn:aws:eks:eu-west-3:123456789012:cluster/prod-fra-payments"
namespace: "payments-api"
read-timeout: "PT10S"
period: "1m"
profile:
  provider: "aws"
  instance-type: "c5.2xlarge"
  location: "FRA"
  lifespan: "4y"

---

# Asset 2: GCP GKE Analytics & Batch Cluster
id: "k8s-gcp-deu-analytics"
environment-id: "production-eu"
name: "GCP GKE Analytics Processing Cluster"
config-path: "/var/secrets/k8s/kubeconfig-enterprise.yaml"
context: "gke_enterprise-analytics_europe-west3_prod-batch-cluster"
read-timeout: "PT15S"
period: "2m"
profile:
  provider: "gcp"
  instance-type: "n2-highcpu-16"
  location: "DEU"
  lifespan: "5y"
```
