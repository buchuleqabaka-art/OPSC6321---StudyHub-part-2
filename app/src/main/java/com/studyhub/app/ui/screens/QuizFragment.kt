package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyhub.app.R
import com.studyhub.app.quiz.QuizState
import com.studyhub.app.quiz.QuizViewModel

class QuizFragment : HubFragment(R.layout.fragment_quiz) {
    private val model: QuizViewModel by viewModels()
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::select) }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe(model.state) { render(view.findViewById(R.id.quiz_content), it) }
    }
    private fun render(box: LinearLayout, state: QuizState) {
        box.removeAllViews()
        fun label(value: String, size: Float = 16f) {
            box.addView(TextView(requireContext()).apply {
                text = value; textSize = size; setTextColor(requireContext().getColor(R.color.ink)); setPadding(0, 12, 0, 12)
            })
        }
        fun button(title: String, action: () -> Unit) {
            box.addView(MaterialButton(requireContext()).apply { text = title; isEnabled = !state.busy; setOnClickListener { action() } })
        }
        label(getString(R.string.quiz_title), 26f)
        label(getString(R.string.quiz_disclaimer))
        if (state.busy) { box.addView(ProgressBar(requireContext())); label(getString(R.string.quiz_working)) }
        state.error?.let { label(getString(it)) }
        val quiz = state.active
        if (quiz != null) {
            button(getString(R.string.quiz_library)) { model.closeQuiz() }
            label(quiz.title, 22f)
            if (!quiz.complete) {
                val q = quiz.questions[quiz.answers.size]
                label(getString(R.string.quiz_progress, quiz.answers.size + 1, quiz.questions.size))
                label(q.prompt, 20f)
                val options = RadioGroup(requireContext())
                q.options.forEachIndexed { index, option -> options.addView(RadioButton(requireContext()).apply {
                    id = View.generateViewId(); tag = index; text = option; textSize = 16f; isEnabled = !state.busy
                    setPadding(0, 12, 0, 12)
                }) }
                box.addView(options)
                val submit = MaterialButton(requireContext()).apply {
                    text = getString(R.string.quiz_submit); isEnabled = false
                    setOnClickListener { options.findViewById<RadioButton>(options.checkedRadioButtonId)?.let { model.answer(it.tag as Int) } }
                }
                options.setOnCheckedChangeListener { _, id -> submit.isEnabled = id != -1 && !state.busy }
                box.addView(submit)
            } else {
                label(getString(R.string.quiz_score, quiz.score, quiz.questions.size), 24f)
                if (quiz.score < quiz.questions.size) button(getString(R.string.quiz_retry)) { model.retry() }
                quiz.questions.forEachIndexed { i, q ->
                    label("${i + 1}. ${q.prompt}", 20f)
                    label(getString(R.string.quiz_your_answer, q.options[quiz.answers[i]]))
                    label(getString(R.string.quiz_correct_answer, q.options[q.answer]))
                    label(q.explanation)
                    label(getString(R.string.quiz_source, q.source))
                }
            }
            return
        }
        label(getString(R.string.quiz_intro))
        button(getString(if (state.selected) R.string.quiz_replace_pdf else R.string.quiz_choose_pdf)) { picker.launch(arrayOf("application/pdf")) }
        if (state.selected) label(getString(R.string.quiz_pdf_ready))
        label(getString(R.string.quiz_count))
        val spinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("5", "10"))
            setSelection(if (model.count == 10) 1 else 0); isEnabled = !state.busy
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { model.count = if (position == 1) 10 else 5 }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        box.addView(spinner)
        val generate = MaterialButton(requireContext()).apply {
            text = getString(R.string.quiz_generate); isEnabled = state.selected && model.consent && !state.busy
            setOnClickListener { model.generate(resources.configuration.locales[0].language) }
        }
        box.addView(CheckBox(requireContext()).apply {
            text = getString(R.string.quiz_consent); isChecked = model.consent; isEnabled = !state.busy
            setOnCheckedChangeListener { _, checked -> model.consent = checked; generate.isEnabled = checked && state.selected && !state.busy }
        })
        box.addView(generate)
        label(getString(R.string.quiz_saved), 22f)
        if (state.saved.isEmpty()) label(getString(R.string.quiz_empty))
        state.saved.forEach { saved ->
            button(saved.title + " — " + getString(R.string.quiz_saved_progress, saved.answers.size, saved.questions.size)) { model.open(saved) }
            button(getString(R.string.quiz_delete)) {
                MaterialAlertDialogBuilder(requireContext()).setMessage(getString(R.string.quiz_delete_confirm, saved.title))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.quiz_delete) { _, _ -> model.delete(saved.id) }.show()
            }
        }
    }
}
