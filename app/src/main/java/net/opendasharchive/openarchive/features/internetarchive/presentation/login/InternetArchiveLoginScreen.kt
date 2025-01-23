package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.Theme
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.InternetArchiveHeader
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginAction.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginAction.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginAction.UpdatePassword
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginAction.UpdateUsername
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext
import org.koin.core.parameter.parametersOf
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginAction as Action

@Composable
fun InternetArchiveLoginScreen(space: Space, onResult: (IAResult) -> Unit) {
    val viewModel: InternetArchiveLoginViewModel = koinViewModel {
        parametersOf(space)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {})

    LaunchedEffect(Unit) {
        viewModel.actions.collect { action ->
            when (action) {
                is CreateLogin -> launcher.launch(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse(CreateLogin.URI)
                    )
                )

                is Action.Cancel -> onResult(IAResult.Cancelled)

                is Action.LoginSuccess -> onResult(IAResult.Saved)

                else -> Unit
            }
        }
    }

    InternetArchiveLoginContent(state, viewModel::dispatch)
}

@Composable
private fun InternetArchiveLoginContent(
    state: InternetArchiveLoginState, dispatch: Dispatch<Action>
) {

    // If extra paranoid could pre-hash password in memory
    // and use the store/dispatcher
    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(state.isLoginError) {
        while (state.isLoginError) {
            delay(3000)
            dispatch(Action.ErrorClear)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        InternetArchiveHeader(
            modifier = Modifier.padding(bottom = ThemeDimensions.spacing.large)
        )

        CustomTextField(
            value = state.username,
            onValueChange = { dispatch(UpdateUsername(it)) },
            label = stringResource(R.string.label_username),
            placeholder = stringResource(R.string.placeholder_email_or_username),
            isError = state.isUsernameError,
            isLoading = state.isBusy,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )

        Spacer(Modifier.height(ThemeDimensions.spacing.large))

        CustomSecureField(
            value = state.password,
            onValueChange = { dispatch(UpdatePassword(it)) },
            label = stringResource(R.string.label_password),
            placeholder = stringResource(R.string.placeholder_password),
            isError = state.isPasswordError,
            isLoading = state.isBusy,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        )

        Spacer(Modifier.height(ThemeDimensions.spacing.large))

        AnimatedVisibility(
            visible = state.isLoginError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = stringResource(R.string.error_incorrect_username_or_password),
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier
                .padding(top = ThemeDimensions.spacing.small)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.prompt_no_account),
                color = ThemeColors.material.onBackground
            )
            TextButton(
                modifier = Modifier.heightIn(ThemeDimensions.touchable),
                onClick = { dispatch(CreateLogin) }) {
                Text(
                    text = stringResource(R.string.label_create_login),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ThemeDimensions.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(ThemeDimensions.touchable)
                    .padding(ThemeDimensions.spacing.small),
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { dispatch(Action.Cancel) }) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                modifier = Modifier
                    .heightIn(ThemeDimensions.touchable)
                    .weight(1f),
                enabled = !state.isBusy && state.isValid,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { dispatch(Login) },
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(color = ThemeColors.material.primary)
                } else {
                    Text(stringResource(R.string.label_login))
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
private fun InternetArchiveLoginPreview() {
    DefaultScaffoldPreview {
        InternetArchiveLoginContent(
            state = InternetArchiveLoginState(
                username = "user@example.org", password = "abc123"
            )
        ) {}
    }
}

@Composable
fun DefaultScaffoldPreview(
    content: @Composable () -> Unit
) {

    Theme {

        Scaffold(
            topBar = {
                ComposeAppBar()
            }
        ) { paddingValues ->

            Box(modifier = Modifier.padding(paddingValues), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeAppBar(
    title: String = "Save App",
    onNavigationAction: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title)
        },
        navigationIcon = {
            IconButton(onClick = onNavigationAction) {
                Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = null)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    placeholder: String? = null,
    isError: Boolean = false,
    isLoading: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {

    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        enabled = !isLoading,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        placeholder = {
            placeholder?.let {
                Text(placeholder)
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = keyboardType,
            imeAction = imeAction,
            platformImeOptions = PlatformImeOptions(),
            showKeyboardOnFocus = true,
            hintLocales = null
        ),
        isError = isError,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
fun CustomSecureField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false,
    isLoading: Boolean = false,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
) {

    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }

    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        enabled = !isLoading,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        placeholder = {
            Text(placeholder)
        },
        singleLine = true,
        shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = keyboardType,
            imeAction = imeAction,
            platformImeOptions = PlatformImeOptions(),
            showKeyboardOnFocus = true,
            hintLocales = null
        ),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        isError = isError,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            IconButton(
                modifier = Modifier.sizeIn(ThemeDimensions.touchable),
                onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "show password"
                )
            }
        },
    )
}
