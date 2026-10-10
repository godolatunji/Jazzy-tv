package com.nexient.jazzy.feature.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexient.jazzy.core.designsystem.component.HeroBackdrop
import com.nexient.jazzy.core.designsystem.component.HeroText
import com.nexient.jazzy.core.designsystem.component.OfflineBanner
import com.nexient.jazzy.core.designsystem.theme.JazzyColors
import com.nexient.jazzy.core.designsystem.theme.JazzyTheme
import com.nexient.jazzy.feature.home.HomeActions
import com.nexient.jazzy.feature.home.HomeUiState
import com.nexient.jazzy.feature.home.HomeViewModel
import com.nexient.jazzy.feature.home.R
import com.nexient.jazzy.feature.home.snapshot
import kotlinx.coroutines.launch

/**
 * 10-foot Home: the billboard fills the top ~60% and follows the focused card (dynamic backdrop);
 * Back from deep in the rails scrolls to the top first (HM-6).
 */
@Composable
internal fun HomeTvScreen(state: HomeUiState, viewModel: HomeViewModel, actions: HomeActions) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    BackHandler(enabled = scrolled) { scope.launch { listState.animateScrollToItem(0) } }

    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(state.hero?.content, Modifier.fillMaxWidth().fillMaxHeight(0.65f).align(Alignment.TopEnd))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = JazzyTheme.spacing.verticalSafe, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "hero") {
                Box(Modifier.fillMaxWidth().height(340.dp).padding(horizontal = JazzyTheme.spacing.gutter)) {
                    state.hero?.let { hero ->
                        HeroText(hero.content, Modifier.align(Alignment.BottomStart)) {
                            val inList = hero.summary.ref in state.myList
                            val liked = hero.summary.ref in state.liked
                            Button(
                                onClick = { actions.openTitle(hero.summary.ref, null, null, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            ) { Icon(Icons.Filled.PlayArrow, null); Text(stringResource(R.string.home_hero_watch)) }
                            OutlinedButton(onClick = { viewModel.toggleMyList(hero.summary.snapshot()) }) {
                                Icon(if (inList) Icons.Filled.Check else Icons.Filled.Add, null); Text(stringResource(R.string.home_hero_my_list))
                            }
                            OutlinedButton(onClick = { viewModel.toggleLike(hero.summary.snapshot()) }) {
                                Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (liked) JazzyColors.Accent else Color.White)
                                Text(stringResource(if (liked) R.string.home_hero_liked else R.string.home_hero_like))
                            }
                            OutlinedButton(onClick = { actions.openTitle(hero.summary.ref, null, null, false) }) { Text(stringResource(R.string.home_hero_more_info)) }
                        }
                    }
                }
                if (state.offline) OfflineBanner()
            }
            items(state.rails, key = { it.key }) { rail ->
                HomeRailContent(rail, state, viewModel, actions, onFocusTitle = viewModel::onCardFocused)
            }
        }
    }
}
