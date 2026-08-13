import sys

path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

text = text.replace('\"Mozilla/5.0 (Linux; Android 14)\"', '\"MyIIS/1.0 CFNetwork/1408.0.4 Darwin/22.5.0\"')

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
