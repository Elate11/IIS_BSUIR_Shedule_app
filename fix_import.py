import sys

path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

# Add the missing import
import_stmt = 'import okhttp3.RequestBody.Companion.toRequestBody'
if import_stmt not in text:
    text = text.replace('import okhttp3.RequestBody', f'import okhttp3.RequestBody\n{import_stmt}')
    
with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
