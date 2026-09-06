package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun BentoNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Главная", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        NavItem("Пары", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_schedule"),
        NavItem("СРС", Icons.Filled.Assignment, Icons.Outlined.Assignment, "nav_srs"),
        NavItem("Профиль", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp))
            .border(1.dp, BentoBorderLight, RoundedCornerShape(28.dp))
            .testTag("bento_bottom_nav"),
        shape = RoundedCornerShape(28.dp),
        color = BentoSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) BentoPrimaryContainer else BentoSurface,
                    label = "tabBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) BentoOnPrimaryContainer else BentoOnSurfaceVariant.copy(alpha = 0.7f),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                lineHeight = 12.sp,
                                letterSpacing = (-0.2).sp
                            )
                        )
                    }
                }
            }
        }
    }
}
