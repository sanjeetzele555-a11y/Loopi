package com.example.myapplication

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.ui.theme.LoopiColors
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val LoopiBlue = LoopiColors.Navy
private val LoopiBrightBlue = LoopiColors.Blue
private val LoopiYellow = LoopiColors.Amber
private val LoopiPink = LoopiColors.Pink
private val LoopiGreen = LoopiColors.Teal
private val LoopiRed = LoopiColors.Red
private val LoopiBackground = LoopiColors.Background

private const val PREFS_NAME = "loopi_auth"

private fun getAuthPreferences(
    context: Context
) = context.getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
)

private const val PASSWORD_ITERATIONS = 210_000

internal fun deriveSecretHash(secret: String, salt: ByteArray, iterations: Int = PASSWORD_ITERATIONS): ByteArray {
    val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, 256)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

private fun protectSecret(
    preferences: android.content.SharedPreferences,
    secret: String,
    saltKey: String,
    hashKey: String
) {
    val salt = ByteArray(16).also(SecureRandom()::nextBytes)
    val hash = deriveSecretHash(secret, salt)
    preferences.edit()
        .putString(saltKey, Base64.getEncoder().encodeToString(salt))
        .putString(hashKey, Base64.getEncoder().encodeToString(hash))
        .apply()
}

private fun verifySecret(
    preferences: android.content.SharedPreferences,
    secret: String,
    saltKey: String,
    hashKey: String
): Boolean {
    val salt = preferences.getString(saltKey, null)?.let {
        runCatching { Base64.getDecoder().decode(it) }.getOrNull()
    } ?: return false
    val expected = preferences.getString(hashKey, null)?.let {
        runCatching { Base64.getDecoder().decode(it) }.getOrNull()
    } ?: return false
    val actual = deriveSecretHash(secret, salt)
    return MessageDigest.isEqual(expected, actual)
}

private fun storePassword(preferences: android.content.SharedPreferences, password: String) {
    protectSecret(preferences, password, "password_salt", "password_hash")
    preferences.edit().remove("password").apply()
}

private fun verifyAndUpgradePassword(
    preferences: android.content.SharedPreferences,
    password: String
): Boolean {
    if (preferences.contains("password_hash")) {
        return verifySecret(preferences, password, "password_salt", "password_hash")
    }
    // Upgrade credentials created by the earlier local prototype on successful sign-in.
    val legacyPassword = preferences.getString("password", null) ?: return false
    if (legacyPassword != password) return false
    storePassword(preferences, password)
    return true
}

private fun createRecoveryCode(): String = ByteArray(8).also(SecureRandom()::nextBytes)
    .joinToString("") { "%02X".format(it.toInt() and 0xFF) }

private fun validEmail(
    email: String
): Boolean {

    return android.util.Patterns.EMAIL_ADDRESS
        .matcher(email.trim())
        .matches()
}

private fun validPassword(
    password: String
): Boolean {

    return password.length >= 6 &&
            password.any {
                it.isDigit()
            }
}

/* =========================================================
   1A — SPLASH
   ========================================================= */

