package com.vibe.util

import kotlin.random.Random

object UsernameGenerator {
    private val adjectives = listOf(
        "Cool", "Vibe", "Chill", "Hot", "Smooth", "Wild", "Urban", "Neon", "Cosmic", "Mystic",
        "Zen", "Bold", "Epic", "Luxe", "Nova", "Pure", "True", "Real", "Deep", "Soul",
        "Happy", "Lucky", "Funky", "Groovy", "Rad", "Lit", "Dope", "Fresh", "Sleek", "Sharp"
    )

    private val nouns = listOf(
        "Rider", "Soul", "Heart", "Star", "Wolf", "Lion", "Tiger", "Bear", "Hawk", "Eagle",
        "Fox", "Panda", "Ninja", "Samurai", "Viking", "Knight", "Wizard", "King", "Queen", "Prince",
        "Princess", "Angel", "Demon", "Ghost", "Spirit", "Phantom", "Shadow", "Light", "Spark", "Flame",
        "Dreamer", "Lover", "Seeker", "Chaser", "Hunter", "Walker", "Runner", "Surfer", "Skater", "Gamer"
    )

    fun generate(): String {
        val adj = adjectives.random()
        val noun = nouns.random()
        val number = Random.nextInt(10, 999)
        return "$adj$noun$number"
    }
}
