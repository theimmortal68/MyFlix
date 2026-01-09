package com.myflix.app.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

/**
 * Shimmer loading placeholder for the hero section.
 * Displays while featured items are being fetched.
 */
@Composable
fun HeroSectionShimmer(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerTranslateAnimation - 200f, 0f),
        end = Offset(shimmerTranslateAnimation, 0f)
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.30f)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Content placeholder on left side
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .padding(start = 48.dp, top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Title placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(36.dp),
                brush = shimmerBrush
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(24.dp),
                brush = shimmerBrush
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rating row placeholder
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(20.dp),
                        brush = shimmerBrush
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description placeholders (2 lines)
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(18.dp),
                brush = shimmerBrush
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(18.dp),
                brush = shimmerBrush
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Button placeholders
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp),
                    brush = shimmerBrush,
                    cornerRadius = 20.dp
                )
                
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    brush = shimmerBrush,
                    cornerRadius = 20.dp
                )
            }
        }
        
        // Page indicator placeholders
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { index ->
                ShimmerBox(
                    modifier = Modifier
                        .width(if (index == 0) 24.dp else 8.dp)
                        .height(8.dp),
                    brush = shimmerBrush,
                    cornerRadius = 4.dp
                )
            }
        }
    }
}

/**
 * Individual shimmer box component.
 */
@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    brush: Brush,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

/**
 * Empty state for when no featured items are available.
 */
@Composable
fun HeroSectionEmpty(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.30f)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // Optionally show a message or just leave empty
        // The hero section will be hidden entirely in production
    }
}
