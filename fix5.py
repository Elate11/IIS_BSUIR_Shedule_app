import os
path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

text = text.replace(
    'val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)',
    '@Suppress("OPT_IN_USAGE")\n            val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)'
)

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
