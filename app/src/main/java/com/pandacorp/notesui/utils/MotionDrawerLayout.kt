package com.pandacorp.notesui.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.drawerlayout.widget.DrawerLayout

class MotionDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    MotionLayout(context, attrs, defStyleAttr), DrawerLayout.DrawerListener {
    private val TAG = "Utils"
    
    override fun onDrawerStateChanged(newState: Int) {
    
    }
    
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
        progress = slideOffset
        
    }
    
    override fun onDrawerClosed(drawerView: View) {
    
    }
    
    override fun onDrawerOpened(drawerView: View) {
    
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? DrawerLayout)?.addDrawerListener(this)
    }
    
}