@Composable
fun SplashScreen(
    navController: NavHostController
) {

    LaunchedEffect(Unit) {

        delay(1500)

        // Returning users with an active session skip onboarding.
        val prefs = getAuthPreferences(navController.context)
        val destination = when {
            !prefs.getBoolean("logged_in", false) -> "welcome"
            !prefs.getBoolean("setup_completed", false) -> "first_time_setup"
            else -> "home"
        }

        navController.navigate(
            destination
        ) {

            popUpTo(
                "splash"
            ) {
                inclusive = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                LoopiBackground
            )
            .padding(
                horizontal = 28.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "Loopi",
            fontSize = 48.sp,
            fontWeight =
                FontWeight.ExtraBold,
            color = LoopiBlue
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                "Learn. Review. Repeat.",
            fontSize = 15.sp,
            fontWeight =
                FontWeight.SemiBold,
            color = LoopiBlue
        )

        Spacer(
            modifier =
                Modifier.height(42.dp)
        )

        LoopiCycleLogo(
            modifier =
                Modifier.size(130.dp)
        )

        Spacer(
            modifier =
                Modifier.height(38.dp)
        )

        Text(
            text = "📚",
            fontSize = 76.sp
        )
    }
}

/* =========================================================
   1B — WELCOME
   ========================================================= */

@Composable
fun WelcomeScreen(
    navController: NavHostController
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                LoopiBackground
            )
            .padding(
                horizontal = 26.dp,
                vertical = 38.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Loopi",
            fontSize = 38.sp,
            fontWeight =
                FontWeight.ExtraBold,
            color = LoopiBlue
        )

        Text(
            text =
                "Learn. Review. Repeat.",
            fontSize = 14.sp,
            fontWeight =
                FontWeight.SemiBold,
            color = LoopiBlue
        )

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        Box(
            modifier = Modifier
                .size(190.dp)
                .background(
                    Brush.radialGradient(listOf(LoopiColors.BlueSoft, LoopiBackground)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎓📚",
                fontSize = 66.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        FeatureRow(Icons.Rounded.AutoAwesome, "Study smarter", "Flashcards and quizzes you create", LoopiBrightBlue)

        FeatureRow(Icons.Rounded.LocalFireDepartment, "Find weak topics", "Practise where you need it most", LoopiRed)

        FeatureRow(Icons.Rounded.Insights, "Track your progress", "Accuracy, streaks and confidence", LoopiPink)

        FeatureRow(Icons.Rounded.WifiOff, "Works offline", "Everything stays on your phone", LoopiGreen)

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Button(
            onClick = {

                navController.navigate(
                    "signup"
                )
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBlue
                )
        ) {

            Text(
                text =
                    "Get started →",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "Already have an account?",
                fontSize = 13.sp,
                color = Color.Gray
            )

            TextButton(
                onClick = {

                    navController.navigate(
                        "signin"
                    )
                }
            ) {

                Text(
                    text = "Sign in",
                    color = LoopiBlue,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/* =========================================================
   1C — SIGN IN
   ========================================================= */

@Composable
fun SignInScreen(
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val prefs =
        remember {
            getAuthPreferences(
                context
            )
        }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var emailError by remember {
        mutableStateOf<String?>(null)
    }

    var passwordError by remember {
        mutableStateOf<String?>(null)
    }

    var generalError by remember {
        mutableStateOf<String?>(null)
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    AuthPage(
        navController =
            navController
    ) {

        Text(
            text =
                "Welcome back 👋",
            fontSize = 29.sp,
            fontWeight =
                FontWeight.Bold,
            color = LoopiBlue
        )

        Text(
            text =
                "Sign in to continue learning",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        AuthLabel(
            "Email"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = LoopiColors.Muted) },

            value = email,

            onValueChange = {

                email = it
                emailError = null
                generalError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter your email"
                )
            },

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                ),

            singleLine = true,

            isError =
                emailError != null
        )

        emailError?.let {

            ErrorMessage(it)
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        AuthLabel(
            "Password"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = LoopiColors.Muted) },

            value = password,

            onValueChange = {

                password = it
                passwordError = null
                generalError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter your password"
                )
            },

            singleLine = true,

            visualTransformation =

                if (
                    passwordVisible
                ) {

                    VisualTransformation.None

                } else {

                    PasswordVisualTransformation()
                },

            trailingIcon = {

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = LoopiColors.Muted)
                }
            },

            isError =
                passwordError != null
        )

        passwordError?.let {

            ErrorMessage(it)
        }

        Box(
            modifier =
                Modifier.fillMaxWidth(),

            contentAlignment =
                Alignment.CenterEnd
        ) {

            TextButton(
                onClick = {

                    navController.navigate(
                        "forgot_password"
                    )
                }
            ) {

                Text(
                    text =
                        "Forgot password?",
                    color = LoopiBlue
                )
            }
        }

        generalError?.let {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 12.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(
                                0xFFFFEBEE
                            )
                    )
            ) {

                Text(
                    text = it,
                    color = LoopiRed,
                    fontSize = 13.sp,

                    modifier =
                        Modifier.padding(
                            12.dp
                        )
                )
            }
        }

        Button(
            onClick = {

                emailError = null
                passwordError = null
                generalError = null

                if (
                    !validEmail(
                        email
                    )
                ) {

                    emailError =
                        "Please enter a valid email"

                    return@Button
                }

                if (
                    password.isEmpty()
                ) {

                    passwordError =
                        "Password is required"

                    return@Button
                }

                val savedEmail =
                    prefs.getString(
                        "email",
                        null
                    )

                if (
                    savedEmail == null ||
                    (!prefs.contains("password_hash") && !prefs.contains("password"))
                ) {

                    generalError =
                        "No account found. Please sign up first."

                    return@Button
                }

                if (
                    !email.trim()
                        .equals(
                            savedEmail,
                            ignoreCase = true
                        ) ||
                    !verifyAndUpgradePassword(prefs, password)
                ) {

                    generalError =
                        "Incorrect email or password."

                    return@Button
                }

                prefs.edit()
                    .putBoolean(
                        "logged_in",
                        true
                    )
                    .apply()

                val setupCompleted =
                    prefs.getBoolean(
                        "setup_completed",
                        false
                    )

                if (
                    setupCompleted
                ) {

                    navController.navigate(
                        "home"
                    ) {

                        popUpTo(
                            "welcome"
                        ) {
                            inclusive = true
                        }
                    }

                } else {

                    navController.navigate(
                        "first_time_setup"
                    )
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBlue
                )
        ) {

            Text(
                text = "Sign in",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(LoopiColors.TealSoft, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = LoopiGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Your account and study data stay on this phone. No internet needed.",
                fontSize = 13.sp,
                color = LoopiColors.Ink
            )
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Row(
            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "Don't have an account?",
                fontSize = 13.sp
            )

            TextButton(
                onClick = {

                    navController.navigate(
                        "signup"
                    )
                }
            ) {

                Text(
                    text =
                        "Sign up",
                    color = LoopiBlue,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/* =========================================================
   1D / 1E / 1F — SIGN UP + VALIDATION
   ========================================================= */

@Composable
fun SignUpScreen(
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val prefs =
        remember {
            getAuthPreferences(
                context
            )
        }

    var fullName by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var fullNameError by remember {
        mutableStateOf<String?>(null)
    }

    var emailError by remember {
        mutableStateOf<String?>(null)
    }

    var passwordError by remember {
        mutableStateOf<String?>(null)
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    AuthPage(
        navController =
            navController
    ) {

        Text(
            text =
                "Create your account",
            fontSize = 27.sp,
            fontWeight =
                FontWeight.Bold,
            color = LoopiBlue
        )

        Text(
            text =
                "Join Loopi and start learning",
            color = Color.Gray
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        AuthLabel(
            "Full name"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = LoopiColors.Muted) },

            value =
                fullName,

            onValueChange = {

                fullName = it
                fullNameError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter your full name"
                )
            },

            singleLine = true,

            isError =
                fullNameError != null
        )

        fullNameError?.let {

            ErrorMessage(it)
        }

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        AuthLabel(
            "University email"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = LoopiColors.Muted) },

            value = email,

            onValueChange = {

                email = it
                emailError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter your university email"
                )
            },

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                ),

            singleLine = true,

            isError =
                emailError != null
        )

        emailError?.let {

            ErrorMessage(it)
        }

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        AuthLabel(
            "Password"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = LoopiColors.Muted) },

            value = password,

            onValueChange = {

                password = it
                passwordError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Create a password"
                )
            },

            singleLine = true,

            visualTransformation =

                if (
                    passwordVisible
                ) {

                    VisualTransformation.None

                } else {

                    PasswordVisualTransformation()
                },

            trailingIcon = {

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = LoopiColors.Muted)
                }
            },

            isError =
                passwordError != null
        )

        Text(
            text =
                "At least 6 characters with a number",

            fontSize = 12.sp,

            color =
                if (
                    passwordError != null
                ) {
                    LoopiRed
                } else {
                    Color.Gray
                }
        )

        passwordError?.let {

            ErrorMessage(it)
        }

        Spacer(
            modifier =
                Modifier.height(25.dp)
        )

        Button(
            onClick = {

                fullNameError =
                    if (
                        fullName.trim()
                            .isEmpty()
                    ) {
                        "Full name is required"
                    } else {
                        null
                    }

                emailError =
                    if (
                        !validEmail(
                            email
                        )
                    ) {
                        "Please enter a valid email"
                    } else {
                        null
                    }

                passwordError =
                    if (
                        !validPassword(
                            password
                        )
                    ) {
                        "Password must be at least 6 characters and include a number"
                    } else {
                        null
                    }

                if (
                    fullNameError == null &&
                    emailError == null &&
                    passwordError == null
                ) {

                    if (!prefs.getString("email", null).isNullOrBlank()) {
                        emailError = "A local account already exists. Sign in or use its recovery code."
                        return@Button
                    }

                    val recoveryCode = createRecoveryCode()
                    storePassword(prefs, password)
                    protectSecret(prefs, recoveryCode, "recovery_salt", "recovery_hash")

                    prefs.edit()
                        .putString(
                            "full_name",
                            fullName.trim()
                        )
                        .putString(
                            "email",
                            email.trim()
                                .lowercase()
                        )
                        .putString("recovery_code_display", recoveryCode)
                        .putBoolean(
                            "logged_in",
                            false
                        )
                        .putBoolean(
                            "setup_completed",
                            false
                        )
                        .apply()

                    navController.navigate(
                        "account_created"
                    )
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBlue
                )
        ) {

            Text(
                text =
                    "Sign up",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Row(
            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "Already have an account?",
                fontSize = 13.sp
            )

            TextButton(
                onClick = {

                    navController.navigate(
                        "signin"
                    )
                }
            ) {

                Text(
                    text =
                        "Sign in",
                    color = LoopiBlue,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/* =========================================================
   1G — ACCOUNT CREATED
   ========================================================= */

@Composable
fun AccountCreatedScreen(
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val prefs =
        remember {
            getAuthPreferences(
                context
            )
        }

    val fullName =
        prefs.getString(
            "full_name",
            ""
        ) ?: ""

    val recoveryCode = prefs.getString("recovery_code_display", "") ?: ""

    SuccessScreenLayout(
        emoji = "✅",

        title =
            "Account created!",

        description =

            if (
                fullName.isNotBlank()
            ) {

                "Welcome to Loopi, $fullName!\n\nRecovery code: $recoveryCode\nSave this code somewhere private. It is required to reset this offline account's password."

            } else {

                "Welcome to Loopi!\n\nRecovery code: $recoveryCode\nSave this code somewhere private. It is required to reset this offline account's password."
            },

        buttonText =
            "Continue →",

        onClick = {

            prefs.edit().remove("recovery_code_display").apply()

            navController.navigate(
                "first_time_setup"
            )
        }
    )
}

/* =========================================================
   1H — FORGOT PASSWORD
   ========================================================= */

@Composable
fun ForgotPasswordScreen(
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val prefs =
        remember {
            getAuthPreferences(
                context
            )
        }

    var email by remember {
        mutableStateOf("")
    }

    var recoveryCode by remember { mutableStateOf("") }

    var emailError by remember {
        mutableStateOf<String?>(null)
    }

    AuthPage(
        navController =
            navController
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "✉️",
            fontSize = 68.sp,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Text(
            text =
                "Forgot password?",

            fontSize = 28.sp,

            fontWeight =
                FontWeight.Bold,

            color = LoopiBlue,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "This account is stored on this device. Enter your email and the recovery code shown when you created the account.",

            textAlign =
                TextAlign.Center,

            color = Color.Gray
        )

        Spacer(
            modifier =
                Modifier.height(26.dp)
        )

        AuthLabel(
            "Email"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = LoopiColors.Muted) },

            value = email,

            onValueChange = {

                email = it
                emailError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter your university email"
                )
            },

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                ),

            singleLine = true,

            isError =
                emailError != null
        )

        emailError?.let {

            ErrorMessage(it)
        }

        Spacer(Modifier.height(12.dp))
        AuthLabel("Recovery code")
        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null, tint = LoopiColors.Muted) },

            value = recoveryCode,
            onValueChange = { recoveryCode = it.trim().uppercase(); emailError = null },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your recovery code") },
            singleLine = true,
            isError = emailError != null
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {

                if (
                    !validEmail(
                        email
                    )
                ) {

                    emailError =
                        "Please enter a valid email"

                    return@Button
                }

                val savedEmail =
                    prefs.getString(
                        "email",
                        null
                    )

                if (
                    savedEmail == null ||
                    !savedEmail.equals(
                        email.trim(),
                        ignoreCase = true
                    )
                ) {

                    emailError =
                        "Email or recovery code is incorrect."

                    return@Button
                }

                if (!verifySecret(prefs, recoveryCode, "recovery_salt", "recovery_hash")) {
                    emailError = "Email or recovery code is incorrect."
                    return@Button
                }

                prefs.edit()
                    .putString(
                        "reset_email",
                        email.trim()
                            .lowercase()
                    )
                    .putBoolean("reset_verified", true)
                    .apply()

                navController.navigate(
                    "reset_password"
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBlue
                )
        ) {

            Text("Verify and continue")
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        TextButton(
            onClick = {

                navController.navigate(
                    "signin"
                )
            },

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        ) {

            Text(
                text =
                    "Back to sign in",
                color = LoopiBlue
            )
        }
    }
}

