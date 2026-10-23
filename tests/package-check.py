from pathlib import Path
import subprocess, zipfile, xml.etree.ElementTree as E
A='{http://schemas.android.com/apk/res/android}'
m=E.parse('AndroidManifest.xml').getroot()
assert m.attrib['package']=='cn.g.clear'
assert {p.attrib[A+'name'] for p in m.findall('uses-permission')}=={'android.permission.RECEIVE_BOOT_COMPLETED'}
s=m.find('application/service')
assert s.attrib[A+'permission']=='android.permission.BIND_ACCESSIBILITY_SERVICE'
assert s.attrib[A+'exported']=='true'
assert m.find('application').attrib[A+'allowBackup']=='false'
x=E.parse('res/xml/accessibility.xml').getroot()
assert x.attrib[A+'canRetrieveWindowContent']=='true'
assert x.attrib.get(A+'isAccessibilityTool')=='false'
assert 'typeAllMask' not in x.attrib[A+'accessibilityEventTypes']
with zipfile.ZipFile('build/G-clear-0.1.apk') as z:
 assert {'classes.dex','AndroidManifest.xml','resources.arsc'} <= set(z.namelist())
 assert not any(n.endswith(('.jks','.keystore','.java')) or 'password' in n for n in z.namelist())
 assert Path('tools/signing-password').read_bytes().strip() not in z.read('classes.dex')
cert=subprocess.check_output(['keytool','-exportcert','-keystore','tools/g-clear.keystore','-storepass:file','tools/signing-password','-alias','gclear'],stderr=subprocess.DEVNULL)
import hashlib
assert hashlib.sha256(cert).hexdigest()==Path('signing-certificate.sha256').read_text().strip()
subprocess.run(['apksigner','verify','--verbose','build/G-clear-0.1.apk'],check=True)
subprocess.run(['zipalign','-c','4','build/G-clear-0.1.apk'],check=True)
print('PASS: package identity, restricted permissions, service declaration, no credentials, signature and alignment')
