package com.example.dermascanai

data class BlogPost(
    var postId: String = "",
    var userId: String = "",
    var fullName: String = "",
    var profilePicBase64: String = "",
    var content: String = "",
    var postImageBase64: String? = null,
    var timestamp: Long = 0L,
    var likeCount: Int = 0,
    var commentCount: Int = 0,
    var likes: MutableMap<String, Boolean> = mutableMapOf()
)




data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val userProfileImageBase64: String? = null,
    val comment: String = "",
    val timestamp: Long = 0L
)




