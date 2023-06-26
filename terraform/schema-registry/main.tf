terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

resource "docker_container" "schema-registry" {
  image = "confluentinc/cp-schema-registry"
  name  = "schema-registry"
  env   = [
    "SCHEMA_REGISTRY_HOST_NAME=schema-registry",
    "SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL=${tostring(join(",", var.zookeeper_connect_list))}",
    "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS=${tostring(join(",", var.kafka_connect_list))}"
  ]

  ports {
    internal = 8081
    external = 8081
  }

  networks_advanced {
    name = var.network
  }
}