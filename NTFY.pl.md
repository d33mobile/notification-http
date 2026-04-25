# Stawianie własnego serwera ntfy

[ntfy.sh](https://ntfy.sh) to prosty serwer pub-sub do powiadomień push: wysyłasz HTTP POST na URL z nazwą tematu, klient (np. apka Android `ntfy`) zasubskrybowany na ten temat dostaje notyfikację. Można korzystać z publicznego `ntfy.sh` lub postawić własny — i właśnie to opisuje ten dokument. Dla aplikacji `notification-http` (ten fork) to docelowy odbiorca webhooków.

## Wybór: publiczny czy własny

| | Publiczny `ntfy.sh` | Własny |
|---|---|---|
| Setup | 0 minut | 15 minut |
| Prywatność | Twoje powiadomienia przez czyjś serwer | Tylko Twój |
| Rate limit | Tak, ale szczodry | Brak |
| Auth | Opcjonalnie konto + token | Pełna kontrola (ACL, basic auth, bearer) |
| Koszt | 0 zł lub Pro od ~$5/mies | Hosting (~5 zł/mies VPS) |

Jeśli ufasz `ntfy.sh` i nie wysyłasz nic poufnego — zrób tylko `https://ntfy.sh/jakis-trudny-do-zgadnięcia-topic` i pomiń resztę. Reszta dokumentu zakłada, że stawiasz swojego.

## Wymagania

- Linux serwer z publicznym IP lub dostępny przez Twój VPN/Tailscale
- Domena (opcjonalna, ale potrzebna do TLS i ładnego URL)
- Port 80/443 wolny (lub inny dowolny — wtedy w URL dodasz `:port`)

## Instalacja — trzy opcje

### Opcja A: docker (najprostsze)

```bash
mkdir -p /var/lib/ntfy/cache /etc/ntfy
cat > /etc/ntfy/server.yml <<'EOF'
base-url: "https://ntfy.example.com"
listen-http: ":80"
cache-file: "/var/cache/ntfy/cache.db"
auth-file: "/var/lib/ntfy/user.db"
auth-default-access: "deny-all"
behind-proxy: true
EOF

docker run -d \
  --name ntfy \
  --restart unless-stopped \
  -p 127.0.0.1:8080:80 \
  -v /etc/ntfy:/etc/ntfy \
  -v /var/lib/ntfy:/var/cache/ntfy \
  binwiederhier/ntfy serve
```

### Opcja B: apt (Debian/Ubuntu)

```bash
curl -fsSL https://archive.heckel.io/apt/pubkey.txt | sudo gpg --dearmor -o /usr/share/keyrings/archive.heckel.io.gpg
sudo tee /etc/apt/sources.list.d/archive.heckel.io.list <<<'deb [signed-by=/usr/share/keyrings/archive.heckel.io.gpg] https://archive.heckel.io/apt debian main'
sudo apt update && sudo apt install -y ntfy
sudo systemctl enable --now ntfy
```

Konfiguracja w `/etc/ntfy/server.yml`, log w `journalctl -u ntfy -f`.

### Opcja C: gołe binarki

```bash
wget https://github.com/binwiederhier/ntfy/releases/download/v2.13.0/ntfy_2.13.0_linux_amd64.tar.gz
tar -xzf ntfy_*.tar.gz
sudo mv ntfy_*/ntfy /usr/local/bin/
ntfy serve --base-url https://ntfy.example.com
```

## Konfiguracja minimalna (`/etc/ntfy/server.yml`)

```yaml
# Publicznie widoczny URL — wpisujesz to do aplikacji jako webhook URL
base-url: "https://ntfy.example.com"

# Słuchaj tylko po localhost; reverse proxy (Caddy/nginx/Traefik) terminuje TLS
listen-http: "127.0.0.1:8080"

# Persystencja powiadomień, żeby ntfy app mogła je dociągnąć po offline
cache-file: "/var/cache/ntfy/cache.db"
cache-duration: "12h"

# Plik z użytkownikami i ACL (patrz niżej)
auth-file: "/var/lib/ntfy/user.db"
auth-default-access: "deny-all"

# Limit rozmiaru attachmentów (opcjonalne)
attachment-cache-dir: "/var/cache/ntfy/attachments"
attachment-total-size-limit: "1G"
attachment-file-size-limit: "15M"

# Jeśli za reverse proxy — żeby logi pokazywały realne IP klientów
behind-proxy: true
```

Po zmianie config: `sudo systemctl restart ntfy` (lub `docker restart ntfy`).

## Reverse proxy + TLS (Caddy — 5 linii)

```caddyfile
ntfy.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

Caddy sam pobiera Let's Encrypt cert i odnawia. Dla nginx zobacz [oficjalną dokumentację ntfy](https://docs.ntfy.sh/config/#nginxapache2-tls).

## Authentication: token bearer

To jest config który pasuje do tego, co aplikacja `notification-http` wstawia w `Authorization: Bearer <token>`.

```bash
# Stwórz użytkownika "phone" z hasłem (hasło wymagane przy create, potem nie używamy)
sudo ntfy user add phone

# Daj mu prawo write na konkretny topic
sudo ntfy access phone "moj-telefon" write

# Wygeneruj token bezterminowy dla tego usera
sudo ntfy token add phone --label "android-fork"
# wypisze: tk_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

W aplikacji `notification-http` na telefonie:

- **Webhook URL**: `https://ntfy.example.com/moj-telefon`
- **Bearer token**: `tk_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX` (bez prefiksu `Bearer ` — aplikacja sama doda)

## Subskrypcja na telefonie

Zainstaluj aplikację `ntfy` (Play Store, F-Droid, lub APK z [github.com/binwiederhier/ntfy-android](https://github.com/binwiederhier/ntfy-android)).

1. W appce: + → Subscribe to topic
2. Topic: `moj-telefon`
3. Use another server: `https://ntfy.example.com`
4. Authentication: Username/Password lub Token; wpisz tego samego tokena co wyżej

Po sekundach od POSTa serwera — masz natywne push'a.

## Test end-to-end z linii komend

```bash
# Z dowolnej maszyny:
curl -H "Authorization: Bearer tk_XXX..." \
     -H "Title: Test" \
     -H "Tags: cli" \
     -d "Wiadomość testowa" \
     https://ntfy.example.com/moj-telefon

# Powinno wrócić JSON z `id`, a apka ntfy na telefonie zaraz potem pokaże powiadomienie.
```

## Pułapki

- **`auth-default-access: deny-all`** + brak ACL = wszyscy dostają 401. Najczęstsza przyczyna "ntfy nie działa". Sprawdź `sudo ntfy access` żeby zobaczyć kto co może.
- **Topic name = secret w defaultowym setupie**. Jeśli nie używasz auth, każdy kto zna nazwę topicu może wysyłać i czytać. Użyj długiej losowej nazwy (`openssl rand -hex 16`) lub auth.
- **Token vs login/hasło**: Bearer token jest wygodniejszy dla skryptów i nie wymaga przechowywania hasła w plain text — używaj go.
- **Reverse proxy timeouts**: ntfy app trzyma długi `GET /topic/json` (long-poll/SSE). Caddy domyślnie OK, nginx wymaga `proxy_read_timeout 600s;`.
- **HTTPS vs HTTP**: aplikacja `notification-http` ma `usesCleartextTraffic="true"` więc przyjmie http://, ale dla publicznego URL **zawsze HTTPS** (ntfy server inaczej leakuje token w plain text przez sieć).

## Szybkie polecenia diagnostyczne

```bash
# Logi servera
sudo journalctl -u ntfy -f       # systemd
docker logs -f ntfy              # docker

# Lista topiców z aktywnymi subskrybentami
curl https://ntfy.example.com/v1/stats

# Aktualne połączenia
sudo ss -tnp 'sport = :8080'
```

## Linki

- Oficjalne docs: <https://docs.ntfy.sh/>
- Self-hosting: <https://docs.ntfy.sh/install/>
- Konfiguracja: <https://docs.ntfy.sh/config/>
- Android app source: <https://github.com/binwiederhier/ntfy-android>
