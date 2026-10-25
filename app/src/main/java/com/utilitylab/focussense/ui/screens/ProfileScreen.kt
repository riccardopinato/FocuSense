package com.utilitylab.focussense.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.utilitylab.focussense.R
import com.utilitylab.focussense.auth.GoogleAccountManager
import com.utilitylab.focussense.ui.MainViewModel
import com.utilitylab.focussense.ui.components.CalibrationCard
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(vm:MainViewModel){
    val prefs by vm.preferences.collectAsStateWithLifecycle(); val profile by vm.personalProfile.collectAsStateWithLifecycle(); val context=LocalContext.current; val activity=context as? Activity; val scope=rememberCoroutineScope(); val message=remember{mutableStateOf<String?>(null)}; val serverClientId=stringResource(R.string.google_server_client_id)
    LaunchedEffect(Unit){vm.refreshPersonalProfile()}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Profilo",style=MaterialTheme.typography.headlineLarge)
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(prefs.signedIn){Text(prefs.googleDisplayName.ifBlank{"Account Google"},style=MaterialTheme.typography.titleLarge);Text(prefs.googleEmail);OutlinedButton(onClick={if(activity!=null)scope.launch{runCatching{GoogleAccountManager(activity).signOut()};vm.clearGoogleProfile()}}){Text("Esci dall'account")}}
            else {Text("Account Google",style=MaterialTheme.typography.titleLarge);Text("Collega un account per associare il profilo all'app. Il backup Android può trasferire dati e preferenze, ma non è un cloud sync realtime.");Button(onClick={if(activity!=null)scope.launch{val m=GoogleAccountManager(activity);runCatching{m.signIn(serverClientId)}.onSuccess{p->vm.saveGoogleProfile(p.uniqueId,p.email,p.displayName,p.photoUri);message.value="Account collegato"}.onFailure{message.value=it.message?:"Accesso non riuscito"}}}){Text("Accedi con Google")}}
            message.value?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
        }}
        CalibrationCard(profile)
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Promemoria postura",style=MaterialTheme.typography.titleLarge);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Attivo ogni ${prefs.postureReminderMinutes} min");Switch(prefs.postureReminderEnabled,{vm.setPostureReminder(it,prefs.postureReminderMinutes)})};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(20,30,45).forEach{m->OutlinedButton(onClick={vm.setPostureReminder(prefs.postureReminderEnabled,m)}){Text("$m min")}}}}}
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Privacy e limiti",style=MaterialTheme.typography.titleLarge);Text("L'audio non viene registrato né salvato: vengono memorizzati soltanto valori numerici dBFS e dati dei sensori sul dispositivo.");Text("La postura è un promemoria con autovalutazione: il telefono non misura la postura del corpo.");Text("Il profilo personale resta locale ed è derivato dalle tue valutazioni. FocuSense non è un dispositivo medico né un fonometro certificato.")}}
    }
}
