package com.bpitindia.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ResetSuccessfulFragment : Fragment() {
    private var fromFragment: String? = null
    private lateinit var logInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fromFragment = it.getString("fromFragment")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reset_successful, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logInButton = view.findViewById(R.id.login_button)
        logInButton.setOnClickListener {
            if (fromFragment == "reset")
                findNavController().navigate(R.id.action_resetSuccessfulFragment_to_loginFragment)
            else if (fromFragment == "change")
                findNavController().navigate(R.id.action_resetSuccessfulFragment_to_subjectListFragment)
        }

    }
}