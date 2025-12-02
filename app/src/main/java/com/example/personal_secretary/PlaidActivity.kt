package com.example.personal_secretary

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage
import java.net.UnknownHostException
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

class PlaidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme = ThemeList.currentTheme
            Box(modifier = Modifier.fillMaxSize()) {
                if (theme.backgroundRes != 0) {
                    Image(
                        painter = painterResource(id = theme.backgroundRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                Surface(modifier = Modifier.fillMaxSize(), color=Color.Transparent) {
                    PlaidScreen(onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaidScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var transactions by remember { mutableStateOf<List<PlaidTransaction>>(emptyList()) }
    var weeklyTotal by remember { mutableStateOf(0.0) }
    var monthlyTotal by remember { mutableStateOf(0.0) }
    var aiResponse by remember { mutableStateOf("Loading AI advice...") }

    var showTransactionModal by remember { mutableStateOf(false) }
    var showAIModal by remember { mutableStateOf(false) }

    val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val accessToken = createPlaidSandboxItem(context)
            val end = LocalDate.now()
            val start = end.minusDays(90)
            val fmt = DateTimeFormatter.ISO_DATE
            val list = getPlaidTransactionsWithRetry(
                context,
                accessToken,
                start.format(fmt),
                end.format(fmt)
            )
            transactions = list.sortedByDescending { it.date }.take(30)
            weeklyTotal = computeWeeklyTotal(transactions)
            monthlyTotal = computeMonthlyTotal(transactions)

            val recentTx =
                transactions.filter { LocalDate.parse(it.date) >= LocalDate.now().minusDays(7) }
            val promptText = buildString {
                append("Rate my spending and give me advice.\n")
                append("Note that the information I give you is all that I can give you.\n")
                append("Weekly total: $${"%.2f".format(weeklyTotal)}\n")
                append("Monthly total: $${"%.2f".format(monthlyTotal)}\n")
                append("Transactions in past 7 days:\n")
                recentTx.forEach { tx ->
                    append("- ${tx.date}: ${tx.name} $${"%.2f".format(tx.amount)}\n")
                }
            }
            scope.launch {
                aiResponse = sendToBackend(context, promptText)
            }

        } catch (e: Exception) {
            error = e.localizedMessage ?: "Unknown error"
            Log.e("PLAID_DEBUG", "Error loading sandbox data", e)
        } finally {
            loading = false
        }
    }

    val categoryTotals by remember(transactions) { mutableStateOf(computeCategoryTotals(transactions)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Habits (Beta)") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor=Color.Transparent
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            if (loading) {
                Column(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Loading transaction data from servers (Beta feature)")
                }
            }

            error?.let { Text(it, color = Color.Red) }

            if (!loading && error == null) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryCardDynamic("Weekly total", weeklyTotal, Color(0xFFB3E5FC))
                    SummaryCardDynamic("Monthly total", monthlyTotal, Color(0xFFFFF9C4))
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Spending by Category", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    SpendingPieChart(categoryTotals)
                    Spacer(Modifier.height(8.dp))
                }


                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    Button(onClick = { showTransactionModal = true }) {
                        Text("View Transactions (${transactions.size})")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { showAIModal = true }) {
                        Text("View Spending Suggestions")
                    }
                }

            }
        }



        if (showTransactionModal) {
            ModalBottomSheet(
                onDismissRequest = { showTransactionModal = false },
                sheetState = transactionSheetState
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Transactions", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(transactions) { tx ->
                            TransactionRow(tx)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }


        if (showAIModal) {
            ModalBottomSheet(
                onDismissRequest = { showAIModal = false },
                sheetState = aiSheetState
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Spending Review", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(
                                Color.LightGray.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(aiResponse)
                    }
                }
            }
        }
    }
}
@Composable
fun SummaryCardDynamic(label: String, amount: Double, backgroundColor: Color) {
    val amountText = "$${"%.2f".format(amount)}"
    val cardWidth = (label.length.coerceAtLeast(amountText.length) * 7).dp + 32.dp
    Card(
        modifier = Modifier.widthIn(min = cardWidth),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(amountText, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TransactionRow(tx: PlaidTransaction) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable {  }
        .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                tx.logoUrl?.let { url ->
                    AsyncImage(model = url, contentDescription = tx.merchantName ?: tx.name, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Column {
                    Text(tx.merchantName ?: tx.name, style = MaterialTheme.typography.bodyMedium)
                    Text(tx.personalFinanceCategory?.primary ?: tx.category?.firstOrNull() ?: "Other",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(tx.date, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("$${"%.2f".format(tx.amount)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun Context.getConfigValue(key: String): String? {
    val props = Properties()
    return try {
        val input = resources.openRawResource(R.raw.config)
        props.load(input)
        input.close()
        props.getProperty(key)
    } catch (e: Exception) {
        Log.e("CONFIG", "Error loading config", e)
        null
    }
}

@Serializable
data class BackendRequest(val prompt: String)

@Serializable
data class BackendResponse(val response: String)

interface BackendApi {
    @POST
    suspend fun sendPrompt(@Url url: String, @Body body: BackendRequest): Response<BackendResponse>
}

suspend fun sendToBackend(
    context: Context,
    prompt: String,
    retries: Int = 3,
    delayMs: Long = 1000L
): String {
    val backendUrl = context.getConfigValue("BACKEND_URL") ?: throw Exception("BACKEND_URL not set")

    val contentType = "application/json".toMediaType()
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(200, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(240, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(240, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://google.com/")
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
        .build()

    val api = retrofit.create(BackendApi::class.java)

    repeat(retries - 1) { attempt ->
        try {
            val response = api.sendPrompt(backendUrl, BackendRequest(prompt))
            return if (response.isSuccessful) {
                response.body()?.response ?: "Empty response"
            } else {
                "Error: ${response.code()} ${response.message()}"
            }
        } catch (e: Exception) {
            if (e is UnknownHostException || e.message?.contains("Unable to resolve host") == true) {

                delay(delayMs)
            } else {

                throw e
            }
        }
    }


    return try {
        val response = api.sendPrompt(backendUrl, BackendRequest(prompt))
        if (response.isSuccessful) {
            response.body()?.response ?: "Empty response"
        } else {
            "Error: ${response.code()} ${response.message()}"
        }
    } catch (e: Exception) {
        "Error fetching suggestions from the server: ${e.localizedMessage}"
    }
}

@Serializable
data class SandboxPublicTokenRequest(val client_id: String, val secret: String, val institution_id: String, val initial_products: List<String>)

@Serializable
data class SandboxPublicTokenResponse(val public_token: String? = null)

@Serializable
data class PublicTokenExchangeRequest(val client_id: String, val secret: String, val public_token: String)

@Serializable
data class PublicTokenExchangeResponse(val access_token: String? = null)

@Serializable
data class TransactionsGetRequest(val client_id: String, val secret: String, val access_token: String, val start_date: String, val end_date: String, val options: TransactionsGetOptions)

@Serializable
data class TransactionsGetOptions(val count: Int, val offset: Int = 0)

@Serializable
data class PlaidTransactionRaw(
    val name: String? = null,
    val amount: Double? = null,
    val date: String? = null,
    val category: List<String>? = null,
    val merchant_name: String? = null,
    val logo_url: String? = null,
    val personal_finance_category: PersonalFinanceCategoryRaw? = null,
    val payment_channel: String? = null
)

@Serializable
data class PersonalFinanceCategoryRaw(
    val confidence_level: String? = null,
    val detailed: String? = null,
    val primary: String? = null,
    val version: String? = null
)

interface PlaidApi {
    @POST("sandbox/public_token/create")
    suspend fun createSandboxToken(@Body body: SandboxPublicTokenRequest): SandboxPublicTokenResponse

    @POST("item/public_token/exchange")
    suspend fun exchangePublicToken(@Body body: PublicTokenExchangeRequest): PublicTokenExchangeResponse

    @POST("transactions/get")
    suspend fun getTransactions(@Body body: TransactionsGetRequest): TransactionsGetResponse
}

private suspend fun createPlaidSandboxItem(context: Context): String {
    val clientId = context.getConfigValue("PLAID_CLIENT_ID")!!
    val secret = context.getConfigValue("PLAID_SECRET")!!
    val contentType = "application/json".toMediaType()
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(logging).build()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://sandbox.plaid.com/")
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
        .build()
    val api = retrofit.create(PlaidApi::class.java)
    val req = SandboxPublicTokenRequest(clientId, secret, "ins_109508", listOf("transactions"))
    val publicResp = api.createSandboxToken(req)
    val publicToken = publicResp.public_token ?: throw Exception("Plaid did not return public_token")
    val exchangeReq = PublicTokenExchangeRequest(clientId, secret, publicToken)
    val exchangeResp = api.exchangePublicToken(exchangeReq)
    return exchangeResp.access_token ?: throw Exception("Plaid did not return access_token")
}

private suspend fun getPlaidTransactionsWithRetry(context: Context, accessToken: String, startDate: String, endDate: String): List<PlaidTransaction> {
    var attempts = 0
    val maxAttempts = 10
    while (attempts < maxAttempts) {
        try {
            return getPlaidTransactions(context, accessToken, startDate, endDate)
        } catch (e: Exception) {
            attempts++
            delay(5000L)
        }
    }
    throw Exception("Transactions product not ready after $maxAttempts attempts")
}

private fun computeWeeklyTotal(list: List<PlaidTransaction>): Double {
    val cutoff = LocalDate.now().minusDays(7)
    return list.filter { LocalDate.parse(it.date) >= cutoff }.sumOf { it.amount }
}

private fun computeMonthlyTotal(list: List<PlaidTransaction>): Double {
    val cutoff = LocalDate.now().minusMonths(1)
    return list.filter { LocalDate.parse(it.date) >= cutoff }.sumOf { it.amount }
}

data class PlaidTransaction(
    val name: String,
    val amount: Double,
    val date: String,
    val category: List<String>? = null,
    val merchantName: String? = null,
    val logoUrl: String? = null,
    val personalFinanceCategory: PersonalFinanceCategory? = null,
    val paymentChannel: String? = null
)

data class PersonalFinanceCategory(
    val confidenceLevel: String?,
    val detailed: String?,
    val primary: String?,
    val version: String?
)

@Composable
fun SpendingPieChart(categoryTotals: Map<String, Double>, modifier: Modifier = Modifier.size(200.dp)) {
    val total = categoryTotals.values.sum()
    val colors = listOf(
        Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835),
        Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFFFF7043),
        Color(0xFF00897B), Color(0xFF6D4C41), Color(0xFF7CB342),
        Color(0xFF5C6BC0), Color(0xFFEC407A), Color(0xFF26C6DA),
        Color(0xFFA1887F), Color(0xFFAB47BC), Color(0xFFFFCA28),
        Color(0xFF29B6F6)
    )
    Row (
        modifier=Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically){
         Canvas(modifier = modifier) {
             var startAngle = -90f
             var colorIndex = 0
           categoryTotals.forEach { (_, amount) ->
               val sweepAngle = (amount / total * 360).toFloat()
            drawArc(
                color = colors[colorIndex % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
            colorIndex++
        }
    }
            Spacer(Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.Center) {
                var colorIndex = 0
                categoryTotals.forEach { (category, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .background(Color.Black.copy(alpha=0.3f), shape=MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(colors[colorIndex % colors.size])
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("$category: $${"%.2f".format(amount)}", style = MaterialTheme.typography.bodyMedium,color=Color.White)
                    }
                    colorIndex++
                }
        }
    }
}

@Serializable
data class TransactionsGetResponse(
    val transactions: List<PlaidTransactionRaw> = emptyList()
)

private fun computeCategoryTotals(list: List<PlaidTransaction>): Map<String, Double> {
    return list.groupBy { it.personalFinanceCategory?.primary ?: "Other" }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
}

private fun PlaidTransactionRaw.toPlaidTransaction(): PlaidTransaction {
    return PlaidTransaction(
        name = this.name ?: "Unknown",
        amount = this.amount ?: 0.0,
        date = this.date ?: "",
        category = this.category,
        merchantName = this.merchant_name,
        logoUrl = this.logo_url,
        personalFinanceCategory = this.personal_finance_category?.let { pfc ->
            PersonalFinanceCategory(
                confidenceLevel = pfc.confidence_level,
                detailed = pfc.detailed,
                primary = pfc.primary,
                version = pfc.version
            )
        },
        paymentChannel = this.payment_channel
    )
}

private suspend fun getPlaidTransactions(
    context: Context,
    accessToken: String,
    startDate: String,
    endDate: String,
    count: Int = 30,
    offset: Int = 0
): List<PlaidTransaction> {
    val clientId = context.getConfigValue("PLAID_CLIENT_ID")!!
    val secret = context.getConfigValue("PLAID_SECRET")!!
    val contentType = "application/json".toMediaType()
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(logging).build()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://sandbox.plaid.com/")
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
        .build()
    val api = retrofit.create(PlaidApi::class.java)
    val req = TransactionsGetRequest(
        clientId,
        secret,
        accessToken,
        startDate,
        endDate,
        TransactionsGetOptions(count.coerceIn(1, 30), offset)
    )
    val rawResp = api.getTransactions(req)
    return rawResp.transactions.map { it.toPlaidTransaction() }
}