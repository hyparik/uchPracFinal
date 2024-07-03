package com.bignerdranch.android.z2.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import com.bignerdranch.android.z2.ui.database.MyData
import com.bignerdranch.android.z2.ui.database.MyDataBase
import com.example.android.z2.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var ivMyImage: ImageView
    private lateinit var imageUrl: Uri
    private lateinit var db: MyDataBase
    private lateinit var fName: EditText
    private lateinit var sName: EditText
    private lateinit var group: EditText
    private lateinit var saveButton: Button

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "error", Toast.LENGTH_SHORT).show()
        }
    }

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            ivMyImage.setImageURI(imageUrl)
        } else {
            Toast.makeText(requireContext(), "error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        imageUrl = createImageUri()
        ivMyImage = binding.imageView
        fName = binding.Name
        sName = binding.Surname
        group = binding.Group
        saveButton = binding.buttonSave

        ivMyImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        db = MyDataBase.getInstance(requireContext())
        getDataFromDatabase()
        saveButton.setOnClickListener {
            saveDataToDatabase()
        }
        return binding.root
    }

    private fun launchCamera() {
        contract.launch(imageUrl)
    }

    private fun getDataFromDatabase() {
        db.getDbDao().query().asLiveData().observe(viewLifecycleOwner) { dataList ->
            if (dataList.isNotEmpty()) {
                val data = dataList[0]
                Log.d("HomeFragment", "successful: $data")
                updateUI(data)
            } else {
                Log.d("HomeFragment", "error")
            }
        }
    }


    private fun updateUI(data: MyData) {
        fName.setText(data.name)
        sName.setText(data.surname)
        group.setText(data.group)
        val bitmap = data.image?.let { BitmapFactory.decodeByteArray(data.image, 0, it.size) }
        ivMyImage.setImageBitmap(bitmap)
    }

    private fun saveDataToDatabase() {
        val fName = fName.text.toString()
        val sName = sName.text.toString()
        val group = group.text.toString()
        val imageBytes = convertImageToBytes(ivMyImage)
        if (fName.isEmpty() || sName.isEmpty() || group.isEmpty() || imageBytes == null) {
            Log.e("HomeFragment", "error")
            return
        }
        val data = MyData(
            PrimaryKey = 1,
            image = imageBytes,
            name = fName,
            surname = sName,
            group = group
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    db.getDbDao().insert(data)
                    Log.d("HomeFragment", "successful: $data")
                } catch (e: Exception) {
                    Log.e("HomeFragment", "error", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createImageUri(): Uri {
        val image = File(requireActivity().filesDir, "myPhoto.png")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().applicationInfo.packageName}.FileProvider",
            image
        )
    }

    private fun convertImageToBytes(imageView: ImageView): ByteArray? {
        try {
            val drawable = imageView.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()))

                val compressedStream = ByteArrayOutputStream()
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, compressedStream)
                return compressedStream.toByteArray()
            }
        } catch (e: OutOfMemoryError) {
            Log.e("HomeFragment", "error", e)
        } catch (e: Exception) {
            Log.e("HomeFragment", "error", e)
        }
        return null
    }


}
