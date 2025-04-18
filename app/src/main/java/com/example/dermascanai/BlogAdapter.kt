package com.example.dermascanai


import android.content.Context
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dermascanai.databinding.ItemBlogPostBinding
import com.google.firebase.database.*
import java.sql.Date
import java.util.Locale

class BlogAdapter(
    private val context: Context,
    private val blogList: List<BlogPost>,
    private val currentUserId: String,
    private val currentUserProfilePic: String
) : RecyclerView.Adapter<BlogAdapter.BlogViewHolder>() {

    private val firebaseInstance = FirebaseDatabase.getInstance("https://dermascanai-2d7a1-default-rtdb.asia-southeast1.firebasedatabase.app/")

    inner class BlogViewHolder(val binding: ItemBlogPostBinding) : RecyclerView.ViewHolder(binding.root)

    private fun updateCommentCountInUI(holder: BlogViewHolder, newCommentCount: Int) {
//        holder.binding.commentCount.text = "$newCommentCount"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogViewHolder {
        val binding = ItemBlogPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlogViewHolder, position: Int) {
        val post = blogList[position]
        val blogRef = firebaseInstance.getReference("blogPosts").child(post.postId)

        holder.binding.fullName.text = post.fullName
        holder.binding.textView8.text = post.content

        val profileBytes = Base64.decode(post.profilePicBase64, Base64.DEFAULT)
        holder.binding.profilePic.setImageBitmap(BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size))

        if (!post.postImageBase64.isNullOrEmpty()) {
            val postBytes = Base64.decode(post.postImageBase64, Base64.DEFAULT)
            holder.binding.imagePost.visibility = View.VISIBLE
            holder.binding.imagePost.setImageBitmap(BitmapFactory.decodeByteArray(postBytes, 0, postBytes.size))
        } else {
            holder.binding.imagePost.visibility = View.GONE
        }

        holder.binding.heartCount.text = post.likeCount.toString()
        holder.binding.commentSection.text = "${post.commentCount} Comment(s)"
        holder.binding.timestamp.text = getTimeAgo(post.timestamp)

        val comments = mutableListOf<Comment>()
        val commentAdapter = AdapterComment(context, comments)
        holder.binding.recyclerViewComment.layoutManager = LinearLayoutManager(context)
        holder.binding.recyclerViewComment.adapter = commentAdapter

        holder.binding.commentSection.setOnClickListener {
            val isVisible = holder.binding.commentLayout.visibility == View.VISIBLE
            holder.binding.commentLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) loadComments(holder, post.postId)
        }

        // Like button UI update
        blogRef.child("likes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userLiked = snapshot.hasChild(currentUserId)
                holder.binding.heart.setImageResource(
                    if (userLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Like button click logic
        holder.binding.heart.setOnClickListener {
            blogRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val blog = mutableData.getValue(BlogPost::class.java) ?: return Transaction.success(mutableData)
                    if (blog.likes == null) blog.likes = mutableMapOf()

                    val liked = blog.likes.containsKey(currentUserId)
                    if (liked) {
                        blog.likes.remove(currentUserId)
                        blog.likeCount = (blog.likeCount - 1).coerceAtLeast(0)
                    } else {
                        blog.likes[currentUserId] = true
                        blog.likeCount += 1
                    }

                    mutableData.value = blog
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (committed) {
                        val updated = currentData?.getValue(BlogPost::class.java)
                        holder.binding.heart.setImageResource(
                            if (updated?.likes?.containsKey(currentUserId) == true)
                                R.drawable.ic_heart_filled
                            else
                                R.drawable.ic_heart_outline
                        )
                        holder.binding.heartCount.text = updated?.likeCount.toString()
                    } else {
                        Log.e("BlogAdapter", "Transaction failed: ${error?.message}")
                    }
                }
            })
        }

        // Send comment
        holder.binding.sendBtn.setOnClickListener {
            val commentText = holder.binding.post.text.toString().trim()
            if (commentText.isNotEmpty()) {
                val commentRef = firebaseInstance.getReference("comments").child(post.postId)
                val commentId = commentRef.push().key ?: return@setOnClickListener

                val comment = Comment(
                    commentId = commentId,
                    postId = post.postId,
                    userId = currentUserId,
                    userProfileImageBase64 = currentUserProfilePic,
                    comment = commentText,
                    timestamp = System.currentTimeMillis()
                )

                commentRef.child(commentId).setValue(comment)
                    .addOnSuccessListener {
                        // Increment the comment count for the blog post
                        updateCommentCount(post, holder)

                        holder.binding.post.setText("")
                        Toast.makeText(context, "Comment posted!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(context, "Failed to post comment: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        firebaseInstance.getReference("comments").child(post.postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments.clear()
                    for (commentSnap in snapshot.children) {
                        val comment = commentSnap.getValue(Comment::class.java)
                        comment?.let { comments.add(it) }
                    }
                    commentAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateCommentCount(post: BlogPost, holder: BlogViewHolder) {
        val blogRef = firebaseInstance.getReference("blogPosts").child(post.postId)
        blogRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val blog = mutableData.getValue(BlogPost::class.java) ?: return Transaction.success(mutableData)

                // Increment the comment count
                blog.commentCount = (blog.commentCount + 1)

                mutableData.value = blog
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) {
                    val updated = currentData?.getValue(BlogPost::class.java)

                    // Update the comment count UI with the new value
                    updated?.let { updateCommentCountInUI(holder, it.commentCount) }
                } else {
                    Log.e("BlogAdapter", "Transaction failed: ${error?.message}")
                }
            }
        })
    }

    private fun loadComments(holder: BlogViewHolder, postId: String) {
        val commentRef = firebaseInstance.getReference("comments").child(postId)
        val commentList = mutableListOf<Comment>()

        commentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()
                for (snap in snapshot.children) {
                    val comment = snap.getValue(Comment::class.java)
                    comment?.let { commentList.add(it) }
                }

                holder.binding.recyclerViewComment.layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.binding.recyclerViewComment.adapter = AdapterComment(context, commentList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getTimeAgo(time: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(time))
        }
    }

    override fun getItemCount(): Int = blogList.size
}
