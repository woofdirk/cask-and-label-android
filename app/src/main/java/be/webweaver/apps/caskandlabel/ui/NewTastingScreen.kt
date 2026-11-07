package be.webweaver.apps.caskandlabel.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import be.webweaver.apps.caskandlabel.R
import be.webweaver.apps.caskandlabel.core.Score
import be.webweaver.apps.caskandlabel.core.Vocabulary
import be.webweaver.apps.caskandlabel.data.Photos
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTastingScreen(
    app: AppViewModel, online: Boolean, initialBottleId: String?, initialGroupId: String?, onDone: () -> Unit,
    vm: NewTastingViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.start(initialBottleId, initialGroupId) }
    BackHandler { if (!vm.back()) onDone() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(when (vm.step) {
                        NewTastingStep.CHOOSE_BOTTLE -> R.string.choose_bottle
                        NewTastingStep.NEW_BOTTLE -> R.string.new_bottle
                        NewTastingStep.TASTE -> R.string.new_tasting
                    }))
                },
                navigationIcon = {
                    IconButton(onClick = { if (!vm.back()) onDone() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (vm.step) {
                NewTastingStep.CHOOSE_BOTTLE -> ChooseBottle(vm)
                NewTastingStep.NEW_BOTTLE -> BottleForm(vm)
                NewTastingStep.TASTE -> TastingForm(vm, app, online) {
                    app.refresh()
                    onDone()
                }
            }
        }
    }
}

