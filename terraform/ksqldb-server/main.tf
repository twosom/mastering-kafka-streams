terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

resource "docker_container" "ksqldb-server" {
  image = "confluentinc/cp-ksqldb-server:latest.arm64"
  name  = var.container_name

  networks_advanced {
    name = var.network
  }

  ports {
    external = 8088
    internal = 8088
  }

  working_dir = "/etc/ksqldb-server"
  hostname    = var.container_name

  volumes {
    host_path      = abspath("${path.module}/files/ksqldb-server")
    container_path = "/etc/ksqldb-server"
  }

  command = ["ksql-server-start", "ksql-server.properties"]
}