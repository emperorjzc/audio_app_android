package com.cheng.audio

import android.app.Activity
import android.content.Intent

fun Activity.start(cls: Class<*>?) {
    this.startActivity(Intent(this, cls))
}