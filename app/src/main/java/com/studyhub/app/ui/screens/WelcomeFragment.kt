package com.studyhub.app.ui.screens

import android.os.Bundle
import android.view.View
import com.studyhub.app.R
import com.studyhub.app.databinding.FragmentWelcomeBinding
import com.studyhub.app.ui.navigation.Routes

class WelcomeFragment : HubFragment(R.layout.fragment_welcome) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWelcomeBinding.bind(view)
        binding.buttonGetStarted.setOnClickListener { navigator.navigate(Routes.REGISTER) }
        binding.buttonGoLogin.setOnClickListener { navigator.navigate(Routes.LOGIN) }
    }
}
