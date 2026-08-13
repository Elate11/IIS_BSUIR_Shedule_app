import os
path = 'app/src/main/java/com/example/schedule/MainActivity.kt'
with open(path, 'r', encoding='utf-8-sig') as f:
    text = f.read()

text = text.replace(
    '@androidx.compose.foundation.ExperimentalFoundationApi\n            val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)',
    'val defaultFling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)'
)

text = text.replace(
    '@Composable\nfun MinMainScreen(',
    '@androidx.compose.foundation.ExperimentalFoundationApi\n@Composable\nfun MinMainScreen('
)

with open(path, 'w', encoding='utf-8-sig') as f:
    f.write(text)
