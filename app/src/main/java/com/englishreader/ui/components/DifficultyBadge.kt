package com.englishreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.englishreader.domain.model.DifficultyLevel
import com.englishreader.ui.theme.DifficultyAdvanced
import com.englishreader.ui.theme.DifficultyEasy
import com.englishreader.ui.theme.DifficultyHard
import com.englishreader.ui.theme.DifficultyMedium

@Composable
fun DifficultyBadge(
    level: DifficultyLevel,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (level) {
        DifficultyLevel.EASY -> DifficultyEasy
        DifficultyLevel.MEDIUM -> DifficultyMedium
        DifficultyLevel.HARD -> DifficultyHard
        DifficultyLevel.ADVANCED -> DifficultyAdvanced
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = level.labelCn,
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor
        )
    }
}

@Composable
fun CategoryBadge(
    categoryName: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
