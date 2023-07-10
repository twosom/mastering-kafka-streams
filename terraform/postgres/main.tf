terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}


resource "docker_container" "postgres" {
  image = "postgres:9.6.19-alpine"
  name  = "postgres"

  networks_advanced {
    name = var.network
  }

  env = [
    "POSTGRES_PASSWORD=secret",
    "POSTGRES_USER=root",
    "POSTGRES_DB=root"
  ]

  ports {
    external = 5432
    internal = 5432
  }

  volumes {
    host_path      = abspath("${path.module}/script")
    container_path = "/docker-entrypoint-initdb.d"
  }
}