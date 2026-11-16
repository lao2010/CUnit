package com.cunit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cunit.app.ui.components.LiquidBottomTab
import com.cunit.app.ui.components.LiquidBottomTabs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private enum class Tab(val label: String, val icon: ImageVector) {
    Status("状态", AppIcons.Status),
    Add("存入", AppIcons.Add),
    Search("检索", AppIcons.Search),
    Settings("设置", AppIcons.Settings)
}

@Composable
fun CUnitApp(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) } // 默认进入「状态」页

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0C11))) {
        val backdrop = rememberLayerBackdrop {
            drawRect(Color(0xFF0B0C11))
            drawContent()
        }

        // 只有背景渐变被记入 backdrop。
        // 官方文档（glass-bottom-sheet 教程）：玻璃元素不能出现在 layerBackdrop 的记录范围内，
        // 否则"写 layer 的同时读 layer"形成循环，RenderThread SIGSEGV 闪退。
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            AppBackgroundGradient()
        }

        // 页面内容在记录范围之外，其中的玻璃卡片可安全读取 backdrop
        Box(Modifier.fillMaxSize()) {
            when (Tab.entries[tab]) {
                Tab.Status -> StatusPage(vm, backdrop)
                Tab.Add -> AddPage(vm, backdrop)
                Tab.Search -> SearchPage(vm, backdrop)
                Tab.Settings -> SettingsPage(vm, backdrop)
            }
        }

        // 玻璃底部导航
        GlassBottomBar(
            backdrop = backdrop,
            selectedTabIndex = { tab }, // 必须读 Compose 状态，官方组件靠 snapshotFlow 观察
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GlassBottomBar(
    backdrop: LayerBackdrop,
    selectedTabIndex: () -> Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 官方 LiquidBottomTabs 组件：整条玻璃胶囊栏 + 栏内折射高亮胶囊（可拖拽切换）
    LiquidBottomTabs(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onSelect,
        backdrop = backdrop,
        tabsCount = Tab.entries.size,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Tab.entries.forEachIndexed { index, tabItem ->
            LiquidBottomTab(onClick = { onSelect(index) }) {
                Icon(
                    imageVector = tabItem.icon,
                    contentDescription = tabItem.label,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    tabItem.label,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
