import os
path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

text = text.replace(
    'val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)',
    '@androidx.compose.foundation.ExperimentalFoundationApi\n            val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)'
)

text = text.replace(
    '@androidx.compose.foundation.ExperimentalFoundationApi\n            @androidx.compose.foundation.ExperimentalFoundationApi',
    '@androidx.compose.foundation.ExperimentalFoundationApi'
)

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
