from zeroconf import Zeroconf, ServiceBrowser

class MyListener:
    def add_service(self, zeroconf, type, name):
        print(f"Service found: {name}")

zeroconf = Zeroconf()
browser = ServiceBrowser(zeroconf, "_findmysong._tcp.local.", MyListener())

try:
    input("Browsing... press enter to exit\n")
finally:
    zeroconf.close()