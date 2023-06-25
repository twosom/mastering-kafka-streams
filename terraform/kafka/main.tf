terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}


resource "docker_container" "kafka" {
  image = "confluentinc/cp-kafka:latest"
  name  = var.container_name

  env = [
    "KAFKA_BROKER_ID=${var.server_id}",
    "KAFKA_ZOOKEEPER_CONNECT=${tostring(join(",", var.zookeeper_connect_list))}",
    "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT",
    "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${var.container_name}:9092,PLAINTEXT_INTERNAL://localhost:${local.broker_port}",
    "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=3"
  ]

  networks_advanced {
    name = var.network
  }

  ports {
    external = local.broker_port
    internal = local.broker_port
  }

  hostname = var.container_name
}
locals {
  broker_port = 9092 + tonumber(var.server_id + 1)
}
