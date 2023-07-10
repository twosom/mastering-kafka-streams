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

  entrypoint = ["/bin/sh"]
  tty        = true

}