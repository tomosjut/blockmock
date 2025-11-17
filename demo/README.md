# BlockMock Demo Package

Complete demo setup voor BlockMock met ondersteuning voor meerdere protocols.

## 📁 Inhoud

```
demo/
├── DEMO_GUIDE.md                      # 🎯 COMPLETE DEMO INSTRUCTIES - START HIER!
├── docker-compose-brokers.yml         # Docker setup voor RabbitMQ, Artemis, IBM MQ
├── complete-demo-endpoints.json       # Importeerbare endpoints (HTTP, SFTP, Brokers)
├── test-http.sh                       # HTTP REST API test script
├── test-sftp.sh                       # SFTP file transfer test script
├── test-artemis.py                    # Apache Artemis JMS test script
└── test-ibmmq.py                      # IBM MQ test script
```

## 🚀 Quick Start

```bash
# 1. Start alle message brokers
docker-compose -f demo/docker-compose-brokers.yml up -d

# 2. Wacht tot brokers ready zijn (~30 sec)
docker-compose -f demo/docker-compose-brokers.yml ps

# 3. Start BlockMock (in project root)
cd ..
mvn quarkus:dev -Dquarkus.http.port=8888

# 4. Import demo endpoints
# Open http://localhost:8888
# Ga naar Mock Endpoints → Import → Select demo/complete-demo-endpoints.json

# 5. Run tests!
cd demo
chmod +x *.sh
./test-http.sh
./test-sftp.sh
python3 test-artemis.py
```

## 📚 Volledige Instructies

Zie **[DEMO_GUIDE.md](DEMO_GUIDE.md)** voor:
- Stap-voor-stap demo scenario's
- Team presentatie flow (15-20 min)
- Troubleshooting
- Success metrics
- Demo checklist

## 🎯 Demo Highlights

Deze demo showcased:
- ✅ **HTTP/REST** - Web API mocking
- ✅ **SFTP** - File transfer mocking
- ✅ **RabbitMQ** - AMQP 0.9.1 messaging
- ✅ **Apache Artemis** - JMS messaging
- ✅ **IBM MQ** - Enterprise messaging

**★ NEW:** Broker Abstraction Layer - één interface voor 3 brokers!

## 🔧 Vereisten

### Software
- Docker & Docker Compose
- Python 3.8+
- curl
- sshpass + sftp client

### Python Packages
```bash
pip install stomp.py    # Voor Artemis
pip install pymqi       # Voor IBM MQ (optioneel)
```

## 🌐 URLs

Na opstarten:
- BlockMock UI: http://localhost:8888
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Artemis Console: http://localhost:8161 (admin/admin)
- IBM MQ Console: https://localhost:9443 (admin/passw0rd)

## 📊 Demo Flow

Aanbevolen volgorde voor team presentatie:
1. HTTP Demo (3 min) - `./test-http.sh`
2. SFTP Demo (2 min) - `./test-sftp.sh`
3. RabbitMQ Demo (2 min) - Via Management UI
4. **Artemis Demo (3 min)** - `python3 test-artemis.py` ⭐
5. **IBM MQ Demo (3 min)** - Via Console ⭐
6. Architecture Highlight (3 min)
7. Wrap-up (2 min)

**Totaal: 15-20 minuten**

## 💡 Tips

- Rehearse de demo van tevoren!
- Heb alle browser tabs klaar
- Test alle scripts vooraf
- Backup plan: screenshots als iets misgaat

## 🐛 Troubleshooting

**Brokers starten niet:**
```bash
docker-compose -f demo/docker-compose-brokers.yml logs
```

**Endpoints werken niet:**
- Check of ze **enabled** zijn in UI
- Verifieer **broker type** selectie
- Check **Request Logs** voor errors

**Python scripts falen:**
```bash
pip install stomp.py pymqi
```

Zie **[DEMO_GUIDE.md](DEMO_GUIDE.md)** voor meer troubleshooting tips.

## 🎬 Voor de Demo

Checklist:
- [ ] Alle brokers draaien
- [ ] BlockMock draait
- [ ] Endpoints geïmporteerd
- [ ] Scripts executable
- [ ] Browser tabs open
- [ ] Terminal klaar

**Klaar? Veel succes! 🚀**

---

**Vragen?** Check `DEMO_GUIDE.md` of `../BROKER_ABSTRACTION_GUIDE.md` voor technische details.
