package com.nagabhushansn95.financetracker.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.data.investments.InvestmentSummary
import com.nagabhushansn95.financetracker.data.investments.SummaryLine
import com.nagabhushansn95.financetracker.ui.common.MoneyFormat

/** Which three columns the summary table shows; the rest sit in a row's expanded detail. */
enum class MetricView(val label: String, val columns: List<String>) {
    VALUE("Value", listOf("Invested", "Current", "Held P/L")),
    RETURNS("Returns", listOf("Realised", "Total P/L", "XIRR")),
    ALLOCATION("Allocation", listOf("% inv", "% cur", "Target")),
}

fun pct(value: Double?): String =
    value?.let { "%.1f%%".format(if (kotlin.math.abs(it) < 0.00005) 0.0 else it * 100) } ?: "—"
private fun short(value: Double) = MoneyFormat.compact(value)

@Composable
fun MetricViewChips(
    view: MetricView,
    onChange: (MetricView) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricView.entries.forEach { v ->
            FilterChip(
                selected = view == v,
                onClick = { onChange(v) },
                label = { Text(v.label) })
        }
    }
}

/** Text and sign (for colouring) of the three cells of [line] in [view]. */
private fun cells(
    line: SummaryLine,
    s: InvestmentSummary,
    view: MetricView
): List<Pair<String, Double?>> = when (view) {
    MetricView.VALUE -> listOf(
        short(line.invested) to null,
        short(line.current) to null,
        short(line.held) to line.held
    )

    MetricView.RETURNS -> listOf(
        short(line.realised) to line.realised,
        short(line.total) to line.total,
        pct(line.xirr) to null
    )

    MetricView.ALLOCATION -> {
        val off = line.offTarget(s.total.current)
        listOf(
            pct(s.investedShare(line)) to null, pct(s.currentShare(line)) to null,
            (line.targetPct?.let { pct(it) + (if (off) " !" else "") }
                ?: "—") to (if (off) -1.0 else null))
    }
}

/**
 * The summary as a narrow table: a name column and three metric columns that fit a phone. Tap a row to expand it:
 * every metric, the notes, and the sub-lines underneath.
 */
@Composable
fun SummaryTable(
    lines: List<SummaryLine>,
    summary: InvestmentSummary,
    view: MetricView,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    footer: SummaryLine? = null,
) {
    Column {
        Row(modifier = Modifier.padding(vertical = 6.dp)) {
            Box(modifier = Modifier.weight(1.4f))
            view.columns.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider()
        lines.forEach { line ->
            LineBlock(
                line,
                summary,
                view,
                key = line.name,
                indent = 0.dp,
                bold = true,
                expanded = expanded,
                onToggle = onToggle
            )
        }
        if (footer != null) {
            HorizontalDivider()
            LineRow(
                footer,
                summary,
                view,
                bold = true,
                indent = 0.dp,
                marker = null,
                onClick = null,
                bar = false
            )
        }
    }
}

@Composable
private fun LineBlock(
    line: SummaryLine,
    s: InvestmentSummary,
    view: MetricView,
    key: String,
    indent: Dp,
    bold: Boolean,
    expanded: Set<String>,
    onToggle: (String) -> Unit
) {
    val open = key in expanded
    LineRow(
        line,
        s,
        view,
        bold = bold,
        indent = indent,
        marker = if (open) "▾" else "▸",
        onClick = { onToggle(key) })
    if (open) {
        LineDetails(line, s, indent = indent + 12.dp)
        line.children.forEach { child ->
            LineBlock(
                child,
                s,
                view,
                key = "$key/${child.name}",
                indent = indent + 16.dp,
                bold = false,
                expanded = expanded,
                onToggle = onToggle
            )
        }
    }
}

@Composable
private fun LineRow(
    line: SummaryLine,
    s: InvestmentSummary,
    view: MetricView,
    bold: Boolean,
    indent: Dp,
    marker: String?,
    onClick: (() -> Unit)?,
    bar: Boolean = true
) {
    val weight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (marker?.let { "$it " } ?: "") + line.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = weight,
                modifier = Modifier
                    .weight(1.4f)
                    .padding(start = indent),
                maxLines = 2,
            )
            cells(line, s, view).forEach { (text, signed) ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = weight,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    color = signedColour(signed),
                )
            }
        }
        if (bar && view == MetricView.ALLOCATION && line.targetPct != null) {
            AllocationBar(
                share = s.currentShare(line),
                target = line.targetPct,
                tolerance = line.tolerancePct,
                modifier = Modifier.padding(start = indent, top = 3.dp)
            )
        }
    }
}

