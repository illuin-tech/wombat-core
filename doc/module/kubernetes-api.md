# Kubernetes API Module

| Property           | Value                                                          |
|--------------------|----------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.kubernetes-api`                     |
| **Family**         | `KUBERNETES_CONTAINER`                                         |
| **Java Class**     | `tech.illuin.wombat.module.kubernetes_api.KubernetesAPIModule` |
| **Components**     | `source` (`KubernetesAPISource`), `profileResolver` (`KubernetesProfileResolver`) |

## Description

The **Kubernetes API Module** connects directly to Kubernetes cluster APIs via standard kubeconfig configurations to collect live CPU and memory metrics from running pods and nodes.

Use this module when:
- You have live Kubernetes clusters (on-premises or managed cloud providers like AWS EKS, Google GKE, Azure AKS, OVHcloud, or Scaleway).
- You want automated, scheduled telemetry ingestion into Wombat's metric store without needing intermediary metric scrapers.
- You need fine-grained namespace scoping (a whitelist of namespaces per cluster) or multi-cluster telemetry aggregation.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.kubernetes-api` (`KubernetesAPIAsset`) with an associated server profile (`KubernetesAPIServerProfile`), declared by the user or [inferred from the cluster](#inferred).

For cross-cutting data formats such as `Period`, `Duration`, `Lifetime`, `Country`, `ServerProvider` and `SecretKey`, refer to the [Generic Type Glossary](../type-glossary.md).

The secret referenced by `kubeconfig-key` must resolve to the full YAML content of a kubeconfig. It is parsed in memory, with no file behind it: certificates and keys must be inlined (`certificate-authority-data`, `client-certificate-data`, `client-key-data`), since relative file references have nothing to resolve against. The secret is read again on every poll and every profile inference: a rotated kubeconfig is used from the next one on, and one that cannot be used fails them until it is fixed.

With an empty `namespaces` whitelist, pod metrics are listed cluster-wide: the kubeconfig needs cluster-scoped read access to `pods.metrics.k8s.io`, and the namespace of its context is not used. For a kubeconfig restricted to some namespaces, list them in `namespaces`. A whitelisted namespace whose metrics cannot be listed, e.g. one the kubeconfig cannot read, is skipped with a warning while the others are still polled.

```yaml
id: "k8s-prod-cluster-01" # required - Unique identifier for the monitored asset.
environment-id: "production" # required - Identifier of the target environment (e.g. `production`, `staging`).
kubeconfig-key: "secrets.kubernetes.prod" # required - Secret holding the kubeconfig contents, resolved by the configured secret resolver.
namespaces: # optional (default: empty) - Whitelist of Kubernetes namespaces to poll (polls all namespaces if empty or omitted).
  - "payments-api"
  - "payments-worker"
context: "prod-cluster-ctx" # optional (default: active context) - Kubeconfig context name to target.
read-timeout: "PT10S" # optional (default: client default) - HTTP read timeout for Kubernetes API requests.
period: "5m" # optional (default: monitor default) - Polling interval for metric sampling (e.g. `30s`, `1m`, `5m`, `1h`).
profile: # optional (default: inferred from the cluster) - Server hardware profile configuration, a cloud instance type, a bare server specification or an inferred one (see Profile Types).
  type: "cloud" # optional (default: `cloud`) - Profile type: `cloud` for a provider's instance type, `server` for a bare server specification, `inferred` to read it from the cluster.
  provider: "aws" # required - Infrastructure provider (`aws`, `azure`, `gcp`, `ovhcloud`, `scaleway`).
  instance-type: "m5.2xlarge" # required - Machine or virtual instance specification (e.g. `m5.xlarge`, `n2-standard-4`).
  location: "FRA" # required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `FRA`, `USA`, `DEU`).
  lifespan: "5y" # required - Expected operational lifespan of server hardware (e.g. `5y`, `60m`, `1825d`).
```

## Profile Types

The `profile.type` property selects how the nodes' hardware is described. Profiles without a `type` are `cloud` ones, and an omitted `profile` is an `inferred` one.

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

### `inferred`

The nodes' hardware is read from the cluster, through the asset's kubeconfig, each time the asset is evaluated (the result is kept for an hour). Only the hardware is inferred: `location` and `lifespan` can still be set, and win over the defaults.

```yaml
id: "k8s-prod-cluster-01" # required - Unique identifier for the monitored asset.
environment-id: "production" # required - Identifier of the target environment.
kubeconfig-key: "secrets.kubernetes.prod" # required - Secret holding the kubeconfig contents.
profile: # optional - Omitting it entirely is the same as `type: inferred` with no other property.
  type: "inferred" # required - Selects the inferred profile.
  location: "FRA" # optional (default: `XXX`, the world average) - ISO 3166-1 alpha-3 (or alpha-2) country code.
  lifespan: "5y" # optional (default: `5y`) - Expected operational lifespan of server hardware.
```

The profile is read from the first of these sources the kubeconfig can read:

| Order | Source                                                          | Profile                                                                                                                                               | Needs                                                  |
|-------|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| 1     | The nodes                                                       | `cloud`, of the nodes' provider and instance type; `server` of their capacity when the provider is unknown                                             | `get`, `list` on `nodes` (cluster-wide)                 |
| 2     | The resource quotas of the polled namespaces                    | `server` of the quotas' total CPU and memory, requests before limits; skipped when all namespaces are polled, or a namespace has no CPU quota          | `list` on `resourcequotas` in the polled namespaces     |
| 3     | The containers of the running pods of the polled namespaces     | `server` of the pods' total requests, or else limits, counted as Kubernetes does (sidecars, init containers, overhead)                                 | `list` on `pods` in the polled namespaces              |

- **Provider.** It is read from the nodes' `spec.providerID` (`aws://`, `gce://`, `azure://`, `scaleway://`), else from provider-specific node labels (EKS, GKE, AKS, OVHcloud, Scaleway), else from the host name of a managed API server (`*.eks.amazonaws.com`, `*.azmk8s.io`, `*.gke.goog`, `*.k8s.ovh.net`, `*.scw.cloud`). A cluster whose provider cannot be told gets a `server` profile, never a `cloud` one.
- **Instance type.** It is read from the `node.kubernetes.io/instance-type` label. Nodes of several kinds are profiled after the most common one.
- **Slices.** Quotas and containers size a slice of the cluster, not its nodes, as a single server. When they set CPU but no memory, 4 GiB per core is assumed. Their disks being unknown, Boavizta's default (one 1 TB SSD) applies.
- **What is assumed.** Each evaluation tells where its profile comes from (`profileOrigin`: `DECLARED` for a declared profile, else this module's `KubernetesProfileOrigin`: `NODES`, `RESOURCE_QUOTAS`, `CONTAINER_REQUESTS` or `CONTAINER_LIMITS`) and what the inference had to assume (`warnings`), both shown on the impact dashboard.
- **Failures.** When no source can be read, the asset is left out of the evaluation, with the reason of each source, e.g. `Cannot infer the server profile: nodes unreadable (forbidden to list nodes); no CPU quota in namespace payments; pods unreadable (forbidden to list pods)`. The impact API lists such assets in its `status.metadata.failures`.
- **Distributed setups.** The profile is inferred where the asset is evaluated, i.e. by the aggregator, which has no kubeconfig: assets polled by workers need a declared profile.

All of these permissions are read-only. The built-in `view` ClusterRole, bound in the polled namespaces, covers the quotas and the containers; reading the nodes needs a ClusterRole of its own:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: wombat-node-reader
rules:
  - apiGroups: [""]
    resources: ["nodes"]
    verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: wombat-node-reader
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: wombat-node-reader
subjects:
  - kind: ServiceAccount
    name: wombat # the identity of the asset's kubeconfig
    namespace: wombat
```

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

Without a `profile`, the nodes' hardware is [inferred from the cluster](#inferred), in the world and over 5 years:

```yaml
id: "k8s-prod-cluster-01"
environment-id: "production"
kubeconfig-key: "secrets.kubernetes.prod"
period: "5m"
```

## Advanced Setup

### Multi-Cluster & Heterogeneous Cloud Infrastructure

In complex enterprise architectures, services often run across multiple distinct Kubernetes clusters spanning different cloud providers, environments, and geographical regions. A single shared `kubeconfig` bundle can contain contexts for different clusters.

The scenario below demonstrates:
1. **Frontend / API Cluster on AWS EKS** (`FRA`): Scoped strictly to the `payments-api` namespace (`namespaces` whitelist) with 1-minute polling and custom timeout.
2. **Data Processing Cluster on GCP GKE** (`DEU`): Monitored across all namespaces with a 15-second read timeout and 2-minute polling.

```yaml
---
# Asset 1: AWS EKS Payments Service Cluster
id: "k8s-aws-fra-payments"
environment-id: "production-eu"
kubeconfig-key: "secrets.kubernetes.enterprise"
context: "arn:aws:eks:eu-west-3:123456789012:cluster/prod-fra-payments"
namespaces:
  - "payments-api"
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
