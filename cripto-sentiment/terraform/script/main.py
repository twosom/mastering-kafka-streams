import random
import string
import time
import os
from json import dumps

import nltk
from kafka import KafkaProducer

topic = 'tweets'

servers = os.environ['BOOTSTRAP_SERVERS']
interval = os.environ['TOPIC_SEND_INTERVAL']
print(servers)
producer = KafkaProducer(bootstrap_servers=servers,\
                         value_serializer=lambda x: dumps(x).encode('utf-8')
                         )
nltk.download('punkt')  # nltk 패키지 다운로드


def generate_random_sentence():
    subjects = ["Bitcoin", "Ethereum", "Cryptocurrency", "Blockchain", "Finance", "Crypto mining"]
    verbs = ["has", "is", "requires", "offers", "changes"]
    adjectives = ["a lot of promise", "smart", "decentralized", "revolutionary", "powerful"]
    objects = ["the future", "the world", "a smart move", "a big impact", "innovation"]

    subject = random.choice(subjects)
    verb = random.choice(verbs)
    adjective = random.choice(adjectives)
    obj = random.choice(objects)

    sentence = f"{subject} {verb} {adjective} for {obj}."
    return sentence


def generate_random_string(length):
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for _ in range(length))


def generate_random_url():
    protocols = ["http://", "https://"]
    domains = ["example.com", "test.com", "sample.org", "dummy.net"]
    protocol = random.choice(protocols)
    domain = random.choice(domains)
    return f"{protocol}{domain}"


def generate_random_lang_code():
    lang_codes = ["en", "ko", "ja", "fr", "es", "de"]
    return random.choice(lang_codes)


def generate_random_lang_sentence(lang_code):
    if lang_code == "en":
        return generate_random_sentence()
    elif lang_code == "ko":
        return generate_random_sentence_ko()
    elif lang_code == "ja":
        return generate_random_sentence_ja()
    elif lang_code == "fr":
        return generate_random_sentence_fr()
    elif lang_code == "es":
        return generate_random_sentence_es()
    elif lang_code == "de":
        return generate_random_sentence_de()


def generate_random_sentence_ko():
    sentences = [
        "비트코인은 큰 잠재력을 가지고 있습니다.",
        "이더리움에 대해서는 잘 모르겠어요.",
        "암호화폐는 세상을 바꾸고 있습니다.",
        "블록체인 기술에 투자하는 것은 현명한 선택입니다.",
        "금융의 미래는 탈중앙화되어 있습니다.",
        "암호화폐 채굴은 많은 계산 능력을 요구합니다."
    ]
    return random.choice(sentences)


def generate_random_sentence_ja():
    sentences = [
        "ビットコインは大いに期待されています。",
        "イーサリアムについてはよくわかりません。",
        "暗号通貨は世界を変えています。",
        "ブロックチェーン技術への投資は賢明な選択です。",
        "金融の未来は非中央集権化されています。",
        "暗号通貨のマイニングには多くの計算能力が必要です。"
    ]
    return random.choice(sentences)


def generate_random_sentence_fr():
    sentences = [
        "Bitcoin a beaucoup de potentiel.",
        "Je ne suis pas trop sûr de Ethereum.",
        "Les crypto-monnaies changent le monde.",
        "Investir dans la technologie blockchain est un choix judicieux.",
        "L'avenir de la finance est décentralisé.",
        "Le minage de crypto-monnaie demande beaucoup de puissance de calcul."
    ]
    return random.choice(sentences)


def generate_random_sentence_es():
    sentences = [
        "Bitcoin tiene mucho potencial.",
        "No estoy muy seguro de Ethereum.",
        "Las criptomonedas están cambiando el mundo.",
        "Invertir en tecnología blockchain es una elección inteligente.",
        "El futuro de las finanzas es descentralizado.",
        "La minería de criptomonedas requiere mucha potencia de cálculo."
    ]
    return random.choice(sentences)


def generate_random_sentence_de():
    sentences = [
        "Bitcoin hat viel Potenzial.",
        "Ich bin mir nicht so sicher über Ethereum.",
        "Kryptowährungen verändern die Welt.",
        "In die Blockchain-Technologie zu investieren ist eine kluge Entscheidung.",
        "Die Zukunft der Finanzwelt ist dezentralisiert.",
        "Das Mining von Kryptowährungen erfordert viel Rechenleistung."
    ]
    return random.choice(sentences)


while True:
    created_at = int(time.time() * 1000)
    tweet_id = random.randint(10000, 99999)
    lang = generate_random_lang_code()
    text = generate_random_lang_sentence(lang)
    retweet = random.choice([True, False])
    source = ""
    user_id = str(random.randint(10000000, 99999999))
    user_name = generate_random_string(10)
    description = generate_random_string(20)
    screen_name = generate_random_string(8)
    url = generate_random_url()
    followers_count = str(random.randint(100, 1000000))
    friends_count = str(random.randint(100, 1000000))

    data = {
        "CreatedAt": created_at,
        "Id": tweet_id,
        "Text": text,
        "Lang": lang,
        "Retweet": retweet,
        "Source": source,
        "User": {
            "Id": user_id,
            "Name": user_name,
            "Description": description,
            "ScreenName": screen_name,
            "URL": url,
            "FollowersCount": followers_count,
            "FriendsCount": friends_count
        }
    }
    producer.send(topic, data)
    time.sleep(float(interval))
