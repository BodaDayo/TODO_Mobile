package com.rgbstudios.todomobile.ui.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.FragmentSupportUsBinding

class SupportUsFragment : Fragment() {

    private lateinit var binding: FragmentSupportUsBinding
    private lateinit var fragmentContext: Context

    private val connectivityChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                loadPage()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSupportUsBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            loadPage()

            swipeRefreshLayout.setOnRefreshListener {
                // Update data from the database when the user performs the pull-to-refresh action
                loadPage()
            }

            // Customize onBackPressed method
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                if (paystackWebView.canGoBack()) {
                    paystackWebView.goBack()
                } else {
                    popBackStackManager()
                }
            }
        }

        // Register the connectivity change receiver
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(connectivityChangeReceiver, filter)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Unregister the connectivity change receiver to prevent memory leaks
        requireContext().unregisterReceiver(connectivityChangeReceiver)
    }

    private fun loadPage() {
        binding.apply {
            // Check for internet connectivity
            if (isNetworkAvailable()) {
                // Internet is available, load the WebView
                paystackWebView.settings.javaScriptEnabled = true
                paystackWebView.loadUrl(getString(R.string.paystack_url))

                // Show WebView and hide the no internet message
                paystackWebView.visibility = View.VISIBLE
                swipeRefreshLayout.visibility = View.GONE
            } else {
                // No internet, show the no internet message
                paystackWebView.visibility = View.GONE
                swipeRefreshLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false
    }

    private fun popBackStackManager() {
        // If no changes, simply pop back stack
        activity?.supportFragmentManager?.popBackStack()
    }
}
