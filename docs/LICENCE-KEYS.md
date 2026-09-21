# Licence keys

Storm can be built so that it only starts with a key you issued. This page
covers how to turn that on, how to hand keys out, and — just as important —
what it does not protect against.

## What it is

A key is a signed statement. You sign it with a private key that never leaves
your machine; the client checks it with the matching public key, which is
compiled into the build you hand out. Nobody can mint a key without the private
key, and changing a single character of one invalidates it.

A key carries who it is for, a plan name, when it was issued, an optional expiry
and an optional machine id. The client rejects a key that has expired or that
names a different computer.

## What it is not

The check runs on the user's own machine, in a jar they can open. Someone who
knows what they are doing can edit the check out. Every client-side licence
works this way, however it is written; the only thing that genuinely cannot be
copied is code that runs on a server you control.

So treat keys as a way to issue, expire and revoke access for ordinary users —
not as a guarantee. Do not build anything on the assumption that the check
survives a determined user.

## Turning it on

1. Create the signing pair, once:

   ```
   javac -d out tools/licence/LicenceTool.java
   java -cp out LicenceTool genkey
   ```

   This writes `licence-private.key` and prints the public key.

2. Paste the printed line into
   `storm-core/src/main/java/xyz/stormclient/licence/LicenceVerifier.java`:

   ```java
   public static final String PUBLIC_KEY = "MIIBIjANBgkq...";
   ```

3. Rebuild (`./build.sh` or `.\build.ps1`). From now on the client refuses to
   register any module without a valid key.

Keep `licence-private.key` off every machine you give the client to. It is the
one thing that cannot be replaced if it leaks: anyone holding it can issue keys
your build accepts, and the only fix is a new pair and a new build.

Leaving `PUBLIC_KEY` empty keeps licensing off, which is what a build for
yourself wants.

## Issuing a key

```
java -cp out LicenceTool sign --holder "Ivan" --plan pro --days 30
```

The last line it prints is the key. `--days 0` never expires.

To tie a key to one computer, ask the user for the machine id shown on the
launcher's Licence page (the **Copy machine id** button puts it on their
clipboard) and pass it:

```
java -cp out LicenceTool sign --holder "Ivan" --machine 7bfeffb22a64a8af
```

The id is a hash of a few stable properties of that machine, so it survives
reinstalling the client but not moving to a different computer.

## Using a key

The user pastes it into the launcher's **Licence** page and presses **Apply
key**. The launcher verifies it there, so a bad key is caught before the game
starts, stores it, and passes it to the client as `-Dstorm.licence=<key>`.

In game, the menu's **Licence** page shows who the key belongs to, its plan and
when it runs out.

## Revoking

A signed key cannot be taken back on its own, because the check is offline.
Two ways to handle it:

- Issue short keys (`--days 30`) and reissue them. A revoked user simply stops
  getting a new one.
- Bind keys to a machine, so a leaked key only works on the computer it was
  issued for.

If you need instant revocation you need a server the client asks at start up,
which also means the client cannot run while that server is down.
