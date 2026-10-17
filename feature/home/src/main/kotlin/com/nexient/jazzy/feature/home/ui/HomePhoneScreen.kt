package com.nexient.jazzy.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexient.jazzy.core.data.catalog.HomeTab
import com.nexient.jazzy.core.designsystem.component.AvatarTile
import com.nexient.jazzy.core.designsystem.component.HeroBackdrop
import com.nexient.jazzy.core.designsystem.component.HeroText
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.component.jazzyClickable
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.home.HomeActions
import com.nexient.jazzy.feature.home.HomeUiState
import com.nexient.jazzy.feature.home.HomeViewModel
import com.nexient.jazzy.feature.home.R
import com.nexient.jazzy.feature.home.snapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomePhoneScreen(state: HomeUiState, viewModel: HomeViewModel, actions: HomeActions) {
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header") {
                Box {
                    PhoneHeroSlot(viewModel, state, actions)
                    Column(Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars)) {
                        TopBar(state, actions)
                        TabChips(state.tab, viewModel::selectTab)
                    }
                }
                if (state.offline) OfflineBanner()
            }
            items(state.rails, key = { it.key }, contentType = { it::class }) { rail ->
                HomeRailContent(rail, state, viewModel, actions)
            }
        }
    }
}

/** Hero collected on its own so it (and the whole header) doesn't re-run when rails/state update. */
@Composable
private fun BoxScope.PhoneHeroSlot(viewModel: HomeViewModel, state: HomeUiState, actions: HomeActions) {
    val hero by viewModel.heroState.collectAsStateWithLifecycle()
    hero?.let { h ->
        HeroBackdrop(h.content, Modifier.fillMaxWidth().aspectRatio(0.72f))
        HeroText(h.content, Modifier.align(Alignment.BottomStart).padding(horizontal = JazzyTheme.spacing.gutter, vertical = 8.dp)) {
            val inList = h.summary.ref in state.myList
            val liked = h.summary.ref in state.liked
            Button(
                onClick = { actions.openTitle(h.summary.ref, null, null, true) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) { Icon(Icons.Filled.PlayArrow, null); Text(stringResource(R.string.home_hero_watch)) }
            OutlinedButton(onClick = { viewModel.toggleMyList(h.summary.snapshot()) }) {
                Icon(if (inList) Icons.Filled.Check else Icons.Filled.Add, null)
                Text(stringResource(R.string.home_hero_my_list))
            }
            IconButton(onClick = { viewModel.toggleLike(h.summary.snapshot()) }) {
                Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(if (liked) R.string.home_hero_unlike else R.string.home_hero_like), tint = if (liked) JazzyColors.Accent else Color.White)
            }
            IconButton(onClick = { actions.openTitle(h.summary.ref, null, null, false) }) {
                Icon(Icons.Filled.Info, stringResource(R.string.home_hero_more_info_a11y), tint = Color.White)
            }
        }
    } ?: Box(Modifier.fillMaxWidth().heightIn(min = 140.dp))
}

@Composable
private fun TopBar(state: HomeUiState, actions: HomeActions) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = JazzyTheme.spacing.gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.home_brand_wordmark), style = MaterialTheme.typography.headlineSmall, color = JazzyColors.Accent, modifier = Modifier.weight(1f))
        IconButton(onClick = actions.openSearch) { Icon(Icons.Filled.Search, stringResource(R.string.home_topbar_search), tint = Color.White) }
        state.profile?.let { p ->
            AvatarTile(p.avatarId, Modifier.size(32.dp).jazzyClickable(onClick = actions.openProfiles), size = 32.dp)
        }
    }
}

@Composable
private fun TabChips(tab: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(Modifier.padding(horizontal = JazzyTheme.spacing.gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(HomeTab.ALL to R.string.home_tab_all, HomeTab.MOVIES to R.string.home_tab_movies, HomeTab.TV to R.string.home_tab_tv).forEach { (t, label) ->
            FilterChip(selected = t == tab, onClick = { onSelect(t) }, label = { Text(stringResource(label)) })
        }
    }
    Box(Modifier.height(4.dp))
}
