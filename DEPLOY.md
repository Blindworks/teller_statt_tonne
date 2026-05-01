# Deployment auf Hetzner Cloud (CX, Docker Compose)

Dieses Dokument beschreibt den manuellen Deployment-Workflow. Reverse-Proxy und TLS-Zertifikate werden **direkt auf der VM** eingerichtet (z.B. Host-Nginx mit Certbot) und sind **nicht** Teil dieses Repos.

## Architektur

```
Host-Proxy (auf der VM, vom Admin verwaltet) ─► 127.0.0.1:8080 → backend  (/api/*, /uploads/*)
                                            └► 127.0.0.1:8081 → frontend (/*)

backend  ──► db (mariadb, intern, kein Port-Mapping)
```

## Erstes Deployment

### 1. VM vorbereiten (Ubuntu 24.04 LTS)

```bash
# als root oder mit sudo
apt update && apt upgrade -y
apt install -y docker.io docker-compose-v2 git
systemctl enable --now docker

# Firewall
ufw allow OpenSSH
ufw allow http
ufw allow https
ufw enable
```

### 2. Repo klonen und konfigurieren

```bash
git clone <repo-url> /opt/teller_statt_tonne
cd /opt/teller_statt_tonne
cp .env.example .env
$EDITOR .env  # alle <change-me>-Werte ersetzen
```

Wichtig in `.env`:
- `JWT_SECRET`: mindestens 32 Zeichen, z.B. `openssl rand -base64 48`.
- `CORS_ALLOWED_ORIGINS`: die oeffentliche HTTPS-URL (z.B. `https://teller-statt-tonne.example.de`).
- `ADMIN_EMAIL`/`ADMIN_PASSWORD`: Initial-Admin (wird beim ersten Start angelegt).

### 3. Container starten

```bash
docker compose up -d --build
docker compose logs -f backend
```

Das Backend wartet, bis MariaDB healthy ist, fuehrt dann die Liquibase-Migrationen aus und legt den Initial-Admin an.

### 4. Reverse-Proxy auf dem Host

Beispiel-Routing (Host-Nginx, Caddy o.ae.):
- `/api/*`     → `http://127.0.0.1:8080`
- `/uploads/*` → `http://127.0.0.1:8080`
- `/*`         → `http://127.0.0.1:8081`

TLS (Let's Encrypt o.ae.) wird vom Host-Proxy terminiert. Die Container exponieren bewusst nur Loopback-Ports.

## Updates

```bash
cd /opt/teller_statt_tonne
git pull
docker compose up -d --build
```

Liquibase migriert das DB-Schema beim Start. Daten und Uploads bleiben in den Volumes `db_data` und `uploads` erhalten.

## Logs & Status

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

## Backup

- DB-Dump:
  ```bash
  docker compose exec db sh -c 'mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" "$MARIADB_DATABASE"' > backup-$(date +%F).sql
  ```
- Uploads: das Volume `uploads` sichern, z.B. via `docker run --rm -v teller_statt_tonne_uploads:/data -v $PWD:/backup alpine tar czf /backup/uploads-$(date +%F).tgz -C /data .`

## Troubleshooting

- **Backend startet nicht / DB nicht erreichbar**: `docker compose logs db` pruefen, Healthcheck-Status mit `docker compose ps`.
- **CORS-Fehler im Browser**: `CORS_ALLOWED_ORIGINS` in `.env` muss exakt zur Browser-URL passen (Schema + Host, ohne Pfad). Nach Aenderung: `docker compose up -d backend`.
- **Profilbilder werden nicht angezeigt**: Host-Proxy muss `/uploads/*` an `127.0.0.1:8080` weiterleiten.
