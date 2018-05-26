package fr.project24.fox.pocketpathfinder

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

class MainActivity : AppCompatActivity(), AnkoLogger {
    init {
        instance = this
    }

    companion object {
        private var instance: MainActivity? = null // FIXME This is a memory leak

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prepareActivity()
    }

    override fun onResume() {
        super.onResume()
        prepareActivity()
    }

    private fun prepareActivity() {
        val isInARoom = loadRoomName()
        bindButtons(isInARoom)
    }

    private fun bindButtons(isInARoom: Boolean) {
        /* Public buttons */
        login_button.onClick {
            startActivity<LoginActivity>()
        }

        doc_button.onClick {
            val pages = listOf("Page principale", "Création personnage", "Races", "Classes")
            val pagesURL = listOf(
                    "http://www.pathfinder-fr.org/Wiki/Pathfinder-RPG.MainPage.ashx",
                    "http://www.pathfinder-fr.org/Wiki/Pathfinder-RPG.Cr%c3%a9er%20un%20personnage.ashx",
                    "http://www.pathfinder-fr.org/Wiki/Pathfinder-RPG.races.ashx",
                    "http://www.pathfinder-fr.org/Wiki/Pathfinder-RPG.classes.ashx")
            selector("Quelle page ?", pages, { _, i ->
                info("Browsing ${pages[i]}")
                browse(pagesURL[i])
            })
        }

        tools_button.onClick {
            toast("Not implemented yet!")
        }

        if (!isInARoom) {
            return
        }

        /* In-Room buttons */
        story_button.visibility = View.VISIBLE
        story_button.onClick {
            toast("Not implemented yet!")
        }

        characters_button.visibility = View.VISIBLE
        characters_button.onClick {
            startActivity<CharactersActivity>()
        }
    }

    private fun loadRoomName(): Boolean {
        val roomName = getSharedPreferences("Preferences", Context.MODE_PRIVATE).getString("roomName", "")
        if (roomName.isEmpty()) {
            room_name_display.text = getString(R.string.default_room_name)
            return false
        }
        room_name_display.text = getString(R.string.room_indicator) + roomName
        return true
    }
}
