package ca.gbc.lab5gpslocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnGetLocation: Button

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchGpsLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(this, "Fine GPS permission required for accurate coordinates", Toast.LENGTH_SHORT).show()
                fetchGpsLocation()
            }
            else -> {
                tvStatus.text = "Permission denied. Enable location in device settings."
                Toast.makeText(this, "Location permission was denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvStatus = findViewById(R.id.tvStatus)
        btnGetLocation = findViewById(R.id.btnGetLocation)

        btnGetLocation.setOnClickListener {
            checkPermissionsAndGetLocation()
        }
    }

    private fun checkPermissionsAndGetLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchGpsLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun fetchGpsLocation() {
        if (!isGpsEnabled()) {
            tvStatus.text = "GPS is turned off. Please enable Location Services."
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        tvStatus.text = "Acquiring live location fix from GPS..."

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                tvLatitude.text = String.format(Locale.getDefault(), "%.6f°", location.latitude)
                tvLongitude.text = String.format(Locale.getDefault(), "%.6f°", location.longitude)
                val providerName = location.provider ?: "GPS"
                tvStatus.text = "Location acquired successfully via $providerName"
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        tvLatitude.text = String.format(Locale.getDefault(), "%.6f°", lastLoc.latitude)
                        tvLongitude.text = String.format(Locale.getDefault(), "%.6f°", lastLoc.longitude)
                        tvStatus.text = "Location acquired (cached fix)"
                    } else {
                        tvStatus.text = "Unable to fetch location fix. Try moving outdoors or setting mock location."
                    }
                }
            }
        }.addOnFailureListener { exception ->
            tvStatus.text = "Error acquiring location: ${exception.localizedMessage}"
        }
    }
}
