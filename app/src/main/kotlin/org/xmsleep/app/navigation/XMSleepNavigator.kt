package org.xmsleep.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * 导航器接口，封装所有导航逻辑
 */
interface XMSleepNavigator {
    val navController: NavHostController
    
    /**
     * 导航到主题设置页面
     */
    fun navigateToTheme() {
        navController.navigate("theme")
    }
    
    /**
     * 导航到收藏页面
     */
    fun navigateToFavorite() {
        navController.navigate("favorite")
    }
    
    /**
     * 返回上一页
     */
    fun popBackStack() {
        navController.popBackStack()
    }
    
    /**
     * 返回主页
     */
    fun navigateToMain() {
        navController.navigate("main") {
            // 清除所有返回栈，只保留main页面
            popUpTo("main") { inclusive = true }
        }
    }
}

/**
 * LocalNavigator，用于在组件中访问导航器
 */
val LocalNavigator = compositionLocalOf<XMSleepNavigator> {
    error("Navigator not found. Make sure to provide LocalNavigator in your composition.")
}

/**
 * 创建导航器实例
 */
@Composable
fun rememberXMSleepNavigator(): XMSleepNavigator {
    val navController = rememberNavController()
    return remember(navController) {
        object : XMSleepNavigator {
            override val navController: NavHostController = navController
        }
    }
}

/**
 * 提供导航器的CompositionLocalProvider
 */
@Composable
fun ProvideNavigator(
    navigator: XMSleepNavigator,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavigator provides navigator) {
        content()
    }
}

