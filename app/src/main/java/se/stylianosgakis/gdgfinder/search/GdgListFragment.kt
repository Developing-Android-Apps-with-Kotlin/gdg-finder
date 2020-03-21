package se.stylianosgakis.gdgfinder.search

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import se.stylianosgakis.gdgfinder.R
import se.stylianosgakis.gdgfinder.databinding.FragmentGdgListBinding

private const val LOCATION_PERMISSION_REQUEST = 1
private const val LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION"

class GdgListFragment : Fragment() {
    private val viewModel: GdgListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val adapter = GdgListAdapter(GdgClickListener { chapter ->
            val destination = Uri.parse(chapter.website)
            startActivity(Intent(Intent.ACTION_VIEW, destination))
        })
        val binding = FragmentGdgListBinding.inflate(inflater)
        binding.apply {
            // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
            lifecycleOwner = viewLifecycleOwner
            // Giving the binding access to the OverviewViewModel
            viewModel = this@GdgListFragment.viewModel
            // Sets the adapter of the RecyclerView
            gdgChapterList.adapter = adapter
        }

        viewModel.showNeedLocation.observe(viewLifecycleOwner, Observer { show ->
            if (show == true) {
                Snackbar.make(
                    binding.root,
                    "No location. Enable location in settings (hint: test with Maps) then check app permissions!",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })

        viewModel.regionList.observe(viewLifecycleOwner, Observer { regionList ->
            // Map the regions to a list of Chips
            regionList?.let {
                // 1: Make a new Chip view for each item in the list
                val chipGroup = binding.chipGroup
                val chipGroupInflater = LayoutInflater.from(chipGroup.context)
                // Map the list of Regions into a list if Chip
                val chipList = it.map { regionName ->
                    val chip = chipGroupInflater
                        .inflate(R.layout.region_chip, chipGroup, false) as Chip
                    chip.text = regionName
                    chip.tag = regionName
                    chip.setOnCheckedChangeListener { button, isChecked ->
                        viewModel.onFilterChanged(button.tag as String, isChecked)
                    }
                    chip
                }
                // 2: Remove any views already in the ChipGroup
                chipGroup.removeAllViews()
                // 3: Add the new children to the ChipGroup
                chipList.forEach { chip ->
                    chipGroup.addView(chip)
                }
            }
        })

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLastLocationOrStartLocationUpdates()
    }

    /**
     * Show the user a dialog asking for permission to use location.
     */
    private fun requestLocationPermission() {
        requestPermissions(arrayOf(LOCATION_PERMISSION), LOCATION_PERMISSION_REQUEST)
    }

    /**
     * Request the last location of this device, if known, otherwise start location updates.
     *
     * The last location is cached from the last application to request location.
     */
    private fun requestLastLocationOrStartLocationUpdates() {
        // if we don't have permission ask for it and wait until the user grants it
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                LOCATION_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                startLocationUpdates(fusedLocationClient)
            } else {
                viewModel.onLocationUpdated(location)
            }
        }
    }

    /**
     * Start location updates, this will ask the operating system to figure out the devices location.
     */
    private fun startLocationUpdates(fusedLocationClient: FusedLocationProviderClient) {
        // if we don't have permission ask for it and wait until the user grants it
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                LOCATION_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        val request = LocationRequest().setPriority(LocationRequest.PRIORITY_LOW_POWER)
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                val location = locationResult?.lastLocation ?: return
                viewModel.onLocationUpdated(location)
            }
        }
        fusedLocationClient.requestLocationUpdates(request, callback, null)
    }

    /**
     * This will be called by Android when the user responds to the permission request.
     *
     * If granted, continue with the operation that the user gave us permission to do.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLastLocationOrStartLocationUpdates()
                }
            }
        }
    }
}


