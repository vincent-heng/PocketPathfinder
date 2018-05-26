package fr.project24.fox.pocketpathfinder.model

import com.google.gson.annotations.SerializedName

data class Room (
        // Quick View
        @SerializedName("room_code") val roomCode: String,
        @SerializedName("character_sheet_id") val characterSheetId: String
)
