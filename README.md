![logo](src/main/resources/assets/lanannouncer/icon.png)

# LAN Announcer

Announces the server to the LAN using multicast UDP.

Sends an announcement packet every 1.5 seconds on both IPv4 and IPv6, similar to how the normal Minecraft client does when you "Open to LAN".

Makes the server automagically appear on all Minecraft clients on the same local network.

Really nice when making home server for LANs or for the kids.

# Technical details
Every 1.5 seconds it sends the payload:

```
[MOTD]<server motd>[/MOTD][AD]<server listen port>[/AD]
```

For example:

```
[MOTD]A Minecraft Server[/MOTD][AD]25565[/AD]
```

Over UDP port `4445` to the IP addresses:
- `224.0.2.60`
- `ff75:230::60`

# Published on

- https://modrinth.com/mod/lan-announcer

# Credits

A majority of this project was created with the help of GPT-4.
