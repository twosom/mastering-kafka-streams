terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

resource "docker_container" "ksqldb-cli" {
  image = "confluentinc/cp-ksqldb-cli:latest.arm64"
  name  = var.container_name

  networks_advanced {
    name = var.network
  }

  volumes {
    host_path      = abspath("${path.module}/files/ksqldb-cli")
    container_path = "/etc/ksqldb-cli"
  }

  volumes {
    host_path = abspath("${path.module}/files/sql")
    container_path = "/etc/sql"
  }

  entrypoint = ["/bin/sh"]
  tty        = true

}