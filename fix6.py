import os
path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

text = '@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n' + text

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
