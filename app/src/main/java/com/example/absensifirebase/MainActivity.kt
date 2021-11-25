package com.example.absensifirebase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.absensifirebase.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.layout_dialog_form.view.*
import java.lang.Math.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    companion object{
        const val ID_LOCATION_PERMISSION = 0
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissionLocaton()
        //3 buat method, untuk menghindari penggunaaan code ygbanyak di activity
        onClick()
    }

    private fun checkPermissionLocaton() {
        if (checkPermission()){
            if (!isLocationEnabled()){
                Toast.makeText(this,"Please turn on your location", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }else{
            requestPermission()
        }
    }


    //5
    private fun checkPermission() : Boolean{
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION ) ==
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ){
            return true
        }
        return false
    }

    //4
    private fun isLocationEnabled():Boolean{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            return true
        }
        return false
    }

    //10 mengihitung distance menggunakan formula
    //kemudian masukkan codenya di getlastLocation
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6372.8 // in kilometers

        val radiansLat1 = toRadians(lat1)
        val radianslat2 = toRadians(lat2)
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        return 2 * r * asin(sqrt(sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(radiansLat1) * cos(radianslat2)))
    }

    //9 mebuat function addreses, membuat alamat menjadi langitude dan longitude
    private fun getAddresses ():List<Address>{
        val destinationPlace = "SMK IDN Boarding School"
        val geocode = Geocoder(this, Locale.getDefault())
        return geocode.getFromLocationName(destinationPlace, 100)
        //kemudian tambahkan code di get last location
    }

    //8 mendapatkan lastLocation, kemudian kita set function ini di onclick
    private fun getLastLocation(){
        if (checkPermission()){
            if (isLocationEnabled()){
                LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {location->
                    val currentLat = location.latitude
                    val currentLong = location.longitude



                    //tambahan code langkah ke 9
//
//                    Log.d("coba", "size : {${getAddresses().size}}")
//                    for (address: Address in getAddresses()){
//                        Log.d("coba", "lat : {${address.latitude}} , long : {${address.longitude}}")
//                    }
                    //di ganti menggunakan code ke 10
                    val distance = calculateDistance(currentLat, currentLong, getAddresses()[0].latitude, getAddresses()[0].longitude) *1000
                    //11 tambahkan input dialog jika berada di dalam jarak
                    if(distance < 50.0){
                        showDialogForm()
                    }else{
                        binding.tvCheckInSuccess.visibility = View.VISIBLE
                        binding.tvCheckInSuccess.text = "Diluar jangkauan"
                    }
                    Log.d("coba", "lat : {${currentLat}} , long : {${currentLong}}")



                    stopScanLocation()
                }
            }else{
                Toast.makeText(this,"Please turn on your location", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }else{
            requestPermission()
        }
    }

    private fun showDialogForm() {
        val dialogForm = LayoutInflater.from(this).inflate(R.layout.layout_dialog_form, null)
        AlertDialog.Builder(this)
            .setView(dialogForm)
            .setCancelable(false)
            .setPositiveButton("Submit") { dialog, _ ->
                val name = dialogForm.etName.text.toString()
//                Toast.makeText(this, "name : $name", Toast.LENGTH_SHORT).show()
                inputDataToFireBase(name)
                dialog.dismiss()
            }
            .setNegativeButton("cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    //tambahkan function getCurrentDate
    private fun inputDataToFireBase(name: String) {
        val user = User(name, getCurrentDate())

        val dataBase = FirebaseDatabase.getInstance("https://absensifirebase-775a3-default-rtdb.firebaseio.com/")
        val attendanceRef = dataBase.getReference("attendence")

        attendanceRef.child(name).setValue(user)
            .addOnSuccessListener {
                binding.tvCheckInSuccess.visibility = View.VISIBLE
                binding.tvCheckInSuccess.text = "Check-in Succes"
            }
            .addOnFailureListener {
                Toast.makeText(this,"${it.message}", Toast.LENGTH_SHORT).show()

            }
    }
    private fun getCurrentDate(): String{
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }

    // 7fun ini aktif bila dia meminta ijinkan sebuah aplikasi ini untuk mendapatkan lokasi
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ID_LOCATION_PERMISSION){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Berhasil di ijinkan", Toast.LENGTH_SHORT).show()

                if (!isLocationEnabled()){
                    Toast.makeText(this,"Please turn on your location", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }else{
                Toast.makeText(this,"Gagal di ijinkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //6 ketika aplikasi di munculkan pertama kali dia tidak langsung aktif jadi harus meminta izin terlebih dahulu ke pengguna
    //jgn lupa tambahkan companion object
    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            ID_LOCATION_PERMISSION
        )
    }

    private fun onClick() {
        binding.fabCheckIn.setOnClickListener {
            loadScanLocation()
            Handler().postDelayed({

                getLastLocation()
            }, 4000)
        }
    }

    //2 membuat fun
    private fun loadScanLocation() {
        binding.rippleBackground.startRippleAnimation()
        binding.tvScanning.visibility = View.VISIBLE
        binding.tvCheckInSuccess.visibility = View.GONE
    }

    //1 mebuat func
    private fun stopScanLocation() {
        binding.apply {
            rippleBackground.stopRippleAnimation()
            tvScanning.visibility = View.GONE

        }
    }
}