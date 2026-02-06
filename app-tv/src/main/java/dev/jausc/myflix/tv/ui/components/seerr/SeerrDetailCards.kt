@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.seerr

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "castCardHaloAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(132.dp),
    ) {
        Box {
            if (haloAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .blur(12.dp)
                        .background(TvColors.BluePrimary.copy(alpha = haloAlpha), CircleShape),
                )
            }

            Surface(
                onClick = onClick,
                modifier = Modifier
                    .size(132.dp)
                    .onFocusChanged { isFocused = it.isFocused },
                shape = ClickableSurfaceDefaults.shape(
                    shape = CircleShape,
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvColors.Surface,
                    focusedContainerColor = TvColors.Surface,
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, TvColors.BluePrimary),
                        shape = CircleShape,
                    ),
                ),
                scale = ClickableSurfaceDefaults.scale(
                    focusedScale = 1.05f,
                ),
            ) {
                AsyncImage(
                    model = seerrRepository.getProfileUrl(member.profilePath),
                    contentDescription = member.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isFocused) Color.White else TvColors.TextPrimary,
            maxLines = 1,
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

/**
 * Crew member card with circular profile image.
 */
@Composable
fun SeerrCrewCard(
    member: SeerrCrewMember,
    seerrRepository: SeerrRepository,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(132.dp),
    ) {
        Surface(
            onClick = {},
            modifier = Modifier.size(132.dp),
            shape = ClickableSurfaceDefaults.shape(
                shape = CircleShape,
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                focusedContainerColor = TvColors.Surface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, TvColors.BluePrimary),
                    shape = CircleShape,
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(
                focusedScale = 1.05f,
            ),
        ) {
            AsyncImage(
                model = seerrRepository.getProfileUrl(member.profilePath),
                contentDescription = member.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextPrimary,
            maxLines = 1,
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

/**
 * Related media poster card with title below poster (not overlaid).
 */
@Composable
fun SeerrRelatedMediaCard(
    media: SeerrMedia,
    seerrRepository: SeerrRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val haloAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "relatedCardHaloAlpha",
    )

    Column(modifier = modifier.width(110.dp)) {
        Box {
            if (haloAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(2f / 3f)
                        .blur(12.dp)
                        .background(TvColors.BluePrimary.copy(alpha = haloAlpha), RoundedCornerShape(8.dp)),
                )
            }

            Surface(
                onClick = onClick,
                modifier = Modifier
                    .width(110.dp)
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
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
            ) {
                AsyncImage(
                    model = seerrRepository.getPosterUrl(media.posterPath),
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Text(
            text = media.displayTitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) Color.White else TvColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 4.dp)
                .then(if (isFocused) Modifier.basicMarquee() else Modifier),
        )
    }
}

/**
 * YouTube video thumbnail card with title below thumbnail (not overlaid).
 */
@Composable
fun SeerrVideoCard(
    video: SeerrVideo,
    onClick: (videoKey: String, title: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier.width(210.dp)) {
        Surface(
            onClick = {
                video.key?.let { key ->
                    onClick(key, video.name ?: video.type)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
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
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        ) {
            Box {
                AsyncImage(
                    model = "https://img.youtube.com/vi/${video.key}/mqdefault.jpg",
                    contentDescription = video.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Play icon overlay with circle background
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(4.dp),
                    )
                }
            }
        }

        Text(
            text = video.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = if (isFocused) Color.White else TvColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
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
