import json
import os
import random
import time

from kafka import KafkaProducer

servers = os.environ['BOOTSTRAP_SERVERS']
id = os.environ['PATIENT_ID']

print(servers)
producer = KafkaProducer(
    bootstrap_servers=servers,
    key_serializer=lambda x: json.dumps(x).encode('utf-8'),
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)


def create_data():
    current_time = time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()) + f".{int(time.time() * 1000) % 1000:03d}Z"
    return {"timestamp": current_time}


while True:
    if random.random() < 0.01:  # 약 1%의 확률로 추가 전송
        count = random.uniform(100, 140)
        for _ in range(int(count)):
            producer.send('pulse-events', key=int(id), value=(create_data()))
            time.sleep(60 / count)
    else:
        producer.send('pulse-events', key=int(id), value=(create_data()))
        delay = random.uniform(0.6, 1.4)  # 0.6초부터 1.4초 사이의 랜덤한 지연 시간 설정
        time.sleep(delay)
