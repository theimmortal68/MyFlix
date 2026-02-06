@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.seerr

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jausc.myflix.core.seerr.SeerrCastMember
import dev.jausc.myflix.core.seerr.SeerrCrewMember
import dev.jausc.myflix.core.seerr.SeerrMedia
import dev.jausc.myflix.core.seerr.SeerrMediaStatus
import dev.jausc.myflix.core.seerr.SeerrRepository
import dev.jausc.myflix.core.seerr.SeerrVideo
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Cast member card with circular profile image and blue halo focus effect.
 */
@Composable
fun SeerrCastCard(
    member: SeerrCastMember,
    seerrRepository: SeerrRepository,
    onClick: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "castCardHaloAlpha",
    )

    Box {
        if (haloAlpha > 0f) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .blur(12.dp)
                    .background(TvColors.BluePrimary.copy(alpha = haloAlpha), RoundedCornerShape(8.dp)),
            )
        }

        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(120.dp)
                .onFocusChanged { isFocused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(8.dp),
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = TvColors.Surface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvColors.BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1f,
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp),
            ) {
                AsyncImage(
                    model = seerrRepository.getProfileUrl(member.profilePath),
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextPrimary,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
                member.character?.let { character ->
                    Text(
                        text = character,
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Crew member card with circular profile image.
 */
@Composable
fun SeerrCrewCard(
    member: SeerrCrewMember,
    seerrRepository: SeerrRepository,
) {
    Surface(
        onClick = {},
        modifier = Modifier.width(120.dp),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvColors.Surface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            AsyncImage(
                model = seerrRepository.getProfileUrl(member.profilePath),
                contentDescription = member.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextPrimary,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            member.job?.let { job ->
                Text(
                    text = job,
                    style = MaterialTheme.typography.labelSmall,
                    color = TvColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Related media poster card with blue halo focus effect.
 */
@Composable
fun SeerrRelatedMediaCard(
    media: SeerrMedia,
    seerrRepository: SeerrRepository,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "relatedCardHaloAlpha",
    )

    Box {
        if (haloAlpha > 0f) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .blur(12.dp)
                    .background(TvColors.BluePrimary.copy(alpha = haloAlpha), RoundedCornerShape(8.dp)),
            )
        }

        Surface(
            onClick = onClick,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2f / 3f)
                .onFocusChanged { isFocused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(
                shape = RoundedCornerShape(8.dp),
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.FocusedSurface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvColors.BluePrimary),
                    shape = RoundedCornerShape(8.dp),
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = seerrRepository.getPosterUrl(media.posterPath),
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(8.dp),
                ) {
                    Text(
                        text = media.displayTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * YouTube video thumbnail card.
 */
@Composable
fun SeerrVideoCard(
    video: SeerrVideo,
    onClick: (videoKey: String, title: String?) -> Unit,
) {
    Surface(
        onClick = {
            video.key?.let { key ->
                onClick(key, video.name ?: video.type)
            }
        },
        modifier = Modifier
            .width(210.dp)
            .aspectRatio(16f / 9f),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            focusedContainerColor = TvColors.FocusedSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvColors.BluePrimary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg",
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Play video",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        ),
                    )
                    .padding(8.dp),
            ) {
                Text(
                    text = video.name ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Status badge (Available, Requested, Partial).
 */
@Composable
fun SeerrStatusBadge(status: Int?) {
    val (color, icon, text) = when (status) {
        SeerrMediaStatus.AVAILABLE -> Triple(
            Color(0xFF22C55E),
            Icons.Outlined.Check,
            "Available",
        )
        SeerrMediaStatus.PENDING, SeerrMediaStatus.PROCESSING -> Triple(
            Color(0xFFFBBF24),
            Icons.Outlined.Schedule,
            "Requested",
        )
        SeerrMediaStatus.PARTIALLY_AVAILABLE -> Triple(
            Color(0xFF60A5FA),
            Icons.Outlined.Check,
            "Partial",
        )
        else -> return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
