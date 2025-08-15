package id.kjlogistik.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.kjlogistik.app.data.model.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManifestListItem(manifest: Manifest, onClick: () -> Unit) {
    val isArrival = manifest.status == "IN_TRANSIT"
    val scannedCount = if (isArrival) manifest.arrivalScannedCount else manifest.scannedPackagesCount
    val progress by animateFloatAsState(
        targetValue = if (manifest.totalPackages > 0) scannedCount.toFloat() / manifest.totalPackages.toFloat() else 0f,
        label = "ManifestProgress"
    )

    // Using an Outlined Card for a cleaner look with a subtle border
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Left Column: Destination and Manifest Info ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Destination City: Primary information, large and bold
                Text(
                    text = manifest.destinationHub.address?.city ?: manifest.destinationHub.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Manifest Number: Secondary information, smaller and lighter
                Text(
                    text = "Manifest: ${manifest.manifestNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- Right Column: Circular Progress for Package Count ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 5.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    color = MaterialTheme.colorScheme.primary
                )
                // Text inside the circle for package count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$scannedCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/ ${manifest.totalPackages}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}