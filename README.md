# ecstatic-ecs
"Just enough ECS to be dangerous"

This is a set of tutorials for a backend engineer to become familiar with some of the plumbing that supports a modern containerized
Java/Dropwizard based microservice running on ECS (and AWS).

 [GraphQL { words, dates, interval, domains }] -> [aggregate store, single app] -> clustered counter
                                     -> aggregate builder task
                                     
 
- 00 - Docker build single app, run locally (dockerfile, ports, volumes)
- 01 - Launch an instance, AMIs, security groups, keypairs, SSH, docker push/pull/run. Resource usage, docker exec
- 02 - Launch an instance automatically (ASG), user data, load balancer, health checks
- 03 - Container cluster, ECS as a scheduler - deploy it as a task
- 03 - ECS services, health checks, autoscaling
- 04 - Service discovery - internal load balancers, service discovery, external systems (consul)
- 05 - Immutable deployments. ECS as control plane