package com.example.personal_secretary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

class WeatherActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherCard()
                }
            }
        }
    }
}

@Composable
fun WeatherCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var weatherText by remember { mutableStateOf("Loading weather...") }
    var isLoading by remember { mutableStateOf(true) }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scope.launch {
                    fetchWeather(context) { result ->
                        weatherText = result
                        isLoading = false
                    }
                }
            } else {
                weatherText = "Location permission denied"
                isLoading = false
            }
        }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                fetchWeather(context) { result ->
                    weatherText = result
                    isLoading = false
                }
            }
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Weather",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Text(
                    text = weatherText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

interface WeatherApi {
    @GET("onecall/day_summary")
    suspend fun getDaySummary(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("date") date: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "imperial"
    ): DaySummaryResponse
}

suspend fun fetchWeather(context: Context, onResult: (String) -> Unit) {
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)


        var location = fused.lastLocation.await()


        if (location == null) {
            location = requestFreshLocation(context, fused)
        }

        if (location == null) {
            onResult("Unable to get location (GPS off or emulator has no location)")
            return
        }


        val lat = location.latitude
        val lon = location.longitude

        val apiKey = context.getApiKey("OPENWEATHERMAP_API_KEY") ?: ""
        if (apiKey.isEmpty()) {
            onResult("Missing OpenWeatherMap API key")
            return
        }


        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/3.0/")
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
            .build()


        val api = retrofit.create(WeatherApi::class.java)


        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val response = api.getDaySummary(lat, lon, today, apiKey)

        val tempMin = response.temperature.min
        val tempMax = response.temperature.max
        val humidityAfternoon = response.humidity.afternoon
        val cloudAfternoon = response.cloud_cover.afternoon

        onResult(
            "Min Temperature: $tempMin F\n" +
                    "Max Temperature: $tempMax F\n" +
                    "Afternoon Humidity: $humidityAfternoon%\n" +
                    "Afternoon Cloud Cover: $cloudAfternoon%"
        )
    } catch (e: Exception) {
        e.printStackTrace()
        onResult("Error fetching weather: ${e.localizedMessage}")

    }
}

suspend fun requestFreshLocation(
    context: Context,
    fused: FusedLocationProviderClient
) = suspendCancellableCoroutine<android.location.Location?> { cont ->

    val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000L
    )
        .setWaitForAccurateLocation(true)
        .setMaxUpdates(1)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            fused.removeLocationUpdates(this)
            if (cont.isActive) {
                cont.resume(result.lastLocation)
            }
        }
    }

    // Begin listening
    fused.requestLocationUpdates(request, callback, context.mainLooper)

    // Timeout fallback
    kotlinx.coroutines.GlobalScope.launch {
        kotlinx.coroutines.delay(4000L)
        if (cont.isActive) {
            fused.removeLocationUpdates(callback)
            cont.resume(null)
        }
    }
}

// Helper to read API key
fun Context.getApiKey(key: String): String? {
    return try {
        val props = java.util.Properties()
        val inputStream = resources.openRawResource(R.raw.config)
        props.load(inputStream)
        inputStream.close()
        val value = props.getProperty(key)
        if (value.isNullOrEmpty()) {
            println("⚠️ API key '$key' not found in config.properties")
        }
        value
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Data classes
@Serializable
data class DaySummaryResponse(
    val lat: Double,
    val lon: Double,
    val date: String,
    val temperature: Temperature,
    val humidity: Humidity,
    val cloud_cover: CloudCover
)

@Serializable
data class Temperature(
    val min: Float,
    val max: Float,
    val afternoon: Float,
    val night: Float,
    val evening: Float,
    val morning: Float
)

@Serializable
data class Humidity(
    val afternoon: Float
)

@Serializable
data class CloudCover(
    val afternoon: Float
)
