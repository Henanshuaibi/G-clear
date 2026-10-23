"""Inspect staged content, or committed HEAD with --head, without printing secrets."""
import re, subprocess, sys
from pathlib import Path
head='--head' in sys.argv
names=subprocess.check_output(['git','ls-tree','-rz','--name-only','HEAD'] if head else ['git','diff','--cached','--name-only','-z']).decode().split('\0')
patterns=[rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',rb'gh[pousr]_[A-Za-z0-9]{30,}',rb'github_pat_[A-Za-z0-9_]{35,}',rb'sk-[A-Za-z0-9_-]{24,}']
password=Path('tools/signing-password').read_bytes().strip() if Path('tools/signing-password').exists() else None
count=0
for name in filter(None,names):
 p=Path(name)
 if p.parts[0] not in {'.gitignore','AGENTS.md','README.md','build.sh','AndroidManifest.xml','signing-certificate.sha256','res','src','tests'} or p.suffix in {'.keystore','.jks','.apk','.zip','.jar'} or 'password' in p.name:
  raise SystemExit('FAIL: disallowed source path '+name)
 data=subprocess.check_output(['git','show',('HEAD:' if head else ':')+name])
 if any(re.search(pattern,data) for pattern in patterns) or (password and password in data):raise SystemExit('FAIL: credential match in '+name)
 if (b'/data/' + b'data/com.termux/' + b'files/home/') in data:raise SystemExit('FAIL: private absolute path in '+name)
 count+=1
assert count>0,'No files scanned'
print('PASS: inspected '+str(count)+' source files; credentials and forbidden artifacts: 0')
