terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

resource "docker_image" "datasource" {
  name = "datasource-digital-twin"
  build {
    context = path.module
    tag     = ["datasource:digital-twin"]
  }
}