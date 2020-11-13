# Research: Persist Dynamic Configuration 

### Summary

This work is to research what is the best way for persisting dynamic configuration to and loading the configuration from
a shared repository.

### Motivation

Hazelcast supports dynamically changing configuration in a running cluster. Dynamic changes could be adding a map
configuration, adding WAN publishers etc. These dynamic configurations, however, don't survive restarting the cluster. 
The users need to take great care to keep the static configuration in sync with the dynamically modified
configuration the cluster is running with. Additionally, since every member load their own configuration individually on
their start, local configuration files turn this into a deployment problem that all local configurations need to be
in sync, otherwise various problems may occur. 

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
- Making anything dynamically configurable
- Providing CLI tool for managing the configuration in the configuration stores      

### Terminology
 
- _Static configuration_: The configuration provided as a programmatic configuration, declarative configuration file
present on the classpath, in the working directory or defined via system arguments. In general, any configuration that
cannot be updated during runtime.
- _Dynamic configuration_: The actual configuration hierarchy that the cluster is running with. Some if its elements
cannot be changed during runtime, some others can, making the configuration dynamic.  Making any runtime configuration 
change durable is the purpose of this research.
- _Persisted configuration_: The dynamic configuration stored in a configuration repository. 
- _Configuration repository_: The place where a dynamic configuration is stored to and retrieved from.

### Co-existence of Static and Persisted Configuration

The solution has to allow co-existence of static and persisted configurations. The reason is not all configuration
entries are made and going to be made dynamically changeable, therefore, to support evolving every configuration element
can be done through static configuration only. There are three currently known options for this.



#### Configuration Repository as the Primary Source

With this option Hazelcast persists the entire configuration to its configuration repository and uses that as the
primary configuration source. If there is a local static configuration provided at restart time (either programmatic or
declarative), the two configuration files need to be merged at startup time. This approach takes the merged
configuration, persists it in the repository and uses it as the configuration to start the cluster members with.

##### Advantages

- One single configuration
- Central place for the configuration in use
- The whole configuration is shared amongst the cluster members
- The one single configuration can be reviewed, managed in version control systems etc
      
##### Disadvantages

- The used configuration is constructed at runtime
- Programmatic conflict resolution
- The result configuration loses human formatting (can this be avoided?)
- Comments in the configuration is lost (can this be avoided?)
- The result configuration loses variables as the dynamic configuration works with actual values (feels to be avoidable)



#### Static Configuration as the Primary Source

With this option Hazelcast treats the static configuration as the primary configuration, merges the persisted
configuration into that at startup. The persisted configuration contains deltas/dynamic configurations only. Either
in a single file or in multiple files, one for every configuration. 

##### Advantages

- The configuration is "directed" by the configuration the users provided 
- Human formatting is preserved for the static configuration
      
##### Disadvantages

- The used configuration is constructed at runtime
- Programmatic conflict resolution
- Allows different configurations
- There is no single view of the configuration (we can produce it though on request)


#### Configuration Repository Only

With this option Hazelcast loads the configuration from the repository, just like it loads the static configuration
now. No merging, what's in the repository will be used at startup.

##### Advantages

- The used configuration is constructed offline
- Manual conflict resolution
- In general, full manual control over the configuration
- Human formatting is preserved for the static configuration, the dynamic parts can be imported and post-formatted
      
##### Disadvantages

- Probably this needs some tooling/API support

    
#### Programmatic Conflict Resolution    
    
The _Configuration Repository as the Primary Source_ and the _Static Configuration as the Primary Source_
options allow conflicts. A conflict is if the "same" configuration entry is configured differently in the static
and the persisted configurations. Example: same map, but with different in-memory format. This can be treated as an 
error failing startup or can be resolved automatically. The first option may lead to downtimes, the second to
unwanted configuration.  

### The Configuration Repository

The configuration needs to be persisted to and loaded from a common place that is shared amongst the Hazelcast
members. Persisting the configuration can happen upon request or if triggered by an action, primarily when the
configuration changes dynamically (during runtime on a running cluster) such as when a WAN endpoint is added, security 
permissions are changed etc. This configuration store can be a version control system, a shared network folder, a cloud 
storage or anything else. This requires implementing a pluggable solution that abstracts away the storage implementation 
from the logic persisting the configuration.

#### Requirements

- _Secure_: Hazelcast can interact with the repository only through a well-defined, restricted API, allowing
manageable access control.  




Unorganized Musings
===================


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

1) and 2) are straightforward, but 3) raises the question: what to do with the rest of the member configuration? This 
drives us to the question of the question of the initial or the fallback configuration.          

### The Availability of the Configuration Store

If the configuration store is not locally available always if the deployed binary is available, it is possible that
1) the configuration store is not available,  
2) the store is available, but there is no configuration file to use.   

In both cases one might want to fail fast, while others may want to start with a fallback - possibly local, 
always-available - configuration. Possible use case for the latter: it may be better to not WAN replicate to a recently
added backup site than having a prolonged prod outage.

If a certain deployment opts for start even in this case, there should be an initial/fallback configuration. 

### The Consistency of the Configuration

There are two aspects of configuration consistency.

#### Self-consistency

This means the configuration persisted has to be valid in terms of syntax, must pass every validation rules, must have
referential integrity etc. Generally speaking, the persisted configuration must be loadable by the cluster members.

#### Cluster-wide Consistency

This aspect means the configurations the members use and will use after restart must be semantically consistent, 
compatible and in general valid. Have the same configuration for the same maps etc.
     
Question: Is there a configuration entry where we want to support heterogeneity? Any entry pointing to the local
directory structure could be a candidate. Audit log etc. On the other hand this is really not a good practice and it is
arguable that we should facilitate such deployments with our offering.         

### Rolling Upgrades


#### Questions to Answer

1) Which configuration should be the primary?
Options:
1a) The static configuration, and the persisted configuration has to be merged into that before the instance
starts. Should the persisted
 configuration be the primary configuration
