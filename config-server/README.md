# ecommerce-config

Centralized configuration for the [spring-cloud-ecommerce](https://github.com/biruksolomon/spring-cloud-ecommerce)
stack, served by its `config-server` module (Spring Cloud Config Server,
git backend).

This is a **separate git repository on purpose** - config-server clones/pulls
it independently of the application code, so config can change (and be
audited, reviewed, rolled back) without a service rebuild or redeploy.

## Layout

Spring Cloud Config resolves files by `{application}-{profile}.yml`, falling
back to `{application}.yml`, falling back to `application.yml` (shared by
every client). This repo currently has no profile-specific files, just:

- `application.yml` - shared across every service that imports config
  (Eureka registration target, the internal-service-token every downstream
  service checks, actuator exposure).
- `<service-name>.yml` - one file per service, holding what used to be
  hardcoded in that service's local `application.yml`/`application.yaml`
  (datasource, Kafka/RabbitMQ, ports, feature-specific settings).

`discovery-server` and `config-server` itself are **not** config-server
clients - they're the infrastructure that config depends on, so pulling
them into the same bootstrap chain would be circular. Their config stays
local to their own modules.

## Local dev

config-server's default `CONFIG_REPO_URI` is `file://<repo-root>/../config-repo`
(this folder), so nothing extra needs to run - just start config-server and
it reads straight from disk. `force-pull: true` in config-server means it
re-reads on every fetch, so local edits here take effect on the next
`/actuator/refresh` (or service restart) without needing a commit.

## Shared/prod

Point config-server's `CONFIG_REPO_URI` at a real git remote (e.g.
`git@github.com:<org>/ecommerce-config.git` or `https://...`) and set
`CONFIG_REPO_USERNAME`/`CONFIG_REPO_PASSWORD` (or mount an SSH key) for a
private repo. Nothing else changes - config-server and every client read
the same property names either way.

## Making a config change take effect without a restart

Each client exposes `POST /actuator/refresh` (requires `@RefreshScope` on
any bean that reads the changed property). Commit the change here, then:

```
curl -X POST http://<service-host>:<port>/actuator/refresh
```