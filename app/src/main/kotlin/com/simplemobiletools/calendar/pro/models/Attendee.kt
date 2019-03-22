package com.simplemobiletools.calendar.pro.models

import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.CalendarContract
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

data class Attendee(val contactId: Int, var name: String, val email: String, var status: Int, var photoUri: String, var isMe: Boolean, var relationship: Int) {
    fun getPublicName() = if (name.isNotEmpty()) name else email

    fun updateImage(context: Context, imageView: ImageView, placeholder: Drawable) {
        if (photoUri.isEmpty()) {
            imageView.setImageDrawable(placeholder)
        } else {
            val options = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .error(placeholder)
                    .centerCrop()

            Glide.with(context)
                    .load(photoUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(placeholder)
                    .apply(options)
                    .apply(RequestOptions.circleCropTransform())
                    .into(imageView)
        }
    }

    fun showStatusImage() = status == CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED ||
            status == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED ||
            status == CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE
}
