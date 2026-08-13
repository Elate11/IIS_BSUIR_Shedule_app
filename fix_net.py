import sys

path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

# Replace all inline OkHttpClient builders with NetworkClient.client
old1 = 'val client = okhttp3.OkHttpClient.Builder().connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS).readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(true).build()'
new1 = 'val client = com.example.schedule.NetworkClient.client'

old2 = 'val client = OkHttpClient.Builder().connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS).readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(true).build()'
new2 = 'val client = com.example.schedule.NetworkClient.client'

c1 = text.count(old1)
c2 = text.count(old2)
text = text.replace(old1, new1)
text = text.replace(old2, new2)

print(f'Replaced {c1} okhttp3 builders, {c2} OkHttpClient builders')

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
