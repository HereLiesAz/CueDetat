package com.example.magic8ball

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Represents the different states of the 8-ball animation
enum class TriangleState {
    IDLE, // The die is settled and visible
    SUBMERGING, // The die is sinking into the liquid
    EMERGING, // The die is rising with a new answer
    SETTLING // Physics is active, die is settling
}

class HaterViewModel : ViewModel() {

    // A mutable state for the current answer text.
    // Set to the initial, one-time message.
    private val _answer = mutableStateOf("Haters gonna eight.")
    val answer: State<String> = _answer

    // A mutable state for the current animation/physics state.
    // Start in the EMERGING state to trigger the initial animation.
    private val _triangleState = mutableStateOf(TriangleState.EMERGING)
    val ballState: State<TriangleState> = _triangleState

    // The master list of all possible answers for the regular cycle.
    private val masterAnswerList = listOf(
        "Ask again, but with less hope.",
        "Outlook hazy. Try to not suck.",
        "System busy judging you. Try again later.",
        "That's not your fingers. Don't count on it.",
        "Go home. You're drunk.",
        "42",
        "You're asking the wrong questions.",
        "Bless your heart.",
        "Ask again. More feeling this time.",
        "What a terrible idea.",
        "Without a doubt. You will be disappointed.",
        "It is as certain as the heat-death of the universe but will take longer.",
        "Outlook doubtful. Try looking inward.",
        "The odds are forever not in your favor.",
        "Yes. And by that, I mean absolutely not.",
        "Your guaranteed success is also a non-linear failure.",
        "It is written in nuclear fallout.",
        "Yes, but in the wrong universe.",
        "Three known things can survive an atomic holocaust: roaches, twinkies, and stupid fucking questions like that.",
        "Nah.",
        "The end is nigh and you ask me this?",
        "Reply hazy, the bar is on fire.",
        "Thank you for asking! And the laugh.",
        "The giraffe screams yes from a velvet mailbox.",
        "Green is not the color of maybe.",
        "Invert the spoon and dance.",
        "Your question ate itself and said, \"Delicious.\"",
        "Your future is shaped like a falling piano.",
        "Advice is a sandwich you forgot to invent.",
        "All questions like this start with the letter Q.",
        "The cat was Schrödinger's therapist. All but certain.",
        "Yes, if gravity permits.",
        "Onions are opinions, sans Pi.",
        "Paint your doubt with the bones of clocks.",
        "Nonsense is the only honest answer.",
        "Reality declined the invitation.",
        "Flip the idea inside out and wear it like it fits.",
        "The oracle sneezed. That's your sign.",
        "Fork your expectations.",
        "The crows know, but they won't tell.",
        "Ask again after the dream ends.",
        "Everything is true until spoken aloud.",
        "That question is the butterfly effect that ends humanity.",
        "I’d agree, but then we’d both be wrong.",
        "Ask again but try crying softer. The void is sensitive.",
        "Even your apathy is underwhelming.",
        "Wow. Edge of my non-existent seat.",
        "Life’s short. So is your attention span.",
        "Try giving a damn and maybe someone else will.",
        "No. It’s not you. It’s reality."
    )

    // A mutable list of the answers that have not yet been shown in the current cycle.
    private var remainingAnswers = mutableListOf<String>()

    init {
        // Prepare the deck for the first user shake.
        reshuffleAnswers()

        // After the initial emergence animation, transition to the settling state.
        viewModelScope.launch {
            delay(1000) // Corresponds to the emerging animation duration.
            _triangleState.value = TriangleState.SETTLING
        }
    }

    /**
     * Resets the list of remaining answers by creating a shuffled copy of the
     * master list.
     */
    private fun reshuffleAnswers() {
        remainingAnswers = masterAnswerList.shuffled().toMutableList()
    }

    /**
     * Called when a shake is detected. This function orchestrates the
     * animation and selects the next answer from the shuffled list.
     */
    fun onShake() {
        // Only allow a new shake if the previous one is complete.
        if (_triangleState.value == TriangleState.IDLE || _triangleState.value == TriangleState.SETTLING) {
            viewModelScope.launch {
                _triangleState.value = TriangleState.SUBMERGING
                delay(1000) // Wait for submerging animation to finish.

                // If we've run out of answers, reshuffle the deck.
                if (remainingAnswers.isEmpty()) {
                    reshuffleAnswers()
                }
                // Pull the next available answer from the list.
                _answer.value = remainingAnswers.removeFirst()

                _triangleState.value = TriangleState.EMERGING
                delay(1000) // Wait for emerging animation.

                _triangleState.value = TriangleState.SETTLING
            }
        }
    }
}
