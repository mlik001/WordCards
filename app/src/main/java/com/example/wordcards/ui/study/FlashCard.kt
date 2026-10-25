package com.example.wordcards.ui.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.wordcards.R
import com.example.wordcards.ui.theme.WordCardsTheme

/**
 * 可翻转的单词卡。
 *
 * 翻转本质是绕 Y 轴转 180°：
 * - 0°~90° 显示正面；
 * - 超过 90° 时正面已经是"背面"（镜像），所以背面自己再转 180° 抵消镜像；
 * - `cameraDistance` 必须设，否则透视变形会非常夸张（默认值太近）。
 *
 * 高度写死而不是 wrapContent：正反面文本长度不同，否则翻到一半时卡片会整体跳动。
 */
@Composable
fun FlashCard(
    card: StudyCardUi,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "flash_card_flip"
    )
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .height(320.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = with(density) { 14.dp.toPx() }
            }
            .clickable(onClick = onFlip),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (rotation <= 90f) {
            CardFront(card = card)
        } else {
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                CardBack(card = card)
            }
        }
    }
}

@Composable
private fun CardFront(card: StudyCardUi) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.word,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        card.phonetic?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.study_flip_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CardBack(card: StudyCardUi) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.definition,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        card.example?.let { example ->
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun FlashCardFrontPreview() {
    WordCardsTheme {
        FlashCard(
            card = StudyCardUi(1, "abandon", "/əˈbændən/", "v. 放弃；抛弃", "He abandoned the plan.", 2),
            isFlipped = false,
            onFlip = {},
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun FlashCardBackPreview() {
    WordCardsTheme {
        FlashCard(
            card = StudyCardUi(1, "abandon", "/əˈbændən/", "v. 放弃；抛弃", "He abandoned the plan.", 2),
            isFlipped = true,
            onFlip = {},
            modifier = Modifier.padding(20.dp)
        )
    }
}