/* =========================================================
   1I / 1J — RESET PASSWORD
   ========================================================= */

@Composable
fun ResetPasswordScreen(
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val prefs =
        remember {
            getAuthPreferences(
                context
            )
        }

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("reset_verified", false) || prefs.getString("reset_email", null).isNullOrBlank()) {
            navController.navigate("forgot_password") {
                popUpTo("reset_password") { inclusive = true }
            }
        }
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordError by remember {
        mutableStateOf<String?>(null)
    }

    var confirmError by remember {
        mutableStateOf<String?>(null)
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    AuthPage(
        navController =
            navController
    ) {

        Text(
            text =
                "Reset your password",

            fontSize = 27.sp,

            fontWeight =
                FontWeight.Bold,

            color = LoopiBlue
        )

        Text(
            text =
                "Create a new password for your account.",

            color = Color.Gray
        )

        Spacer(
            modifier =
                Modifier.height(26.dp)
        )

        AuthLabel(
            "New password"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = LoopiColors.Muted) },

            value = password,

            onValueChange = {

                password = it
                passwordError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Enter new password"
                )
            },

            visualTransformation =

                if (
                    passwordVisible
                ) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },

            trailingIcon = {

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = LoopiColors.Muted)
                }
            },

            singleLine = true,

            isError =
                passwordError != null
        )

        passwordError?.let {

            ErrorMessage(it)
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        AuthLabel(
            "Confirm password"
        )

        OutlinedTextField(
            shape = RoundedCornerShape(14.dp),
            colors = authFieldColors(),
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = LoopiColors.Muted) },

            value =
                confirmPassword,

            onValueChange = {

                confirmPassword = it
                confirmError = null
            },

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    "Confirm password"
                )
            },

            visualTransformation =
                PasswordVisualTransformation(),

            singleLine = true,

            isError =
                confirmError != null
        )

        confirmError?.let {

            ErrorMessage(it)
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {

                passwordError =
                    if (
                        !validPassword(
                            password
                        )
                    ) {

                        "Password must be at least 6 characters and include a number"

                    } else {

                        null
                    }

                confirmError =
                    if (
                        confirmPassword.isEmpty()
                    ) {

                        "Confirm password is required"

                    } else if (
                        confirmPassword != password
                    ) {

                        "Passwords do not match"

                    } else {

                        null
                    }

                if (
                    passwordError == null &&
                    confirmError == null
                ) {

                    storePassword(prefs, password)
                    prefs.edit()
                        .remove("reset_email")
                        .remove("reset_verified")
                        .apply()

                    navController.navigate(
                        "signin"
                    ) {

                        popUpTo(
                            "forgot_password"
                        ) {
                            inclusive = true
                        }
                    }
                }
            },

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBlue
                )
        ) {

            Text(
                "Reset password"
            )
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        TextButton(
            onClick = {

                navController.navigate(
                    "signin"
                )
            },

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        ) {

            Text(
                text =
                    "Back to sign in",
                color = LoopiBlue
            )
        }
    }
}

