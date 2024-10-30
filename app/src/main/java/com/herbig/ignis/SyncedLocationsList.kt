package com.herbig.ignis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.herbig.ignis.database.LocationEntity
import com.herbig.ignis.network.LocationSyncManager
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.ui.unit.sp
import com.herbig.ignis.database.LocationDB
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncedLocationsList(
    syncManager: LocationSyncManager,
    database: LocationDB,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.getDefault())

    val entries by syncManager.getSyncedLocationsFlow().collectAsState(initial = emptyList())
    val pending by database.getLocationCountFlow().collectAsState(initial = 0)

    Column (modifier = modifier.fillMaxSize()) {

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Pending: $pending",
            fontSize = 20.sp,
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                Row(entry = entry, dateFormat)
            }
        }
    }
}

@Composable
private fun Row(entry: LocationEntity, formatter: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Latitude: ${entry.latitude}")
            Text(text = "Longitude: ${entry.longitude}")
            Text(text = "Time: ${formatter.format(Date(entry.timestamp))}")
        }
    }
}
