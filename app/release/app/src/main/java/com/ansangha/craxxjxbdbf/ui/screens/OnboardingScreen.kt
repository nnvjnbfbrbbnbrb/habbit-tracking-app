package com.ansangha.craxxjxbdbf.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ansangha.craxxjxbdbf.ui.components.AuroraBackground
import com.ansangha.craxxjxbdbf.ui.theme.PremiumGradients
import com.ansangha.craxxjxbdbf.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumGradients.BackgroundGradient),
    ) {
        AuroraBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                OnboardingPage(
                    page = page,
                    parallax = pageOffset,
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(3) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (active) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) PremiumGradients.PrimaryGradient else Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.12f)),
                                    ),
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (pagerState.currentPage < pagerState.pageCount - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            viewModel.setOnboardingComplete(true)
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = PremiumGradients.BorderNeon,
                    ),
                ) {
                    Text(
                        text = if (pagerState.currentPage < pagerState.pageCount - 1) "Next" else "Enter the flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: Int,
    parallax: Float,
) {
    val density = LocalDensity.current.density
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    translationX = parallax * -36f
                    rotationY = parallax * -10f
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(36.dp))
                .background(PremiumGradients.CardGradient),
            contentAlignment = Alignment.Center,
        ) {
            IllustrationPlaceholder(page)
        }
        Spacer(modifier = Modifier.height(28.dp))
        val (title, body) = when (page) {
            0 -> "Depth without noise" to "Glass layers, neon edges, and motion that stays lightweight on every device."
            1 -> "Momentum you can feel" to "Streaks, pulse analytics, and micro-celebrations tuned for 60fps flows."
            else -> "Privacy-aware routines" to "Your reminders and scans stay battery-friendly—no heroics required."
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun IllustrationPlaceholder(page: Int) {
    val brush = when (page) {
        0 -> Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF22D3EE)))
        1 -> Brush.linearGradient(listOf(Color(0xFF22D3EE), Color(0xFFEAB308)))
        else -> Brush.linearGradient(listOf(Color(0xFFEAB308), Color(0xFF7C3AED)))
    }
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (page) {
                0 -> "◈"
                1 -> "◎"
                else -> "✶"
            },
            style = MaterialTheme.typography.displayMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}
