# Selling keys from a VPS

The short version: the customer pays, gets a key like
`STORM-4K7M-9QX2-JH3D`, types it into the launcher, and plays. This page sets
that up.

## How the pieces fit

There are two different things, and keeping them apart is what makes the whole
thing work:

- **The key** is what the customer gets. It is short, you can revoke it, and it
  carries no authority of its own.
- **The licence** is what the client checks. It is a blob signed with your
  private key, it expires, and the client verifies it offline.

The launcher swaps one for the other:

```
customer pays
     -> Stripe calls your server
     -> server issues STORM-4K7M-9QX2-JH3D
     -> customer types it into the launcher
     -> launcher asks the server, sending the key and the machine id
     -> server signs a licence valid for the next few days
     -> launcher stores it and starts the game with it
     -> client checks the signature against the public key in the build
```

The launcher asks again on every launch. That is the important part: the signed
licence only lasts `leaseDays`, so revoking a key cuts the customer off within
days, and the client still starts when your server is down because the stored
licence is good until it runs out.

## What you need

- An Ubuntu VPS with a domain pointed at it
- Java 17 or newer: `apt install openjdk-17-jre-headless`
- nginx and certbot for TLS
- A Stripe account, if you want payment to be automatic

## 1. Make the signing key

Do this **on your own machine**, not on the VPS, and keep the private key off
every computer you hand the client to.

```
javac -d out tools/licence/LicenceTool.java
java -cp out LicenceTool genkey
```

It writes `licence-private.key` and prints the public key. Paste that into
`storm-core/src/main/java/xyz/stormclient/licence/LicenceVerifier.java`:

```java
public static final String PUBLIC_KEY = "MIIBIjANBgkq...";
```

Rebuild (`.\build.ps1`). From here the client refuses to load any module
without a valid licence.

If the private key ever leaks, anyone can mint licences your build accepts. The
only fix is a new pair, a new build and reissuing every key, so treat the
backup of that one file seriously.

## 2. Put the server on the VPS

Copy `dist/storm-licence-server.jar` and the `storm-licence-server/deploy`
folder up, then:

```
sudo bash deploy/install.sh
```

That creates a `storm` service account, `/opt/storm-licences`, and the systemd
unit. It deliberately does not start anything yet; it prints the three things
left to do.

Copy your private key up and lock it down:

```
scp licence-private.key root@your-vps:/opt/storm-licences/
ssh root@your-vps 'chown storm:storm /opt/storm-licences/licence-private.key &&
                   chmod 600 /opt/storm-licences/licence-private.key'
```

Set `adminToken` in `/opt/storm-licences/storm-licences.properties` (the
install script prints a fresh one), then:

```
systemctl enable --now storm-licences
journalctl -u storm-licences -f
```

## 3. TLS

The server only listens on `127.0.0.1`, so nginx has to be in front of it.
Copy `deploy/nginx.conf.example` to
`/etc/nginx/sites-available/storm-licences`, change the domain, then:

```
ln -s /etc/nginx/sites-available/storm-licences /etc/nginx/sites-enabled/
certbot --nginx -d keys.example.com
nginx -t && systemctl reload nginx
curl https://keys.example.com/health
```

One detail in that config matters: it sets `X-Forwarded-For` to `$remote_addr`,
not `$proxy_add_x_forwarded_for`. The second one keeps whatever the caller
sent, so anyone could claim a different address and get themselves a fresh rate
limit bucket.

## 4. Issuing keys by hand

```
export STORM_SERVER=https://keys.example.com
export STORM_TOKEN=<the adminToken>

tools/licence/keys.sh issue "ivan@example.com" pro 30
tools/licence/keys.sh list
tools/licence/keys.sh status  STORM-4K7M-9QX2-JH3D
tools/licence/keys.sh revoke  STORM-4K7M-9QX2-JH3D
tools/licence/keys.sh unbind  STORM-4K7M-9QX2-JH3D
```

`days 0` never expires. `unbind` clears the machine a key is tied to, which is
what you run when a customer gets a new computer.

## 5. Taking payment

In the Stripe dashboard, add a webhook endpoint at
`https://keys.example.com/hook/stripe` subscribed to
`checkout.session.completed`. Copy its signing secret into
`stripeWebhookSecret` and restart the service.

Set a `plan` in the Checkout Session metadata to pick which plan the buyer
gets; without one they get `defaultPlan`. How long each plan lasts is set by
the `plan.*` lines in the config.

The server checks the signature on every webhook call, refuses anything older
than five minutes, and gives the same checkout the same key however many times
Stripe retries.

### Getting the key to the buyer

The server does not send email. Point Stripe's `success_url` at a page of yours
with `?session_id={CHECKOUT_SESSION_ID}` on the end, and have that page ask:

```
GET https://keys.example.com/api/claim?session=cs_test_a1b2c3
    -> { "key": "STORM-4K7M-9QX2-JH3D", "plan": "pro" }
```

The session id is unguessable and only the buyer's browser has it. Stripe's
webhook and the redirect race each other, so a 404 there means "not yet" rather
than "no": retry for a few seconds before showing an error.

## 6. What the customer does

Storm launcher, **Licence** page: licence key, licence server, **Activate**.
That is the whole thing. The in-game menu's Licence page then shows who the key
belongs to and when it runs out.

A key binds to the first computer it is activated on. If someone needs to move,
run `keys.sh unbind`.

## The endpoints

| Endpoint | Who | What |
|---|---|---|
| `POST /api/activate` | anyone | key + machine id, returns a signed licence |
| `GET /api/status?key=` | anyone | state, plan, end date |
| `GET /api/claim?session=` | the buyer | the key for a finished checkout |
| `POST /api/issue` | admin token | makes a key |
| `POST /api/revoke` | admin token | switches a key off, or back on |
| `POST /api/unbind` | admin token | frees the machine binding |
| `GET /api/keys` | admin token | every key |
| `POST /hook/stripe` | Stripe | issues a key on payment |
| `GET /health` | anyone | is it up |

## Backups

Two files, both in `/opt/storm-licences`:

- `licence-private.key` - irreplaceable, as above
- `licences.json` - every key you ever issued

```
scp root@your-vps:/opt/storm-licences/{licence-private.key,licences.json} ./backup/
```

## What this does not do

The client-side check is a check that runs on the customer's machine, in a jar
they can open. Someone determined can edit it out, and no client-side licence
of any design avoids that. The server makes keys issuable, expirable and
revocable for ordinary customers; it does not make the client uncrackable. Only
code that runs on your server is genuinely out of reach.
