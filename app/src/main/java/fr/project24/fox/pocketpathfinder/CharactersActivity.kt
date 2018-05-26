package fr.project24.fox.pocketpathfinder

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import fr.project24.fox.pocketpathfinder.model.PPFCharacter
import junit.framework.TestSuite.warning
import kotlinx.android.synthetic.main.activity_characters.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


class CharactersActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, AnkoLogger {
    companion object {
        const val REQUEST_ACCOUNT_PICKER = 1000
        const val REQUEST_AUTHORIZATION = 1001
        const val REQUEST_GOOGLE_PLAY_SERVICES = 1002
        const val REQUEST_PERMISSION_GET_ACCOUNTS = 1003

        private const val PREF_ACCOUNT_NAME = "accountName"
        private val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY)
    }

    private lateinit var mCredential: GoogleAccountCredential
    private val channel = Channel<List<PPFCharacter>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCredential = GoogleAccountCredential.usingOAuth2(
                this, SCOPES)
                .setBackOff(ExponentialBackOff())

        setContentView(R.layout.activity_characters)
        getResultsFromApi()

        launch(kotlinx.coroutines.experimental.android.UI) {
            drawCharacterCards(channel.receive())
        }
    }


    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        // Do nothing
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        // Do nothing
    }

    private fun getResultsFromApi() {
        info("getResultsFromApi()")
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices()
        } else if (mCredential.selectedAccountName == null) {
            info("chooseAccount()")
            chooseAccount()
        } else if (!Utils.isNetwork(this)) {
            warning("No network connection available.")
        } else {
            info("Requesting API")
            toast(getString(R.string.requestingMessage))
            characters_progress.visibility = View.VISIBLE
            doAsync {
                try {
                    val result = request(mCredential)
                    channel.offer(result)
                } catch (e: UserRecoverableAuthIOException) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                } catch (e: GoogleAuthIOException) {
                    toast(getString(R.string.api_authentication_failed))
                } finally {
                    uiThread {
                        characters_progress.visibility = View.GONE
                    }
                }
            }

        }
    }

    private fun drawCharacterCards(results: List<PPFCharacter>) {
        results.forEach { character ->
            info(character)

            characters_layout.tableLayout {
                lparams(height = wrapContent, width = matchParent) {
                    topMargin = dip(10)
                    padding = dip(10)
                    isStretchAllColumns = true
                }
                backgroundColor = Color.rgb(180, 220, 200)

                tableRow {
                    padding = dip(5)
                    textView(character.playerName).lparams {
                        span = 2
                    }
                }

                tableRow {
                    padding = dip(5)
                    textView(character.name)
                    textView(character.race + " " + character.ppfClass)
                }

                tableRow {
                    padding = dip(5)
                    textView("Niveau : " + character.level)
                    textView("Experience : " + character.experience)
                }

                tableRow {
                    padding = dip(5)

                    linearLayout {
                        textView("Points de vie : " + character.currentHp + "/" + character.maxHp + " ")
                        horizontalProgressBar {
                            id = R.id.progress_horizontal
                            progress = (character.currentHp.toFloat() / character.maxHp.toFloat() * 100).toInt()
                        }.lparams(width = matchParent) {
                            setMargins(dip(10), 0, 0, 0)
                        }
                    }.lparams(width = matchParent) {
                        span = 2
                    }
                }
            }
        }
    }

    private fun request(credential: GoogleAccountCredential): List<PPFCharacter> {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val mService = com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("PocketPathfinder")
                .build()
        val spreadsheetId = getSharedPreferences("Preferences", Context.MODE_PRIVATE).getString("characterSheetId", "")
        val characterNames = retrieveCharacterNames(mService, spreadsheetId)

        return characterNames.map { retrieveCharacterInfo(mService, spreadsheetId, it) }
    }

    private fun retrieveCharacterInfo(mService: Sheets, spreadsheetId: String, characterName: String): PPFCharacter {
        val statsRange = "$characterName!D11:D"
        val levelRange = "$characterName!R5"
        val raceRange = "$characterName!H4"
        val ppfClassRange = "$characterName!H5"
        val playerNameRange = "$characterName!D6"
        val experienceRange = "$characterName!R4"
        val currentHpRange = "$characterName!R6"
        val maxHpRange = "$characterName!S6"

        val request = mService.spreadsheets().values()
                .batchGet(spreadsheetId)
                .setRanges(listOf(
                        statsRange,
                        levelRange,
                        raceRange,
                        ppfClassRange,
                        playerNameRange,
                        experienceRange,
                        currentHpRange,
                        maxHpRange
                ))


        val response = request.execute()

        val stats = response.valueRanges[0].getValues().take(6).map { it[0].toString().toInt() }
        val level = response.valueRanges[1].getValues()[0][0].toString().toInt()
        val race = response.valueRanges[2].getValues()[0][0].toString()
        val ppfClass = response.valueRanges[3].getValues()[0][0].toString()
        val playerName = response.valueRanges[4].getValues()[0][0].toString()
        val experience = response.valueRanges[5].getValues()[0][0].toString().toInt()
        val currentHp = response.valueRanges[6].getValues()[0][0].toString().toInt()
        val maxHp = response.valueRanges[7].getValues()[0][0].toString().toInt()

        return PPFCharacter(
                playerName = playerName,
                name = characterName,
                level = level,
                experience = experience,
                race = race,
                ppfClass = ppfClass,
                currentHp = currentHp,
                maxHp = maxHp,
                strength = stats[0],
                dexterity = stats[1],
                constitution = stats[2],
                intelligence = stats[3],
                wisdom = stats[4],
                charisma = stats[5]
        )
    }

    private fun retrieveCharacterNames(mService: Sheets, spreadsheetId: String): List<String> {
        val response = mService.spreadsheets()[spreadsheetId].execute()
        return response.sheets.map { sheet -> sheet.properties.title }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private fun chooseAccount() {
        if (EasyPermissions.hasPermissions(
                        this, Manifest.permission.GET_ACCOUNTS)) {
            val accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null)
            if (accountName != null) {
                mCredential.selectedAccountName = accountName
                getResultsFromApi()
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER)
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS)
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     * activity result.
     * @param data Intent (containing result data) returned by incoming
     * activity result.
     */
    override fun onActivityResult(
            requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {
                toast(
                        "This app requires Google Play Services. Please install " + "Google Play Services on your device and relaunch this app.")
            } else {
                getResultsFromApi()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null &&
                    data.extras != null) {
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    val editor = getPreferences(Context.MODE_PRIVATE).edit()
                    editor.putString(PREF_ACCOUNT_NAME, accountName)
                    editor.apply()
                    mCredential.selectedAccountName = accountName
                    getResultsFromApi()
                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode == Activity.RESULT_OK) {
                getResultsFromApi()
            }
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     * requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this)
    }


    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     * Google Play Services on this device.
     */
    private fun showGooglePlayServicesAvailabilityErrorDialog(
            connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
                Activity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES)
        dialog.show()
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }
}