import json
import os
import random
import time

from kafka import KafkaProducer

servers = os.environ['BOOTSTRAP_SERVERS']
interval = os.environ['TOPIC_SEND_INTERVAL']

print(servers)
producer = KafkaProducer(
    bootstrap_servers=servers,
    key_serializer=lambda x: json.dumps(x).encode('utf-8'),
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)


def generate_random_data(game_data, player_data):
    game_ids = [item["id"] for item in game_data]
    player_ids = [item["id"] for item in player_data]
    while True:
        game_id = random.choice(game_ids)
        player_id = random.choice(player_ids)
        score = random.randint(100, 10000)
        data = {
            "score": score,
            "product_id": game_id,
            "player_id": player_id
        }
        # 생성된 데이터 출력
        producer.send('score-events', value=data)

        # 1초 대기
        time.sleep(float(interval))


game_data = [
    {"id": 1, "name": "Super Smash Bros"},
    {"id": 2, "name": "The Legend of the Zelda"},
    {"id": 3, "name": "Genshin"},
    {"id": 4, "name": "DJ MAX"},
    {"id": 5, "name": "NARUTO"},
    {"id": 6, "name": "Mario Kart"},
]

player_data = [
    {"id": 1, "name": "Elyse"},
    {"id": 2, "name": "Mitch"},
    {"id": 3, "name": "Isabelle"},
    {"id": 4, "name": "Sammy"}
]

# 게임 데이터 전송
for game in game_data:
    key = game['id']
    value = game
    producer.send('products', key=key, value=value)

# 플레이어 데이터 전송
for player in player_data:
    key = player['id']
    value = player
    producer.send('players', key=key, value=value)

# 무한 데이터 생성
generate_random_data(game_data, player_data)
