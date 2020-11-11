# Research: Persist Dynamic Configuration 

### Table of Contents

### Summary

This work is to research what is the best way for persisting dynamic configuration and loading configuration from
shared. 

### Motivation

Hazelcast supports dynamically changing configuration in a running cluster. Dynamic changes could be adding a map
configuration, adding WAN publishers etc. These dynamic configurations, however, don't survive restarting the cluster. 
The users need to take great care to keep the static configuration in sync with the dynamically modified used
configuration. Additionally, since every member load their own configuration individually on their start, local
configuration files turn this into a deployment problem that all local configurations need to be in sync, otherwise 
various problems may occur. 

There are several features relying on persisting dynamic configurations. The most important class of these features
are related to security, for which it is a must that for example all role changes made during runtime to present on the 
next start.   

### Goals

The resulting configuration needs to be
- available and processable upon the next restart 
- human-readable
- extendable
- reviewable
- manageable in version control systems

This document describes the main considerations and suggests a direction for the research to be done prior to the
production-ready implementation.    

### Non-Goals

- Providing a production-ready implementation
- Make anything dynamically configurable
- Provide CLI tool for managing the configuration in the configuration stores      

### The Configuration Store

The configuration needs to be persisted to and loaded from a common place that is shared amongst the Hazelcast
members. Persisting the configuration can happen upon request or if triggered by an action, primarily when the
configuration changes dynamically (during runtime on a running cluster) such as when a WAN endpoint is added, security 
permissions are changed etc. This configuration store can be a version control system, a shared network folder, a cloud 
storage or anything else. This requires implementing a pluggable solution that abstracts away the storage implementation 
from the logic persisting the configuration. 

#### Versioning/Tagging Support

Versioning or tagging the configurations is a possibility. This is not a requirement for the research, but can be an
attractive option for various reasons such as debugging, benchmarking, deploying to different regions etc.    

#### The Format of the Config

The infrastructure to generate an XML configuration from an in-memory `Config` hierarchy already exists and working, 
covered with many tests. Since `DynamicConfigurationAwareConfig` is an extension of `Config`, leveraging this
infrastructure seems to be a natural approach. XML as a format meets all requirements we have for the persisted
configuration. Generating YAML configuration is also possible, but there is no infrastructure for that at all. For
the research XML configuration is sufficient.  

#### Configuring the Configuration Store

The configuration store needs to be configured somehow. One choice is configuring it in the member configuration, but
this raises questions. What to do with the rest of the configuration entries? Some options
1) Introduce a separate configuration only to define where to look for the stored configuration
2) Use system arguments for the same
3) Extend the member configuration with the storage configuration

1) and 2) are straightforward, but 3) raises the question: what to do with the rest of the member configuration
? It can be used as         

#### The Initial Configuration



### The Consistency of the Configuration
### The Availability of the Configuration Store
### Rolling Upgrades
