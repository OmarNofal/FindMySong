import netifaces
import socket
from zeroconf.asyncio import AsyncZeroconf, AsyncServiceInfo

from api.constants import PORT



def get_all_ips():
    ips = []
    for iface in netifaces.interfaces():
        addrs = netifaces.ifaddresses(iface).get(netifaces.AF_INET, [])
        for addr in addrs:
            if not addr["addr"].startswith("127."):
                ips.append(socket.inet_aton(addr["addr"]))
    return ips

service_info = AsyncServiceInfo(
    type_='_findmysong._tcp.local.',
    name='Find My Song._findmysong._tcp.local.',
    port=PORT,
    server='myhost.local.',
)

async_zeroconf = AsyncZeroconf()

async def register_service():
    print("[Zeroconf] Registering service...")
    await async_zeroconf.async_register_service(service_info)

async def unregister_service():
    print("[Zeroconf] Unregistering service...")
    await async_zeroconf.async_unregister_all_services()
    await async_zeroconf.async_close()
