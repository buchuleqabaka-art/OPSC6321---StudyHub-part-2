package com.studyhub.app.quiz

data class QuizQuestion(val prompt: String, val options: List<String>, val answer: Int, val explanation: String, val source: String)
data class PaperQuiz(val id: String, val title: String, val questions: List<QuizQuestion>, val answers: List<Int> = emptyList()) {
    val complete get() = answers.size == questions.size
    val score get() = answers.indices.count { answers[it] == questions[it].answer }
    fun checked(): PaperQuiz {
        require(id.matches(Regex("[a-zA-Z0-9-]{1,80}")))
        require(title.isNotBlank() && title.length <= 160 && questions.size in 1..10)
        require(answers.size <= questions.size && answers.all { it in 0..3 })
        questions.forEach {
            require(it.prompt.isNotBlank() && it.prompt.length <= 1000 && it.options.size == 4)
            require(it.options.all { option -> option.isNotBlank() && option.length <= 500 })
            require(it.options.map { option -> option.trim().lowercase() }.distinct().size == 4)
            require(it.answer in 0..3 && it.explanation.isNotBlank() && it.explanation.length <= 1500)
            require(it.source.isNotBlank() && it.source.length <= 200)
        }
        return this
    }
    fun retryMistakes(newId: String) = copy(id = newId,
        questions = questions.filterIndexed { index, q -> answers.getOrNull(index) != q.answer }, answers = emptyList())
}
