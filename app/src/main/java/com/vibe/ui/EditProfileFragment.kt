package com.vibe.ui

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.vibe.R
import com.vibe.api.ApiClient
import kotlinx.coroutines.*
import retrofit2.awaitResponse

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.vibe.util.FileUtil
import com.vibe.api.ProfileUpdate
import com.vibe.api.SessionManager

class EditProfileFragment : Fragment() {

    private lateinit var ivCover: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var etName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var tvUsernameStatus: TextView
    private lateinit var rvGallery: RecyclerView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnEditCover: ImageView
    private lateinit var btnEditProfilePic: ImageView

    private val galleryList = mutableListOf<Uri>() // Storing Uris for display
    private val galleryUrls = mutableListOf<String>() // Storing URLs for save
    private lateinit var galleryAdapter: GalleryAdapter
    
    private var profileUrl: String? = null
    private var coverUrl: String? = null
    private var selectedImageType: String = "" // "profile", "cover", "gallery"

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Show immediately
            when (selectedImageType) {
                "profile" -> ivProfile.setImageURI(it)
                "cover" -> ivCover.setImageURI(it)
                "gallery" -> {
                    galleryList.add(it)
                    galleryAdapter.notifyItemInserted(galleryList.size - 1)
                }
            }
            
            // Upload in background
            uploadImage(it) { url ->
                 when (selectedImageType) {
                    "profile" -> profileUrl = url
                    "cover" -> coverUrl = url
                    "gallery" -> galleryUrls.add(url)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)
        
        ivCover = view.findViewById(R.id.ivCover)
        ivProfile = view.findViewById(R.id.ivProfile)
        etName = view.findViewById(R.id.etName)
        etUsername = view.findViewById(R.id.etUsername)
        etBio = view.findViewById(R.id.etBio)
        tvUsernameStatus = view.findViewById(R.id.tvUsernameStatus)
        rvGallery = view.findViewById(R.id.rvGallery)
        btnSave = view.findViewById(R.id.btnSave)
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        btnEditCover = view.findViewById(R.id.btnEditCover)
        btnEditProfilePic = view.findViewById(R.id.btnEditProfilePic)

        setupGallery()
        setupListeners()
        loadCurrentData()

        return view
    }

    private fun setupGallery() {
        galleryAdapter = GalleryAdapter(galleryList, 
            onDelete = { position ->
                galleryList.removeAt(position)
                galleryAdapter.notifyItemRemoved(position)
            },
            onClick = { position ->
                showImageOptions(position)
            }
        )
        rvGallery.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvGallery.adapter = galleryAdapter
        rvGallery.addItemDecoration(ProfileFragment.HorizontalMarginItemDecoration(8))
    }

    private fun showImageOptions(position: Int) {
        val options = arrayOf("Set as Profile Picture", "Set as Cover Image", "Delete")
        AlertDialog.Builder(requireContext())
            .setTitle("Image Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> ivProfile.setImageURI(galleryList[position])
                    1 -> ivCover.setImageURI(galleryList[position])
                    2 -> {
                        galleryList.removeAt(position)
                        galleryAdapter.notifyItemRemoved(position)
                    }
                }
            }
            .show()
    }

    private fun setupListeners() {
        btnEditCover.setOnClickListener {
            selectedImageType = "cover"
            pickImage.launch("image/*")
        }

        btnEditProfilePic.setOnClickListener {
            selectedImageType = "profile"
            pickImage.launch("image/*")
        }

        btnAddPhoto.setOnClickListener {
            selectedImageType = "gallery"
            pickImage.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        etUsername.addTextChangedListener(object : TextWatcher {
            private var searchJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val username = s.toString()
                if (username.isEmpty()) {
                    tvUsernameStatus.visibility = View.GONE
                    return
                }

                tvUsernameStatus.visibility = View.VISIBLE
                tvUsernameStatus.text = "Checking..."
                tvUsernameStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))

                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(500) // Debounce
                    checkUsernameUnique(username)
                }
            }
        })
    }

    private suspend fun checkUsernameUnique(username: String) {
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.api.checkUsername(username).awaitResponse()
            }
            if (response.isSuccessful && response.body()?.available == true) {
                tvUsernameStatus.text = "Username available"
                tvUsernameStatus.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
            } else {
                tvUsernameStatus.text = "Username taken"
                tvUsernameStatus.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
            }
        } catch (e: Exception) {
            tvUsernameStatus.text = "Error checking username"
            tvUsernameStatus.setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
        }
    }

    private fun uploadImage(uri: Uri, onSuccess: (String) -> Unit) {
        val context = context ?: return
        val file = FileUtil.getFileFromUri(context, uri) ?: return
        
        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        Snackbar.make(requireView(), "Uploading...", Snackbar.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.api.uploadImage(body).execute()
                if (response.isSuccessful && response.body() != null) {
                    val url = response.body()!!.url
                    withContext(Dispatchers.Main) {
                        onSuccess(url)
                        Snackbar.make(requireView(), "Uploaded!", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                     withContext(Dispatchers.Main) {
                        Snackbar.make(requireView(), "Upload failed: ${response.code()}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                 withContext(Dispatchers.Main) {
                    Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCurrentData() {
        val userId = SessionManager.getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.api.getProfile(userId).execute()
                if (response.isSuccessful && response.body() != null) {
                    val p = response.body()!!
                    withContext(Dispatchers.Main) {
                        etName.setText(p.name)
                        etBio.setText(p.bio)
                        // etUsername.setText(p.username) // ProfileResponse needs update or separate call
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun saveProfile() {
        val name = etName.text.toString()
        val username = etUsername.text.toString()
        val bio = etBio.text.toString()
        val userId = SessionManager.getUserId()
        
        if (userId == null) {
            Snackbar.make(requireView(), "Please login first", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        btnSave.isEnabled = false
        btnSave.text = "Saving..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.api.updateProfile(
                    ProfileUpdate(
                        userId = userId,
                        name = name,
                        bio = bio,
                        avatar = profileUrl,
                        cover = coverUrl,
                        gallery = galleryUrls,
                        username = username
                    )
                ).execute()
                
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                    if (response.isSuccessful) {
                        Snackbar.make(requireView(), "Profile updated!", Snackbar.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Snackbar.make(requireView(), "Failed to save: ${response.code()}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                    Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    class GalleryAdapter(
        private val images: List<Uri>,
        private val onDelete: (Int) -> Unit,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.ivImage)
            val btnDelete: ImageView = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_edit, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.img.setImageURI(images[position])
            holder.btnDelete.setOnClickListener { onDelete(position) }
            holder.itemView.setOnClickListener { onClick(position) }
        }

        override fun getItemCount() = images.size
    }
}
