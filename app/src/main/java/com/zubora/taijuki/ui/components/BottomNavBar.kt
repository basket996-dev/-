package com.zubora.taijuki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zubora.taijuki.Tab
import com.zubora.taijuki.ui.theme.AppColors
import com.zubora.taijuki.ui.theme.AppTypography

@Composable
fun BottomNavBar(activeTab: Tab, accent: Color, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(AppColors.NavBar)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.BorderCard))
        Row(modifier = Modifier.fillMaxWidth()) {
            Tab.entries.forEach { tab ->
                val color = if (tab == activeTab) accent else AppColors.NavInactive
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    NavIcon(tab, tint = color, modifier = Modifier.size(20.dp))
                    Text(tab.label, style = AppTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color))
                }
            }
        }
    }
}