@Composable
private fun ChooseBottle(vm: NewTastingViewModel) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            vm.query, { vm.search(it) }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text(stringResource(R.string.search_bottle)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        OutlinedButton(onClick = { vm.startNewBottle() }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.Add, null)
            Text(stringResource(R.string.new_bottle), Modifier.padding(start = 8.dp))
        }
        ErrorText(vm.error, Modifier.padding(16.dp))
        if (vm.searching) CircularProgressIndicator(Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        LazyColumn(Modifier.fillMaxSize()) {
            items(vm.results, key = { it.id }) { b ->
                ListItem(
                    leadingContent = { LabelPhoto(b.labelPhotoPath, 44.dp) },
                    headlineContent = { Text(bottleTitle(context, b)) },
                    supportingContent = { Text(listOfNotNull(b.brand, factsLine(context, b).ifEmpty { null }).joinToString(" · ")) },
                    modifier = Modifier.clickable { vm.choose(b) },
                )
                HorizontalDivider()
            }
            if (!vm.searching && vm.results.isEmpty()) item {
                Text(stringResource(R.string.no_bottles_found), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottleForm(vm: NewTastingViewModel) {
    val context = LocalContext.current
    var askCamera by remember { mutableStateOf(false) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) vm.pendingCameraUri?.let { vm.setPhoto(it) }
    }
    // The QR scanner library declares CAMERA, so the system camera intent needs the permission granted first.
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = Photos.newCameraUri(context)
            vm.pendingCameraUri = uri
            takePicture.launch(uri)
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.setPhoto(it) }
    }
    LaunchedEffect(askCamera) {
        if (askCamera) { askCamera = false; cameraPermission.launch(Manifest.permission.CAMERA) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Label photo at the top, as in the iOS form (0.23).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val bitmap = remember(vm.labelJpeg) { vm.labelJpeg?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
            if (bitmap != null) {
                Image(bitmap, stringResource(R.string.label_photo), contentScale = ContentScale.Crop, modifier = Modifier.size(96.dp))
            } else {
                LabelPhoto(null, 96.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { askCamera = true }) {
                    Icon(Icons.Outlined.PhotoCamera, null); Text(stringResource(R.string.take_photo), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Outlined.PhotoLibrary, null); Text(stringResource(R.string.choose_photo), Modifier.padding(start = 8.dp))
                }
                if (vm.processingPhoto) CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
        Text(stringResource(R.string.photo_private_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Field(vm.name, { vm.name = it }, R.string.field_name_required)
        Field(vm.brand, { vm.brand = it }, R.string.field_brand)
        Field(vm.distillery, { vm.distillery = it }, R.string.field_distillery)
        Picker(R.string.field_kind, Vocabulary.kinds, vm.kind) { vm.kind = it }
        Picker(R.string.field_country, Vocabulary.countries, vm.country) { vm.country = it }
        Field(vm.region, { vm.region = it }, R.string.field_region)
        if (vm.country == "Schotland") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Vocabulary.scotchRegions.forEach { r -> FilterChip(vm.region == r, { vm.region = r }, label = { Text(r) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(vm.age, { vm.age = it }, R.string.field_age, KeyboardType.Number, Modifier.weight(1f),
                isError = NewTastingViewModel.parseIntOrBlank(vm.age) == false)
            Field(vm.abv, { vm.abv = it }, R.string.field_abv, KeyboardType.Decimal, Modifier.weight(1f),
                isError = NewTastingViewModel.parseDoubleOrBlank(vm.abv) == false)
            Field(vm.volume, { vm.volume = it }, R.string.field_volume_cl, KeyboardType.Decimal, Modifier.weight(1f),
                isError = NewTastingViewModel.parseDoubleOrBlank(vm.volume) == false)
        }
        Text(stringResource(R.string.field_peated), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(null to R.string.unknown, true to R.string.yes, false to R.string.no).forEachIndexed { i, (value, label) ->
                SegmentedButton(vm.peated == value, { vm.peated = value }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(stringResource(label)) }
            }
        }
        Text(stringResource(R.string.field_cask_types), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Vocabulary.caskTypes.forEach { c -> FilterChip(c in vm.caskTypes, { vm.toggle(vm.caskTypes, c) }, label = { Text(c) }) }
        }
        Text(stringResource(R.string.field_features), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Vocabulary.features.forEach { f -> FilterChip(f in vm.features, { vm.toggle(vm.features, f) }, label = { Text(f) }) }
        }
        Picker(R.string.field_flavor_profile, Vocabulary.flavorProfiles,
            if (vm.flavorProfileTouched) vm.flavorProfile else vm.suggestedFlavorProfile()) {
            vm.flavorProfile = it; vm.flavorProfileTouched = true
        }
        Text(stringResource(R.string.bottle_pending_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(enabled = vm.bottleFormValid && !vm.processingPhoto, onClick = { vm.bottleFormDone() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.next))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TastingForm(vm: NewTastingViewModel, app: AppViewModel, online: Boolean, onSaved: () -> Unit) {
    val context = LocalContext.current
    val groups by app.groups.collectAsStateWithLifecycle()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val title = vm.chosen?.let { bottleTitle(context, it) } ?: vm.name

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.score_value, Score.ofHalfSteps(vm.halfSteps)!!.format()), style = MaterialTheme.typography.titleMedium)
        StarRow(vm.halfSteps, 28.dp)
        Slider(
            value = vm.halfSteps.toFloat(), onValueChange = { vm.halfSteps = Math.round(it) },
            valueRange = Score.MIN_HALF_STEPS.toFloat()..Score.MAX_HALF_STEPS.toFloat(),
            steps = Score.MAX_HALF_STEPS - Score.MIN_HALF_STEPS - 1,
        )
        Field(vm.nose, { vm.nose = it }, R.string.nose, singleLine = false)
        Field(vm.palate, { vm.palate = it }, R.string.palate, singleLine = false)
        Field(vm.finish, { vm.finish = it }, R.string.finish, singleLine = false)
        Text(stringResource(R.string.flavor_tags), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Vocabulary.flavorWords.forEach { w -> FilterChip(w in vm.flavorWords, { vm.toggle(vm.flavorWords, w) }, label = { Text(w) }) }
        }
        Field(vm.price, { vm.price = it }, R.string.price_paid, KeyboardType.Decimal,
            isError = NewTastingViewModel.parseDoubleOrBlank(vm.price) == false)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatDateTime(vm.tastedAt), Modifier.weight(1f))
            TextButton(onClick = { pickDate = true }) { Text(stringResource(R.string.change_date)) }
            TextButton(onClick = { pickTime = true }) { Text(stringResource(R.string.change_time)) }
        }
        Picker(R.string.share_with, groups.map { it.id }, vm.groupId, allowEmpty = true,
            emptyLabel = stringResource(R.string.scope_private), display = { id -> groups.firstOrNull { it.id == id }?.name ?: id }) {
            vm.groupId = it
        }
        Text(stringResource(R.string.share_footer), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ErrorText(vm.error)
        Button(
            enabled = online && !vm.saving && NewTastingViewModel.parseDoubleOrBlank(vm.price) != false,
            onClick = { vm.save(onSaved) }, modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.save)) }
        if (vm.saving) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }

    val zone = ZoneId.systemDefault()
    if (pickDate) {
        val local = LocalDateTime.ofInstant(vm.tastedAt, zone)
        val state = rememberDatePickerState(initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.tastedAt = date.atTime(local.toLocalTime()).atZone(zone).toInstant()
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val local = LocalDateTime.ofInstant(vm.tastedAt, zone)
        val state = rememberTimePickerState(local.hour, local.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    vm.tastedAt = local.withHour(state.hour).withMinute(state.minute).atZone(zone).toInstant()
                    pickTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, label: Int, keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(), isError: Boolean = false, singleLine: Boolean = true,
) {
    OutlinedTextField(
        value, onChange, label = { Text(stringResource(label)) }, singleLine = singleLine, isError = isError,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Picker(
    label: Int, options: List<String>, value: String?, allowEmpty: Boolean = true, emptyLabel: String = "—",
    display: (String) -> String = { it }, onChange: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = value?.let(display) ?: emptyLabel, onValueChange = {}, readOnly = true,
            label = { Text(stringResource(label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            if (allowEmpty) DropdownMenuItem(text = { Text(emptyLabel) }, onClick = { onChange(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = { onChange(option); expanded = false })
            }
        }
    }
}
