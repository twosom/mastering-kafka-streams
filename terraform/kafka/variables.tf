variable "network" {
  type = string
}

variable "container_name" {
  type = string
}

variable "zookeeper_connect_list" {
  type = list(string)
}

variable "server_id" {
  type = string
}