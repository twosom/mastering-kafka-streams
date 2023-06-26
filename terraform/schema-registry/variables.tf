variable "network" {
  type = string
}

variable "zookeeper_connect_list" {
  type = list(string)
}

variable "kafka_connect_list" {
  type = list(string)
}