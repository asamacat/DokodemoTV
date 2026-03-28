with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

deps_to_add = """
    // SMB storage (jcifs-ng)
    implementation("eu.agno3.jcifs:jcifs-ng:2.1.10")

    // Datastore preferences
    implementation("androidx.datastore:datastore-preferences:1.1.0")

    // OkHttp for Custom Downloader
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
"""

import re
content = re.sub(r'(dependencies\s*\{[^}]+)', r'\1' + deps_to_add, content)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
