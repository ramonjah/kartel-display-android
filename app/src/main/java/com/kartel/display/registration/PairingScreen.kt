package com.kartel.display.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kartel.display.R

@Composable
fun PairingScreen(viewModel: PairingViewModel = viewModel(), onPaired: (String) -> Unit) {
    val state by viewModel.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.start() }
    androidx.compose.runtime.LaunchedEffect(state) {
        val s = state
        if (s is PairingState.Paired) onPaired(s.token)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(id = R.string.pairing_title), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))

        when (val s = state) {
            is PairingState.Requesting -> CircularProgressIndicator()
            is PairingState.ShowingCode -> {
                Text(text = s.code, fontSize = 96.sp, fontWeight = FontWeight.Bold, letterSpacing = 12.sp)
                Text(text = stringResource(id = R.string.pairing_subtitle), fontSize = 16.sp)
            }
            is PairingState.Expired -> Text(text = stringResource(id = R.string.pairing_expired))
            is PairingState.Paired -> CircularProgressIndicator()
            is PairingState.Error -> Text(text = "Ошибка: ${s.message}")
        }
    }
}
