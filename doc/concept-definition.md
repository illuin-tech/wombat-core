# Wombat Concept Definition Dump

This document serves as a reference for the definition of high level core concepts modeled in `wombat-core`.
Some concepts may not have a matching technical / operational object, but they are useful for understanding what Wombat attempts to do. 

## High-level Concepts

* `System`: a group of entities that performs cohesive business tasks and has environmental footprints (eg. *Chatbot ${name} for ${businessEntity}*, *App for doing ${action} at ${client}*)
* `Project`: an organizational grouping of Systems, relevant for governance and feature design
* `Object`: a concrete entity with a measurable environmental footprint (eg. a container, a deployed model, a SaaS endpoint), this is what we want to measure, model, sum and split in various ways in order to get the environmental footprint of a System
* `Service`: a logical entity representing the use of an Object attributable to a System (eg. could be 100% of a container, or a volume-based estimate for a given SaaS)

## Technical Concepts

* `Environment`: maps to an instance of a System, a given project can have several environments (e.g. TEST, PREPROD, PROD), they consist of one or more `Assets`
* `WombatSource`: a specific way to retrieve `Service` activity/usage/footprint data from an external source, or model that data out of configuration files if the data doesn't exist / isn't accessible (e.g. K8S API source, prometheus LLM behaviour source, modeled LLM behaviour source, etc.)
* `Asset`: a sourced/modeled representation of one or more `Services`, they may point at all or some of the services in one or more namespaces (in a given K8S cluster), they may have specific modeled behaviour assumptions (eg. LLM behaviour) ; in a way, they're a Source configuration
 