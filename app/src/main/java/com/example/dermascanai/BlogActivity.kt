    package com.example.dermascanai

    import android.app.AlertDialog
    import android.content.Intent
    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import android.os.Bundle
    import android.util.Base64
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.PopupWindow
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.GravityCompat
    import androidx.recyclerview.widget.LinearLayoutManager
    import com.example.dermascanai.databinding.ActivityBlogBinding
    import com.example.dermascanai.databinding.DialogAddBlogBinding
    import com.example.dermascanai.databinding.LayoutNotificationPopupBinding
    import com.google.android.material.bottomsheet.BottomSheetDialog
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.*
    import java.io.ByteArrayOutputStream

    class BlogActivity : AppCompatActivity() {

        private lateinit var binding: ActivityBlogBinding
        private lateinit var blogAdapter: BlogAdapter
        private val blogList = mutableListOf<BlogPost>()

        private lateinit var notificationBinding: LayoutNotificationPopupBinding // Use ViewBinding here
        private lateinit var notificationAdapter: NotificationAdapter
        private val notificationList = mutableListOf<Notification>()

        private val auth = FirebaseAuth.getInstance()
        private val userRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("userInfo")
        private val blogRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("blogPosts")

        private val notificationRef = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("notifications")


        private var currentFullName: String = ""
        private var currentImageBase64: String = ""

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityBlogBinding.inflate(layoutInflater)
            setContentView(binding.root)

            loadCurrentUserInfo()
            setupRecyclerView()
            fetchBlogPosts()

            val drawerLayout = binding.drawerLayout
            val navView = binding.navigationView

            val headerView = navView.getHeaderView(0)
            val closeDrawerBtn = headerView.findViewById<ImageView>(R.id.closeDrawerBtn)

            notificationBinding = LayoutNotificationPopupBinding.inflate(layoutInflater)
            val popupWindow = PopupWindow(
                notificationBinding.root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            val currentUserId = auth.currentUser?.uid
            val notifications = mutableListOf<Notification>()


            val notifRecyclerView = notificationBinding.notificationRecyclerView
            notifRecyclerView.layoutManager = LinearLayoutManager(this)


            notificationAdapter = NotificationAdapter(this, notificationList)
            notifRecyclerView.adapter = notificationAdapter

            val userNotificationsRef = notificationRef.child(currentUserId!!)
            userNotificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notificationList.clear()
                    var hasUnread = false
                    for (notifSnapshot in snapshot.children) {
                        val notif = notifSnapshot.getValue(Notification::class.java)
                        notif?.let {
                            notificationList.add(it)
                            if (!it.isRead) {
                                hasUnread = true
                            }
                        }
                    }
                    notificationList.sortByDescending { it.timestamp }

                    notificationAdapter.notifyDataSetChanged()

                    binding.notificationDot.visibility = if (hasUnread) View.VISIBLE else View.GONE

                    if (hasUnread) {
                        binding.notificationDot.visibility = View.VISIBLE
                    } else {
                        binding.notificationDot.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BlogActivity, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            })


            binding.menuIcon.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.END)
            }

            closeDrawerBtn.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END)
            }


            binding.notif.setOnClickListener {
                popupWindow.showAsDropDown(binding.notif, -100, 20)
                binding.notificationDot.visibility = View.GONE

                userNotificationsRef.get().addOnSuccessListener { snapshot ->
                    for (notifSnapshot in snapshot.children) {
                        notifSnapshot.ref.child("isRead").setValue(true)
                    }
                }
            }


            // When user clicks the 'Write Something...' area
            binding.post.setOnClickListener {
                showBottomSheetDialog()
            }

            binding.derma.setOnClickListener {
                val intent = Intent(this, UserPage::class.java)
                startActivity(intent)

            }

            navView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.settings -> {
                        Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show()
                    }
                    R.id.nav_terms -> {
                        val intent = Intent(this, TermsConditions::class.java)
                        startActivity(intent)
                    }
                    R.id.privacy -> {
                        val intent = Intent(this, PrivacyPolicy::class.java)
                        startActivity(intent)
                    }
                    R.id.nav_logout -> {
                        logoutUser()
                    }
                }
                drawerLayout.closeDrawers()
                true
            }

        }

        private fun showBottomSheetDialog() {
            val dialog = BottomSheetDialog(this)
            val bottomSheetBinding = DialogAddBlogBinding.inflate(layoutInflater)
            dialog.setContentView(bottomSheetBinding.root)



            // Choose Image
            bottomSheetBinding.btnChooseImage.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, IMAGE_PICK_CODE)
                tempImageBinding = bottomSheetBinding
            }

            // Post Blog
            bottomSheetBinding.btnPostBlog.setOnClickListener {
                val content = bottomSheetBinding.etBlogContent.text.toString().trim()

                if (content.isEmpty()) {
                    Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val postId = blogRef.push().key!!
                val timestamp = System.currentTimeMillis()

                val blogPost = BlogPost(
                    postId = postId,
                    userId = auth.currentUser?.uid ?: "",
                    fullName = currentFullName,
                    profilePicBase64 = currentImageBase64,
                    content = content,
                    postImageBase64 = selectedImageBase64,
                    timestamp = timestamp,
                    likeCount = 0,
                    commentCount = 0
                )

                blogRef.child(postId).setValue(blogPost)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Blog posted!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show()
                    }
            }

            dialog.show()
        }



        private fun loadCurrentUserInfo() {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                userRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(UserInfo::class.java)
                        if (user != null) {
                            currentFullName = user.name ?: ""
                            currentImageBase64 = user.profileImage ?: ""


                            // Ensure the imageBase64 is not empty
                            if (currentImageBase64.isNotEmpty()) {
                                try {
                                    val imageBytes = Base64.decode(currentImageBase64, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    binding.profilePic.setImageBitmap(bitmap) // Set to ImageView
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(this@BlogActivity, "Error loading image", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Set a default image if the Base64 is empty
                                binding.profilePic.setImageResource(R.drawable.ic_profile2)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@BlogActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }


        private fun setupRecyclerView() {

            val currentUserName = currentFullName

            blogAdapter = BlogAdapter(
                this,
                blogList,
                FirebaseAuth.getInstance().currentUser?.uid ?: "",
                currentImageBase64,
                currentUserName
            )

            binding.recyclerViewBlog.apply {
                layoutManager = LinearLayoutManager(this@BlogActivity)
                adapter = blogAdapter
            }
        }



        private fun fetchBlogPosts() {
            blogRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    blogList.clear()
                    for (postSnapshot in snapshot.children) {
                        val blog = postSnapshot.getValue(BlogPost::class.java)
                        blog?.let { blogList.add(it) }
                    }
                    blogList.reverse()
                    blogAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BlogActivity, "Failed to fetch blog posts", Toast.LENGTH_SHORT).show()
                }
            })
        }

        private val IMAGE_PICK_CODE = 1000
        private var tempImageBinding: DialogAddBlogBinding? = null
        private var selectedImageBase64: String? = null

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
                val uri = data.data
                val inputStream = contentResolver.openInputStream(uri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val imageBytes = outputStream.toByteArray()
                val encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                tempImageBinding?.ivBlogImage?.apply {
                    setImageBitmap(bitmap)
                    visibility = View.VISIBLE
                }

                selectedImageBase64 = encodedImage
            }
        }

        private fun sendNotification(toUserId: String, postId: String, type: String, message: String) {
            val notificationId = notificationRef.push().key ?: return
            val fromUserId = auth.currentUser?.uid ?: return
            val timestamp = System.currentTimeMillis()

            val notification = Notification(
                notificationId = notificationId,
                postId = postId,
                fromUserId = fromUserId,
                toUserId = toUserId,
                type = type,
                message = message,
                timestamp = timestamp
            )

            notificationRef.child(notificationId).setValue(notification)
        }

        private fun logoutUser() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to logout?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }



    }
