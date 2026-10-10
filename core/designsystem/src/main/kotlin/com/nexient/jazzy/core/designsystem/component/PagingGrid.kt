package com.nexient.jazzy.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.designsystem.theme.JazzyColors

/** Shimmering 2:3 poster placeholders for a grid that has no data yet. */
fun LazyGridScope.posterSkeletonItems(count: Int = 12, keyPrefix: String = "skeleton") {
    items(count, key = { "$keyPrefix:$it" }) {
        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)))
    }
}

/** A full-width section header inside a grid. */
fun LazyGridScope.gridHeader(key: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterStart).semantics { heading() })
            if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

/** Footer for a paged grid: a spinner while the next page loads, or an inline retry on failure. */
fun LazyGridScope.pagingFooter(items: LazyPagingItems<*>) {
    when (items.loadState.append) {
        is LoadState.Loading -> item(key = "paging_footer", span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = JazzyColors.Accent)
            }
        }
        is LoadState.Error -> item(key = "paging_footer", span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = items::retry) { Text(stringResource(R.string.designsystem_paging_load_more_failed)) }
            }
        }
        is LoadState.NotLoading -> Unit
    }
}
