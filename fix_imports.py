import re

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'r') as f:
    content = f.read()

# Fix conflicting imports by keeping only unique ones
imports_section = re.search(r'(package com\.example\.dokodemotv\n\n)(.*?)(?=\n@OptIn|\n@UnstableApi|\nclass MainActivity)', content, re.DOTALL)
if imports_section:
    imports_text = imports_section.group(2)
    unique_imports = sorted(list(set([imp.strip() for imp in imports_text.split('\n') if imp.strip().startswith('import ')])))

    new_imports_text = '\n'.join(unique_imports) + '\n'
    content = content[:imports_section.start(2)] + new_imports_text + content[imports_section.end(2):]

with open('app/src/main/java/com/example/dokodemotv/MainActivity.kt', 'w') as f:
    f.write(content)
