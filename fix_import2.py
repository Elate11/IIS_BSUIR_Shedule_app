import sys

path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

text = text.replace('package com.example.schedule', 'package com.example.schedule\n\nimport okhttp3.RequestBody.Companion.toRequestBody\nimport okhttp3.Request')

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
