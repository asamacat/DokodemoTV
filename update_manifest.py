import xml.etree.ElementTree as ET

ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
ET.register_namespace('tools', 'http://schemas.android.com/tools')

def add_permissions():
    tree = ET.parse('app/src/main/AndroidManifest.xml')
    root = tree.getroot()

    permissions = [
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
        "android.permission.WAKE_LOCK",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.POST_NOTIFICATIONS"
    ]

    existing_permissions = []
    for elem in root.findall('uses-permission'):
        name = elem.get('{http://schemas.android.com/apk/res/android}name')
        if name:
            existing_permissions.append(name)

    for perm in permissions:
        if perm not in existing_permissions:
            new_perm = ET.Element('uses-permission')
            new_perm.set('{http://schemas.android.com/apk/res/android}name', perm)
            root.insert(1, new_perm)

    application = root.find('application')

    service_exists = False
    for service in application.findall('service'):
        if service.get('{http://schemas.android.com/apk/res/android}name') == ".service.RecordingForegroundService":
            service_exists = True
            break

    if not service_exists:
        service_elem = ET.Element('service')
        service_elem.set('{http://schemas.android.com/apk/res/android}name', '.service.RecordingForegroundService')
        service_elem.set('{http://schemas.android.com/apk/res/android}exported', 'false')
        service_elem.set('{http://schemas.android.com/apk/res/android}foregroundServiceType', 'dataSync')
        application.append(service_elem)

    ET.indent(tree, space="    ", level=0)
    tree.write('app/src/main/AndroidManifest.xml', encoding='utf-8', xml_declaration=True)

if __name__ == '__main__':
    add_permissions()
