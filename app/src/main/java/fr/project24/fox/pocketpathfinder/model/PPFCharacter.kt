package fr.project24.fox.pocketpathfinder.model

data class PPFCharacter (
        // Quick View
        val playerName: String,
        val name: String,
        val level: Int,
        val race: String,
        val ppfClass: String,
        val experience: Int,
        val currentHp: Int,
        val maxHp: Int,

        // Stats
        val strength: Int,
        val dexterity: Int,
        val constitution: Int,
        val intelligence: Int,
        val wisdom: Int,
        val charisma: Int
)