/* =========================================================
   1K — FIRST-TIME SETUP
   ========================================================= */

@Composable
fun FirstTimeSetupScreen(
    navController: NavHostController
) {

    val context =
        LocalContext.current

    val prefs =
        remember {
            getAuthPreferences(
                context
            )
        }

    val interests =
        listOf(
            "Computer Science",
            "Information Technology",
            "Engineering",
            "Business",
            "Mathematics",
            "Other"
        )

    val oldInterests =
        prefs.getStringSet(
            "interests",
            emptySet()
        ) ?: emptySet()

    var selectedInterests by remember {

        mutableStateOf(
            if (
                oldInterests.isEmpty()
            ) {

                setOf(
                    "Computer Science"
                )

            } else {

                oldInterests
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                LoopiBackground
            )
            .padding(26.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(40.dp)
        )

        Text(
            text =
                "Choose your interests",

            fontSize = 28.sp,

            fontWeight =
                FontWeight.Bold,

            color = LoopiBlue
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Select subjects you want to study. You can change this later.",

            color = Color.Gray,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        interests.forEach {
                interest ->

            val selected =
                selectedInterests
                    .contains(
                        interest
                    )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 5.dp
                    )
                    .clickable {

                        selectedInterests =

                            if (
                                selected
                            ) {

                                selectedInterests -
                                        interest

                            } else {

                                selectedInterests +
                                        interest
                            }
                    },

                colors =
                    CardDefaults.cardColors(
                        containerColor =

                            if (
                                selected
                            ) {

                                Color(
                                    0xFFEAF3FF
                                )

                            } else {

                                Color.White
                            }
                    )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked =
                            selected,

                        onCheckedChange = {

                            selectedInterests =

                                if (
                                    selected
                                ) {

                                    selectedInterests -
                                            interest

                                } else {

                                    selectedInterests +
                                            interest
                                }
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            interest,

                        fontWeight =

                            if (
                                selected
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Button(
            onClick = {

                prefs.edit()
                    .putStringSet(
                        "interests",
                        selectedInterests
                    )
                    .putBoolean(
                        "setup_completed",
                        true
                    )
                    .putBoolean(
                        "logged_in",
                        true
                    )
                    .apply()

                navController.navigate(
                    "setup_complete"
                )
            },

            enabled =
                selectedInterests
                    .isNotEmpty(),

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBrightBlue
                )
        ) {

            Text(
                text =
                    "Continue →",

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

/* =========================================================
   1L — SETUP COMPLETE
   ========================================================= */

@Composable
fun SetupCompletedScreen(
    navController: NavHostController
) {

    SuccessScreenLayout(
        emoji = "📋✅",

        title =
            "You're all set!",

        description =
            "Your profile is ready and you can start learning with Loopi.",

        buttonText =
            "Go to Home →",

        onClick = {

            navController.navigate(
                "home"
            ) {

                popUpTo(
                    "welcome"
                ) {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    )
}

/* =========================================================
   REUSABLE UI
   ========================================================= */

@Composable
private fun AuthPage(
    navController: NavHostController,
    content:
    @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                LoopiBackground
            ).statusBarsPadding()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 26.dp,
                vertical = 20.dp
            )
    ) {

        TextButton(
            onClick = {

                navController
                    .popBackStack()
            }
        ) {

            LoopiBackIcon(LoopiBlue)
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        content()
    }
}

@Composable
private fun AuthLabel(
    text: String
) {

    Text(
        text = text,
        fontWeight =
            FontWeight.SemiBold
    )

    Spacer(
        modifier =
            Modifier.height(6.dp)
    )
}

@Composable
private fun ErrorMessage(
    message: String
) {

    Text(
        text =
            "●  $message",

        color = LoopiRed,

        fontSize = 12.sp,

        modifier =
            Modifier.padding(
                top = 4.dp
            )
    )
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LoopiColors.Ink)
            Text(text = subtitle, fontSize = 13.sp, color = LoopiColors.Muted)
        }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = LoopiColors.RedSoft.copy(alpha = 0.4f),
    focusedBorderColor = LoopiColors.Blue,
    unfocusedBorderColor = LoopiColors.Border
)

@Composable
private fun SuccessScreenLayout(
    emoji: String,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                LoopiBackground
            )
            .padding(28.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "🎉",
            fontSize = 34.sp
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(
            text = emoji,
            fontSize = 80.sp
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = title,

            fontSize = 28.sp,

            fontWeight =
                FontWeight.Bold,

            color = LoopiBlue,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(
            text =
                description,

            color =
                Color.Gray,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(34.dp)
        )

        Button(
            onClick =
                onClick,

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        LoopiBrightBlue
                )
        ) {

            Text(
                text =
                    buttonText,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
fun LoopiCycleLogo(
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier =
            modifier
    ) {

        val strokeWidth =
            size.minDimension *
                    0.12f

        val padding =
            strokeWidth

        drawArc(
            color =
                LoopiGreen,

            startAngle =
                -80f,

            sweepAngle =
                95f,

            useCenter =
                false,

            topLeft =
                Offset(
                    padding,
                    padding
                ),

            size =
                Size(
                    size.width -
                            padding * 2,

                    size.height -
                            padding * 2
                ),

            style =
                Stroke(
                    width =
                        strokeWidth,

                    cap =
                        StrokeCap.Round
                )
        )

        drawArc(
            color =
                LoopiBrightBlue,

            startAngle =
                40f,

            sweepAngle =
                95f,

            useCenter =
                false,

            topLeft =
                Offset(
                    padding,
                    padding
                ),

            size =
                Size(
                    size.width -
                            padding * 2,

                    size.height -
                            padding * 2
                ),

            style =
                Stroke(
                    width =
                        strokeWidth,

                    cap =
                        StrokeCap.Round
                )
        )

        drawArc(
            color =
                LoopiYellow,

            startAngle =
                160f,

            sweepAngle =
                95f,

            useCenter =
                false,

            topLeft =
                Offset(
                    padding,
                    padding
                ),

            size =
                Size(
                    size.width -
                            padding * 2,

                    size.height -
                            padding * 2
                ),

            style =
                Stroke(
                    width =
                        strokeWidth,

                    cap =
                        StrokeCap.Round
                )
        )
    }
}

