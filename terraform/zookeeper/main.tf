terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

resource "docker_container" "zookeeper" {
  image = "confluentinc/cp-zookeeper:7.3.2"
  name  = var.container_name
  env   = [
    "ZOOKEEPER_SERVER_ID=${tonumber(var.server_id) + 1}",
    "ZOOKEEPER_CLIENT_PORT=2181",
    "ZOOKEEPER_TICK_TIME=2000",
    "ZOOKEEPER_SERVERS=${var.servers}"
  ]
  networks_advanced {
    name = var.network
  }

  ports {
    external = local.zookeeper_port
    internal = 2181
  }
  hostname = var.container_name
}

locals {
  zookeeper_port = 2181 + tonumber(var.server_id)
}