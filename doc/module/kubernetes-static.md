# Kubernetes Static Module

| Property           | Value                                                                |
|--------------------|----------------------------------------------------------------------|
| **Technical Name** | `tech.illuin.wombat-module.kubernetes-static`                        |
| **Family**         | `KUBERNETES_CONTAINER`                                               |
| **Java Class**     | `tech.illuin.wombat.module.kubernetes_static.KubernetesStaticModule` |
| **Components**     | `activity-resolver` (`KubernetesStaticActivityResolver`)             |

## Description

The **Kubernetes Static Module** models Kubernetes cluster workloads and resource consumption without requiring live API connections, agent deployments, or historical metric logs.

Use this module when:
- You want to perform predictive carbon footprint assessments or architectural simulations during system design.
- You need deterministic activity modeling based on anticipated diurnal traffic, business hours, and scheduled batch jobs.
- You want to evaluate hypothetical cluster migrations (e.g., resizing instance types, changing node counts, or switching regions/providers).

Workload activity is resolved by evaluating superposed temporal scenarios (constant loads, daytime user traffic curves, and scheduled batch jobs) at hourly or sub-hourly precision over any requested evaluation time range.

## Configuration Specification

The module registers the asset type `tech.illuin.wombat-module.kubernetes-static` (`KubernetesStaticAsset`) with a profile configuration (`KubernetesStaticProfile`).

For cross-cutting data formats such as `Duration`, `Lifetime`, `Country`, `ServerProvider`, `ZoneId`, `ApplicabilityFilter`, `Curve`, and `Smoothing`, refer to the [Generic Type Glossary](../type-glossary.md).

```yaml
id: "k8s-static-backend" # required - Unique identifier for the asset.
environment-id: "production" # required - Identifier of the environment (e.g. `production`, `simulation`).
name: "Static Backend Service" # required - Human-readable asset name.
profile: # required - Static profile configuration.
  provider: "aws" # required - Infrastructure provider (`aws`, `azure`, `gcp`, `ovhcloud`, `scaleway`).
  instance-type: "c5.2xlarge" # required - Machine instance type (e.g. `c5.2xlarge`, `n2-standard-8`).
  location: "FRA" # required - ISO 3166-1 alpha-3 (or alpha-2) country code (e.g. `FRA`, `USA`, `DEU`).
  lifespan: "5y" # required - Operational lifespan of server hardware (e.g. `5y`, `60m`, `1825d`).
  node-count: 1 # optional (default: 1) - Number of identical worker nodes in the cluster.
  zone-id: "UTC" # optional (default: UTC) - Default timezone for scenario time evaluation (e.g. `Europe/Paris`).
  scenarios: # required - List of superposed load scenario models.
    # Constant scenario
    - type: constant
      load: 0.20 # required - Fractional server CPU load ratio (`0.0`–`1.0`).
      applicability: # optional - Temporal applicability filter.
        days-of-week: [MONDAY, TUESDAY] # optional - Active days of week.
        months: [JANUARY] # optional - Active calendar months.
    # User Activity scenario
    - type: user_activity
      baseline-load: 0.05 # required - Off-hours baseline load ratio (from `0.0` to `1.0`).
      peak-load: 0.75 # required - Peak load ratio during peak hours (from `0.0` to `1.0`).
      opening-time: "08:00:00" # required - Start time of user activity window (in `HH:mm:ss` format)
      closing-time: "19:00:00" # required - End time of user activity window. (in `HH:mm:ss` format)
      curve: bell # optional (default: bell) - Mathematical shape: `bell`, `sinusoidal`, `trapezoidal`.
      jitter: 0.05 # optional (default: 0.0) - Deterministic variance factor (e.g. `0.05` for ±5%).
      applicability: # optional - Temporal applicability filter.
        days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY] # optional - Active days.
    # Batch Activity scenario
    - type: batch_activity
      baseline-load: 0.0 # required - Load ratio outside execution window (`0.0`–`1.0`).
      peak-load: 0.85 # required - Load ratio during active batch processing (`0.0`–`1.0`).
      trigger-time: "02:00:00" # required - Daily start time of the batch job (in `HH:mm:ss` format).
      duration: "PT1H30M" # required - Job duration (ISO-8601).
      smoothing: smooth_step # optional (default: none) - Transition smoothing mode: `none`, `linear_ramp`, `smooth_step`.
      jitter: 0.02 # optional (default: 0.0) - Deterministic variance factor.
      applicability: # optional - Temporal applicability filter.
        days-of-week: [TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY] # optional - Active days.
```

## Basic Setup

A minimal configuration modeling a single node with a static 20% constant baseline load.

```yaml
id: "k8s-static-backend"
environment-id: "production"
name: "Static Backend Service"
profile:
  provider: "aws"
  instance-type: "c5.xlarge"
  location: "FRA"
  lifespan: "5y"
  node-count: 1
  zone-id: "UTC"
  scenarios:
    - type: constant
      load: 0.20
```

## Advanced Setup

### Multi-Faceted Production SaaS Cluster Modeling

In a realistic SaaS deployment, a 3-node Kubernetes cluster experiences multiple overlapping workload patterns:
1. **Background Overhead**: A constant 5% load representing daemonsets, ingress controllers, logging, and monitoring agents.
2. **Weekday User Traffic**: Business hours traffic from 08:30 to 18:30 on weekdays (Monday through Friday) following a bell-curve distribution peaking at 70% with ±5% deterministic jitter.
3. **Nightly Data Sync Batch**: A nightly ETL batch job triggered at 02:00 lasting 90 minutes with 80% peak load and a `smooth_step` transition ramp.
4. **Weekend Database Maintenance**: A heavy weekly re-indexing job running on Sundays at 04:00 for 4 hours with 90% peak load.

```yaml
id: "k8s-saas-prod-cluster"
environment-id: "production"
name: "Production SaaS Application Cluster"
profile:
  provider: "gcp"
  instance-type: "n2-standard-8"
  location: "DEU"
  lifespan: "4y"
  node-count: 3
  zone-id: "Europe/Berlin"
  scenarios:
    # 1. Constant operational background overhead
    - type: constant
      load: 0.05
    # 2. Weekday diurnal user traffic
    - type: user_activity
      baseline-load: 0.05
      peak-load: 0.70
      opening-time: "08:30:00"
      closing-time: "18:30:00"
      curve: "bell"
      jitter: 0.05
      applicability:
        days-of-week: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
    # 3. Nightly ETL sync job
    - type: batch_activity
      baseline-load: 0.0
      peak-load: 0.80
      trigger-time: "02:00:00"
      duration: "PT1H30M"
      smoothing: "smooth_step"
      jitter: 0.02
      applicability:
        days-of-week: [TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY] 
    # 4. Weekly maintenance indexing job on Sundays
    - type: batch_activity
      baseline-load: 0.0
      peak-load: 0.90
      trigger-time: "04:00:00"
      duration: "PT4H"
      smoothing: "linear_ramp"
      applicability:
        days-of-week: [SUNDAY]
```
