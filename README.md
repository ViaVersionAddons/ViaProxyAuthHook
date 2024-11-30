# ViaProxyAuthHook
Minecraft Server modification to allow [ViaProxy](https://github.com/RaphiMC/ViaProxy) clients to join online mode servers.

## How it works
This plugin works by redirecting the authentication requests from the server to the ViaProxy instance.
ViaProxy then checks if the client is authenticated with ViaProxy and sends the result back to the server.
Clients which are not authenticated with ViaProxy will be authenticated with the official Mojang authentication servers.

## Installation
1. Download the latest version from [GitHub Actions](https://github.com/ViaVersionAddons/ViaProxyAuthHook/actions).
2. Put the jar file into the plugins folder of ViaProxy
3. Run ViaProxy once to generate the config file
4. Copy the secret key from the AuthHook config file (You will need it later for the server)
5. Download the latest version of the AuthHook agent (Same link as step 1)
6. Put the AuthHook agent into the same folder as the server jar
7. Add the following JVM argument to the server start command: `-javaagent:ViaProxyAuthHook-x.x.x.jar` (Replace x.x.x with the version of the AuthHook agent you downloaded)
8. Start the server once to generate the config file
9. Open the config file (It's in the same folder as the server jar) and set the secret key to the key you copied in step 4
10. Start both the server and ViaProxy. You can now switch the authentication mode to AuthHook (Use `AUTH_HOOK` for CLI or config file).

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/ViaVersionAddons/ViaProxyAuthHook/issues).  
If you just want to talk or need help using ViaProxyAuthHook feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
