package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurfaceVariant

@Stable
class SwipeRefreshState(var isRefreshing: Boolean)

@Composable
fun rememberSwipeRefreshState(isRefreshing: Boolean): SwipeRefreshState {
    return remember(isRefreshing) { SwipeRefreshState(isRefreshing) }
}

/**
 * A pull-to-refresh / SwipeRefresh container for book lists and content grids,
 * ensuring library content remains up-to-date with remote and local Room cache.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier
            .testTag("swipe_refresh")
            .testTag("pull_to_refresh"),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .testTag("swipe_refresh_indicator")
                    .testTag("pull_to_refresh_indicator"),
                containerColor = DarkSurfaceVariant,
                color = AmberPrimary
            )
        }
    ) {
        content()
    }
}

@Composable
fun SwipeRefresh(
    state: SwipeRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SwipeRefresh(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        content = content
    )
}
