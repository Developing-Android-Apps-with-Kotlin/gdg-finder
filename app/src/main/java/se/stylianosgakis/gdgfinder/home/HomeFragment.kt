package se.stylianosgakis.gdgfinder.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import se.stylianosgakis.gdgfinder.databinding.HomeFragmentBinding

class HomeFragment : Fragment() {
    companion object {
        fun newInstance() = HomeFragment()
    }

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = HomeFragmentBinding.inflate(inflater)
        binding.apply {
            viewModel = this@HomeFragment.viewModel
        }

        viewModel.navigateToSearch.observe(viewLifecycleOwner, Observer { doNavigate ->
            if (doNavigate) {
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToGdgListFragment()
                )
                viewModel.onNavigatedToSearch()
            }
        })

        return binding.root
    }
}
