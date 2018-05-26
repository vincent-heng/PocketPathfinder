package fr.project24.fox.pocketpathfinder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.LoaderManager.LoaderCallbacks
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.AutoCompleteTextView
import fr.project24.fox.pocketpathfinder.model.Room
import fr.project24.fox.pocketpathfinder.retrofit.ApiRepositoryProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * "Join a room" screen.
 */
class LoginActivity : AppCompatActivity(), LoaderCallbacks<Cursor>, AnkoLogger {

    // UI references.
    private var mRoomNameView: AutoCompleteTextView? = null
    private var mProgressView: View? = null
    private var mLoginFormView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Set up the login form.
        mRoomNameView = room_name as AutoCompleteTextView

        join_room_button.setOnClickListener { attemptJoin() }

        mLoginFormView = login_form
        mProgressView = login_progress
    }

    /**
     * Attempts to join the room specified by the form.
     * If there are form errors (missing fields, etc.), the
     * errors are presented and no actual request attempt is made.
     */
    private fun attemptJoin() {
        // Reset errors.
        mRoomNameView!!.error = null

        val roomCode = mRoomNameView!!.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid room name.
        if (TextUtils.isEmpty(roomCode)) {
            mRoomNameView!!.error = getString(R.string.error_field_required)
            focusView = mRoomNameView
            cancel = true
        } else if (!isRoomNameValid(roomCode)) {
            mRoomNameView!!.error = getString(R.string.error_invalid_room_name)
            focusView = mRoomNameView
            cancel = true
        }

        if (cancel) {
            // There was an error; focus the first form field with an error.
            focusView!!.requestFocus()
        } else {
            showProgress(true)
            info("Attempting to join $roomCode")

            requestJoinRoom(roomCode, { roomList ->
                showProgress(false)
                if (roomList.isEmpty()) {
                    info(getString(R.string.room_not_found))
                    toast(getString(R.string.room_not_found))
                } else {
                    val room = roomList[0]
                    info("Room found: $room")
                    toast(getString(R.string.room_success))
                    getSharedPreferences("Preferences", Context.MODE_PRIVATE).edit().putString("roomName", room.roomCode).apply()
                    getSharedPreferences("Preferences", Context.MODE_PRIVATE).edit().putString("characterSheetId", room.characterSheetId).apply()
                }
            }, { error ->
                showProgress(false)
                info(error.message)
                toast(getString(R.string.room_connection_failed))
            })
        }
    }

    private fun requestJoinRoom(roomCode: String, callback: (List<Room>) -> Unit, errorCallback: (Throwable) -> Unit) {
        val roomObservable = ApiRepositoryProvider.provideRepository().findRoom(roomCode)

        roomObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback, errorCallback)
    }

    private fun isRoomNameValid(roomName: String): Boolean {
        return roomName.isNotEmpty()
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

        mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
        mLoginFormView!!.animate().setDuration(shortAnimTime.toLong()).alpha(
                (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mLoginFormView!!.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
        mProgressView!!.animate().setDuration(shortAnimTime.toLong()).alpha(
                (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mProgressView!!.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {

    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    private interface ProfileQuery {
        companion object {
            val PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        }
    }
}