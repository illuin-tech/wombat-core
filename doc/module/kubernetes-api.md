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

For cross-cutting data formats such as `Period`, `Duration`, `Lifetime`, `Country`, `ServerProvider` and `SecretKey`, refer to the [Generic Type Glossary](../type-glossary.md).

The secret referenced by `kubeconfig-key` must resolve to the full YAML content of a kubeconfig. It is parsed in memory, with no file behind it: certificates and keys must be inlined (`certificate-authority-data`, `client-certificate-data`, `client-key-data`), since relative file references have nothing to resolve against.

```yaml
id: "k8s-prod-cluster-01" # required - Unique identifier for the monitored asset.
environment-id: "production" # required - Identifier of the target environment (e.g. `production`, `staging`).
kubeconfig-key: "secrets.kubernetes.prod" # required - Secret holding the kubeconfig contents, resolved by the configured secret resolver.
namespace: "payments-api" # optional (default: null) - Kubernetes namespace to restrict metric polling (polls all namespaces if omitted).
context: "prod-cluster-ctx" # optional (default: active context) - Kubeconfig context name to target.
read-timeout: "PT10S" # optional (default: client default) - HTTP read timeout for Kubernetes API requests.
period: "5m" # optional (default: monitor default) - Polling interval for metric sampling (e.g. `30s`, `1m`, `5m`, `1h`).
profile: # required - Server hardware profile configuration, a cloud instance type or a bare server specification (see Profile Types).
  type: "cloud" # optional (default: `cloud`) - Profile type: `cloud` for a provider's instance type, `server` for a bare server specification.
  provider: "aws" # required - Infrastructure provider (`aws`, `azure`, `gcp`, `ovhcloud`, `scaleway`).
  instance-type: "m5.2xlarge" # required - Machine or virtual instance specification (e.g. `m5.xlarge`, `n2-standard-4`).
  location: "FRA" # required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `FRA`, `USA`, `DEU`).
  lifespan: "5y" # required - Expected operational lifespan of server hardware (e.g. `5y`, `60m`, `1825d`).
```

## Profile Types

The `profile.type` property selects how the nodes' hardware is described. Profiles without a `type` are `cloud` ones.

### `cloud`

The nodes are instances of a cloud provider's catalog: `provider` and `instance-type` identify them in the Boavizta dataset, which supplies their hardware and vCPU count. Its properties are listed in the configuration specification above.

### `server`

The nodes are bare servers, specified component by component: on-premises machines, or providers and instance types the Boavizta dataset does not cover. Their impacts come from Boavizta's server endpoint.

```yaml
id: "k8s-onprem-cluster" # required - Unique identifier for the monitored asset.
environment-id: "production" # required - Identifier of the target environment.
kubeconfig-key: "secrets.kubernetes.onprem" # required - Secret holding the kubeconfig contents.
profile: # required - Server hardware profile configuration.
  type: "server" # required - Selects the bare server specification.
  location: "FRA" # required - ISO 3166-1 alpha-3 (or alpha-2) country code.
  lifespan: "5y" # required - Expected operational lifespan of server hardware.
  cpu: # required - Processors of a node.
    units: 2 # required - Number of processors.
    core-units: 24 # required - Number of cores per processor.
    threads-per-core: 2 # optional (default: 2) - Hardware threads per core.
    name: "Intel Xeon Gold 6342" # optional (default: Boavizta's default) - Processor model.
  ram: # required - Memory of a node.
    units: 8 # required - Number of memory modules.
    capacity: 32 # required - Capacity of a module, in GB.
  ssd: # optional (default: Boavizta's default, one 1 TB SSD) - SSDs of a node.
    units: 2 # required - Number of disks.
    capacity: 960 # required - Capacity of a disk, in GB.
  hdd: # optional (default: none) - HDDs of a node.
    units: 1 # required - Number of disks.
    capacity: 4000 # required - Capacity of a disk, in GB.
  gpu: # optional (default: none) - GPUs of a node.
    units: 1 # required - Number of GPUs.
    name: "NVIDIA A100" # optional (default: Boavizta's default) - GPU model.
```

A node's vCPU count, against which the measured CPU usage is set, is `units` × `core-units` × `threads-per-core`: 96 in the example above.

## Basic Setup

A minimal configuration monitoring a single local or standard Kubernetes cluster, polling all namespaces every 5 minutes.

```yaml
id: "k8s-prod-cluster-01"
environment-id: "production"
kubeconfig-key: "secrets.kubernetes.prod"
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
kubeconfig-key: "secrets.kubernetes.enterprise"
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
kubeconfig-key: "secrets.kubernetes.enterprise"
context: "gke_enterprise-analytics_europe-west3_prod-batch-cluster"
read-timeout: "PT15S"
period: "2m"
profile:
  provider: "gcp"
  instance-type: "n2-highcpu-16"
  location: "DEU"
  lifespan: "5y"
```
