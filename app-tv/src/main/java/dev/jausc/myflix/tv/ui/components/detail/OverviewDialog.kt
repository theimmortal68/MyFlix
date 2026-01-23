@file:Suppress("MagicNumber")

package dev.jausc.myflix.tv.ui.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jausc.myflix.tv.ui.components.TvCenteredPopup
import dev.jausc.myflix.tv.ui.theme.TvColors

/**
 * Dialog showing full overview text for an item.
 * Includes title, overview, and genres.
 */
@Composable
fun OverviewDialog(
    title: String,
    overview: String,
    genres: List<String>,
    onDismiss: () -> Unit,
) {
    TvCenteredPopup(
        visible = true,
        onDismiss = onDismiss,
        minWidth = 400.dp,
        maxWidth = 600.dp,
        maxHeight = 500.dp,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )

            // Genres
            if (genres.isNotEmpty()) {
                Text(
                    text = genres.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.BluePrimary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Full overview
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
