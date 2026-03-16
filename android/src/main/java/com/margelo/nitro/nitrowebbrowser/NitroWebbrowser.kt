package com.margelo.nitro.nitrowebbrowser
  
import com.facebook.proguard.annotations.DoNotStrip

@DoNotStrip
class NitroWebbrowser : HybridNitroWebbrowserSpec() {
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }
}
