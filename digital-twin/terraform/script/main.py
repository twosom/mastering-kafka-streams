import json
import os
import random
import time

from kafka import KafkaProducer

servers = os.environ['BOOTSTRAP_SERVERS']
id = os.environ['WIND_TURBINE_ID']

producer = KafkaProducer(
    bootstrap_servers=servers,
    key_serializer=lambda x: json.dumps(x).encode('utf-8'),
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)


def create_reported_data() -> object:
    current_time = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    wind_speed = random.randint(0, 100)
    power_status = random.choice(["ON"])
    return {
        "timestamp": current_time,
        "wind_speed_mph": wind_speed,
        "power": power_status,
        "type": "REPORTED"
    }


# 1|{"timestamp": "2020-11-23T09:12:00.000Z", "power": "ON", "type": "DESIRED"}

def create_desired_data() -> object:
    current_time = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    return {
        "timestamp": current_time,
        "power": "ON",
        "type": "DESIRED"
    }


while True:
    if random.random() < 0.01:  # 약 1%의 확률로 추가 전송
        data = create_desired_data()
        print(data)
        producer.send('desired-state-events', key=int(id), value=data)
        time.sleep(1)
    else:
        data = create_reported_data()
        print(data)
        producer.send('reported-state-events', key=int(id), value=data)
        time.sleep(1)  # 1초 대기
