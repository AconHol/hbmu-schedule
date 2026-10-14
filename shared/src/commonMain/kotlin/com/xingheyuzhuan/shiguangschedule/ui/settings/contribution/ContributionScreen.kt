@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
// Modified by AconHol (2026): offline contributor data, attribution and grouped lists.
package com.xingheyuzhuan.shiguangschedule.ui.settings.contribution

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsCard as Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back_to_previous
import shiguangschedule.shared.generated.resources.a11y_contributor_avatar
import shiguangschedule.shared.generated.resources.action_retry
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.label_github
import shiguangschedule.shared.generated.resources.tab_adapter_development
import shiguangschedule.shared.generated.resources.tab_app_development
import shiguangschedule.shared.generated.resources.text_loading_failed
import shiguangschedule.shared.generated.resources.text_no_contributors
import shiguangschedule.shared.generated.resources.title_contribution_list

/**
 * 贡献者列表主界面
 *
 * @param onBack 返回上一页回调
 * @param viewModel 状态管理 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionScreen(
    onBack: () -> Unit,
    viewModel: ContributionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_contribution_list)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back_to_previous)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ContributionTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = viewModel::selectTab
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                when (val state = uiState) {
                    ContributionUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is ContributionUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ErrorMessage(
                                message = state.message,
                                onRetry = viewModel::loadContributions
                            )
                        }
                    }

                    is ContributionUiState.Success -> {
                        val listToShow = when (selectedTabIndex) {
                            0 -> state.data.appDev
                            1 -> state.data.jiaowuadapter
                            else -> emptyList()
                        }
                        ContributorListContent(
                            list = listToShow,
                            onContributorClick = { url ->
                                uriHandler.openUri(url)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 分类选项卡栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "本项目",
        "上游贡献者"
    )

    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = if (selectedTabIndex == index) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )
                }
            )
        }
    }
}

/**
 * 贡献者列表内容
 */
@Composable
fun ContributorListContent(
    list: List<ContributionList.Contributor>,
    onContributorClick: (String) -> Unit
) {
    if (list.isEmpty()) {
        Text(
            text = stringResource(Res.string.text_no_contributors),
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
        itemsIndexed(list, key = { _, contributor -> contributor.url }) { index, contributor ->
            ContributorCard(
                contributor = contributor,
                onContributorClick = onContributorClick,
                first = index == 0,
                last = index == list.lastIndex
            )
        }
    }
}

/**
 * 单个贡献者卡片组件
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ContributorCard(
    contributor: ContributionList.Contributor,
    onContributorClick: (String) -> Unit,
    first: Boolean = false,
    last: Boolean = false
) {
    val a11yAvatar = stringResource(Res.string.a11y_contributor_avatar, contributor.name)
    val labelGithub = stringResource(Res.string.label_github)

    val avatarBytes by produceState<ByteArray?>(initialValue = null, contributor.avatar) {
        value = if (contributor.avatar.isBlank()) null else try {
            Res.readBytes("files/contributors_data/${contributor.avatar}")
        } catch (_: Exception) {
            null
        }
    }

    Card(
        onClick = { onContributorClick(contributor.url) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(topStart = if (first) 24.dp else 4.dp,
            topEnd = if (first) 24.dp else 4.dp,
            bottomStart = if (last) 24.dp else 4.dp, bottomEnd = if (last) 24.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarBytes != null) AsyncImage(
                model = avatarBytes,
                contentDescription = a11yAvatar,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) else Surface(Modifier.size(48.dp), shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(contributor.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(contributor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (contributor.role.isNotBlank()) Text(contributor.role,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = labelGithub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 加载失败的错误提示页面
 */
@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    val textLoadingFailed = stringResource(Res.string.text_loading_failed, message)
    val actionRetry = stringResource(Res.string.action_retry)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = textLoadingFailed,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = onRetry, shapes = androidx.compose.material3.ButtonDefaults.shapes()) {
            Text(actionRetry)
        }
    }
}
