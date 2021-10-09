package org.walletconnect.example

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.walletconnect.example.databinding.ActivityMainBinding
import org.walletconnect.example.extension.getCurrentDestination
import org.walletconnect.example.extension.getNavController
import org.walletconnect.example.wallet.WalletViewModel
import org.walletconnect.example.wallet.ui.SessionProposalDialog
import org.walletconnect.example.wallet.ui.ShowSessionProposalDialog
import org.walletconnect.example.wallet.ui.ToggleBottomNav

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WalletViewModel by viewModels()
    private var sessionProposalDialog: SessionProposalDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    R.color.blue
                )
            )
        )

        viewModel.eventFlow.observe(this, { event ->
            when (event) {
                is ToggleBottomNav -> {
                    if (event.shouldShown) {
                        binding.bottomNav.visibility = View.VISIBLE
                    } else {
                        binding.bottomNav.visibility = View.GONE
                    }
                }
                is ShowSessionProposalDialog -> {
                    sessionProposalDialog = SessionProposalDialog(this, {
                        viewModel.approve()
                    }, {
                        viewModel.reject()
                    }).apply {
                        setContent(event.proposal)
                        show()
                    }
                }
            }
        })
        setBottomNavigation()
    }

    private fun setBottomNavigation() {
        binding.bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.wallet -> if (getNavController().currentDestination?.id != R.id.walletFragment)
                    getNavController().navigate(R.id.action_dappFragment_to_walletFragment)
                R.id.dapp -> if (getNavController().currentDestination?.id != R.id.dappFragment)
                    getNavController().navigate(R.id.action_walletFragment_to_dappFragment)
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.qrCodeScanner) {
            getNavController().navigate(R.id.action_walletFragment_to_scannerFragment)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when (getCurrentDestination()?.id) {
            R.id.scannerFragment -> {
                menu?.findItem(R.id.qrCodeScanner)?.isVisible = false
                menu?.findItem(R.id.connect)?.isVisible = false
            }
            R.id.walletFragment -> {
                menu?.findItem(R.id.connect)?.isVisible = false
                menu?.findItem(R.id.qrCodeScanner)?.isVisible = true
            }
            R.id.dappFragment -> {
                menu?.findItem(R.id.qrCodeScanner)?.isVisible = false
                menu?.findItem(R.id.connect)?.isVisible = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }
} 