@Composable
private fun signedColour(signed: Double?) = when {
    signed == null -> MaterialTheme.colorScheme.onSurface
    signed < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.tertiary
}

/** Current share as a bar, the target as a marker with its tolerance band shaded around it. Scaled to the larger of the two. */
@Composable
fun AllocationBar(
    share: Double,
    target: Double,
    tolerance: Double?,
    modifier: Modifier = Modifier
) {
    val scale = maxOf(share, target + (tolerance ?: 0.0), 0.01) * 1.1
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (tolerance != null && tolerance > 0) {
            val from = ((target - tolerance) / scale).coerceIn(0.0, 1.0).toFloat()
            val to = ((target + tolerance) / scale).coerceIn(0.0, 1.0).toFloat()
            Row(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()) {
                if (from > 0f) Box(modifier = Modifier.weight(from)) {}
                Box(
                    modifier = Modifier
                        .weight((to - from).coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {}
                if (to < 1f) Box(modifier = Modifier.weight(1f - to)) {}
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth((share / scale).coerceIn(0.0, 1.0).toFloat())
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
        ) {}
        val t = (target / scale).coerceIn(0.0, 1.0).toFloat()
        Row(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()) {
            if (t > 0f) Box(modifier = Modifier.weight(t)) {}
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface)
            ) {}
            if (t < 1f) Box(modifier = Modifier.weight(1f - t)) {}
        }
    }
}

/** Everything about a line, two metrics per row, with full amounts. */
@Composable
fun LineDetails(line: SummaryLine, s: InvestmentSummary, indent: Dp) {
    val pairs: List<Triple<String, String, Double?>> = listOf(
        Triple("Invested", MoneyFormat.amount(line.invested), null),
        Triple("Current", MoneyFormat.amount(line.current), null),
        Triple("Held P/L", MoneyFormat.amount(line.held), line.held),
        Triple("Realised", MoneyFormat.amount(line.realised), line.realised),
        Triple("Total P/L", MoneyFormat.amount(line.total), line.total),
        Triple("Income", MoneyFormat.amount(line.income), null),
        Triple("XIRR", pct(line.xirr), null),
        Triple("Risk", line.riskLevel ?: "—", null),
        Triple("% of invested", pct(s.investedShare(line)), null),
        Triple("% of current", pct(s.currentShare(line)), null),
        Triple(
            "Target",
            line.targetPct?.let { pct(it) + (line.tolerancePct?.let { t -> " ± " + pct(t) } ?: "") }
                ?: "—",
            if (line.offTarget(s.total.current)) -1.0 else null),
        Triple(
            "Drift",
            line.drift(s.total.current)?.let { (if (it >= 0) "+" else "") + pct(it) } ?: "—",
            line.drift(s.total.current)
                ?.let { if (line.offTarget(s.total.current)) -1.0 else 1.0 }),
        Triple("Goal share", line.goalPct?.let(::pct) ?: "—", null),
        Triple("Wealth share", line.wealthPct?.let(::pct) ?: "—", null),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 2.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        pairs.chunked(2).forEach { pair ->
            Row {
                pair.forEach { (label, value, signed) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            color = signedColour(signed)
                        )
                    }
                }
            }
        }
        line.notes?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
