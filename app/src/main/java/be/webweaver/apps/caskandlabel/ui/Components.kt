package be.webweaver.apps.caskandlabel.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.core.BottleTitle
import be.webweaver.apps.caskandlabel.core.Score
import be.webweaver.apps.caskandlabel.data.Bottle
import be.webweaver.apps.caskandlabel.data.Repository
import coil3.compose.AsyncImage
import io.github.jan.supabase.exceptions.HttpRequestException
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun parseInstant(text: String): Instant =
    runCatching { OffsetDateTime.parse(text).toInstant() }.getOrElse { runCatching { Instant.parse(text) }.getOrDefault(Instant.EPOCH) }

fun formatDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)

fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(instant)

fun bottleTitle(context: Context, bottle: Bottle?): String =
    bottle?.let { BottleTitle.title(it.name, it.ageYears) { age -> context.getString(R.string.age_years, age) } } ?: ""

/** "12 jaar · 46 % · Speyside". */
fun factsLine(context: Context, bottle: Bottle): String = buildList {
    bottle.ageYears?.let { add(context.getString(R.string.age_years, it)) }
    bottle.abv?.let { add(context.getString(R.string.abv_value, formatNumber(it))) }
    bottle.region?.let { add(it) }
}.joinToString(" · ")

fun formatNumber(value: Double): String =
    if (value == Math.rint(value)) value.toLong().toString()
    else String.format(java.util.Locale.getDefault(), "%.1f", value)

/** A readable message for a failed server call, in the app's words where we know the cause. */
fun errorMessage(context: Context, error: Throwable): String {
    val text = error.message.orEmpty()
    return when {
        error is HttpRequestException || error is IOException || error.cause is IOException ->
            context.getString(R.string.error_offline)
        text.contains("invalid login", ignoreCase = true) || text.contains("invalid_credentials", ignoreCase = true) ->
            context.getString(R.string.error_wrong_login)
        text.contains("not confirmed", ignoreCase = true) -> context.getString(R.string.error_not_confirmed)
        text.contains("invalid or expired invite", ignoreCase = true) -> context.getString(R.string.error_unknown_code)
        text.contains("already registered", ignoreCase = true) -> context.getString(R.string.error_already_registered)
        else -> context.getString(R.string.error_generic, text.lineSequence().firstOrNull().orEmpty())
    }
}

@Composable
fun ScoreLabel(halfSteps: Int, modifier: Modifier = Modifier) {
    val score = Score.ofHalfSteps(halfSteps) ?: return
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(score.format(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Ten stars, filled in half steps. */
@Composable
fun StarRow(halfSteps: Int, starSize: Dp = 22.dp) {
    Row {
        for (star in 1..10) {
            val icon = when {
                halfSteps >= star * 2 -> Icons.Filled.Star
                halfSteps == star * 2 - 1 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarOutline
            }
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(starSize))
        }
    }
}

/** A label photo from the private bucket (signed URL), or a placeholder. */
@Composable
fun LabelPhoto(path: String?, size: Dp, modifier: Modifier = Modifier) {
    var url by remember(path) { mutableStateOf<String?>(null) }
    LaunchedEffect(path) { url = path?.let { Repository.photoUrl(it) } }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = stringResource(R.string.label_photo),
                contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(Icons.Outlined.Liquor, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun OfflineBanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WifiOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Text(
            stringResource(R.string.offline_banner),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun ErrorText(text: String?, modifier: Modifier = Modifier) {
    if (text != null) Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}
