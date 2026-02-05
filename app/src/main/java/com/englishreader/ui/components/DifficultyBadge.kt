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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishreader.domain.model.DifficultyLevel
import com.englishreader.ui.theme.DifficultyAdvanced
import com.englishreader.ui.theme.DifficultyEasy
import com.englishreader.ui.theme.DifficultyHard
import com.englishreader.ui.theme.DifficultyMedium

/**
 * Apple-style pill badge for difficulty level
 * Features: pill shape, muted colors, refined typography
 */
@Composable
fun DifficultyBadge(
    level: DifficultyLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (level) {
        DifficultyLevel.EASY -> Pair(
            DifficultyEasy.copy(alpha = 0.12f),
            DifficultyEasy
        )
        DifficultyLevel.MEDIUM -> Pair(
            DifficultyMedium.copy(alpha = 0.12f),
            DifficultyMedium
        )
        DifficultyLevel.HARD -> Pair(
            DifficultyHard.copy(alpha = 0.12f),
            DifficultyHard
        )
        DifficultyLevel.ADVANCED -> Pair(
            DifficultyAdvanced.copy(alpha = 0.12f),
            DifficultyAdvanced
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50)) // Full pill shape
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = level.labelCn,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Apple-style pill badge for category
 * Features: pill shape, customizable color, refined typography
 */
@Composable
fun CategoryBadge(
    categoryName: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50)) // Full pill shape
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Subtle text-only badge for metadata display
 */
@Composable
fun MetadataBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
