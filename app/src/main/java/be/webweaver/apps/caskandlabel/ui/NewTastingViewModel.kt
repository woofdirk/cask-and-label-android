package be.webweaver.apps.caskandlabel.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.webweaver.apps.caskandlabel.core.Score
import be.webweaver.apps.caskandlabel.core.Vocabulary
import be.webweaver.apps.caskandlabel.data.Bottle
import be.webweaver.apps.caskandlabel.data.BottleDraft
import be.webweaver.apps.caskandlabel.data.NewTasting
import be.webweaver.apps.caskandlabel.data.Photos
import be.webweaver.apps.caskandlabel.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

enum class NewTastingStep { CHOOSE_BOTTLE, NEW_BOTTLE, TASTE }

/**
 * New tasting: pick a bottle you may see on the server or enter a new one (with an optional label photo), then
 * score and notes, and choose "Delen met: Privé / <groep>". Saving writes straight to the server (online only).
 */
class NewTastingViewModel(app: Application) : AndroidViewModel(app) {
    var step by mutableStateOf(NewTastingStep.CHOOSE_BOTTLE)
    var query by mutableStateOf("")
    var results by mutableStateOf<List<Bottle>>(emptyList())
    var searching by mutableStateOf(false)
    var chosen by mutableStateOf<Bottle?>(null)

    // New bottle
    var name by mutableStateOf("")
    var brand by mutableStateOf("")
    var distillery by mutableStateOf("")
    var kind by mutableStateOf<String?>(null)
    var country by mutableStateOf<String?>(null)
    var region by mutableStateOf("")
    var age by mutableStateOf("")
    var abv by mutableStateOf("")
    var volume by mutableStateOf("70")
    val caskTypes = mutableStateListOf<String>()
    val features = mutableStateListOf<String>()
    var peated by mutableStateOf<Boolean?>(null)
    var flavorProfile by mutableStateOf<String?>(null)
    var flavorProfileTouched by mutableStateOf(false)
    var labelJpeg by mutableStateOf<ByteArray?>(null)
    var processingPhoto by mutableStateOf(false)
    var pendingCameraUri: Uri? = null

    // Tasting
    var halfSteps by mutableStateOf(14)
    var nose by mutableStateOf("")
    var palate by mutableStateOf("")
    var finish by mutableStateOf("")
    val flavorWords = mutableStateListOf<String>()
    var price by mutableStateOf("")
    var tastedAt by mutableStateOf(Instant.now())
    var groupId by mutableStateOf<String?>(null)

    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var started = false
    private var searchJob: Job? = null

    fun start(initialBottleId: String?, initialGroupId: String?) {
        if (started) return
        started = true
        groupId = initialGroupId
        if (initialBottleId != null) {
            viewModelScope.launch {
                runCatching { Repository.bottle(initialBottleId) }
                    .onSuccess { b -> if (b != null) choose(b) }
                    .onFailure { error = errorMessage(getApplication(), it) }
            }
        }
        search("")
    }

    fun search(text: String) {
        query = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            searching = true
            try {
                results = Repository.searchBottles(text)
                error = null
            } catch (e: Exception) {
                error = errorMessage(getApplication(), e)
            } finally {
                searching = false
            }
        }
    }

    fun choose(bottle: Bottle) {
        chosen = bottle
        step = NewTastingStep.TASTE
    }

    fun startNewBottle() {
        chosen = null
        if (name.isBlank()) name = query.trim()
        step = NewTastingStep.NEW_BOTTLE
    }

    fun suggestedFlavorProfile(): String? =
        Vocabulary.flavorProfile(peated, caskTypes.toList(), emptyList())

    fun toggle(list: MutableList<String>, value: String) {
        if (value in list) list.remove(value) else list.add(value)
    }

    fun setPhoto(uri: Uri) {
        processingPhoto = true
        viewModelScope.launch {
            labelJpeg = withContext(Dispatchers.IO) { runCatching { Photos.jpeg(getApplication(), uri) }.getOrNull() }
            processingPhoto = false
        }
    }

    val bottleFormValid: Boolean
        get() = name.isNotBlank() && parseIntOrBlank(age) != false && parseDoubleOrBlank(abv) != false &&
            parseDoubleOrBlank(volume) != false

    fun bottleFormDone() { if (bottleFormValid) step = NewTastingStep.TASTE }

    fun back(): Boolean {
        step = when (step) {
            NewTastingStep.CHOOSE_BOTTLE -> return false
            NewTastingStep.NEW_BOTTLE -> NewTastingStep.CHOOSE_BOTTLE
            NewTastingStep.TASTE -> if (chosen == null) NewTastingStep.NEW_BOTTLE else NewTastingStep.CHOOSE_BOTTLE
        }
        return true
    }

    fun save(onSaved: () -> Unit) {
        val user = Repository.currentUserId ?: return
        val score = Score.ofHalfSteps(halfSteps) ?: return
        saving = true
        error = null
        viewModelScope.launch {
            try {
                val bottleId = chosen?.id ?: Repository.addBottle(draft(), labelJpeg)
                Repository.addTasting(
                    NewTasting(
                        id = Repository.newId(), bottleId = bottleId, userId = user, groupId = groupId,
                        scoreHalfSteps = score.halfSteps, nose = nose.trim(), palate = palate.trim(), finish = finish.trim(),
                        flavorWords = Vocabulary.flavorWords.filter { it in flavorWords },
                        pricePaidEur = price.toDecimal(), tastedAt = tastedAt.toString(),
                    )
                )
                onSaved()
            } catch (e: Exception) {
                error = errorMessage(getApplication(), e)
            } finally {
                saving = false
            }
        }
    }

    private fun draft() = BottleDraft(
        name = name.trim(),
        brand = brand.trim().ifEmpty { null },
        distillery = distillery.trim().ifEmpty { null },
        kind = kind,
        country = country,
        region = region.trim().ifEmpty { null },
        ageYears = age.trim().toIntOrNull(),
        abv = abv.toDecimal(),
        volumeCl = volume.toDecimal(),
        caskTypes = Vocabulary.caskTypes.filter { it in caskTypes },
        features = Vocabulary.features.filter { it in features },
        peated = peated,
        flavorProfile = if (flavorProfileTouched) flavorProfile else suggestedFlavorProfile(),
    )

    companion object {
        fun String.toDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

        /** null = blank (fine), true = valid, false = invalid. */
        fun parseIntOrBlank(text: String): Boolean? =
            if (text.isBlank()) null else text.trim().toIntOrNull()?.let { it in 0..100 } ?: false

        fun parseDoubleOrBlank(text: String): Boolean? =
            if (text.isBlank()) null else text.toDecimal()?.let { it > 0 && it < 10_000 } ?: false
    }
}
