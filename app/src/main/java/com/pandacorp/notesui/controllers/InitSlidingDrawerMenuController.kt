package com.pandacorp.notesui.controllers

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.github.dhaval2404.imagepicker.ImagePicker
import com.pandacorp.domain.models.NoteItem
import com.pandacorp.domain.usecases.notes.database.UpdateNoteUseCase
import com.pandacorp.notesui.R
import com.pandacorp.notesui.databinding.ActivityNoteBinding
import com.pandacorp.notesui.databinding.MenuDrawerEndBinding
import com.pandacorp.notesui.presentation.NoteActivity
import com.pandacorp.notesui.presentation.adapter.ImagesRecyclerAdapter
import com.pandacorp.notesui.utils.ThemeHandler
import com.pandacorp.notesui.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class InitSlidingDrawerMenuController(private val updateNoteUseCase: UpdateNoteUseCase) {
    private val TAG = "NoteActivity"
    
    private lateinit var context: Context
    private lateinit var activity: NoteActivity
    private lateinit var note: NoteItem
    private lateinit var noteBinding: ActivityNoteBinding
    private lateinit var menuBinding: MenuDrawerEndBinding
    
    private lateinit var toolbar: Toolbar
    
    private lateinit var pickImageResult: ActivityResultLauncher<Intent>
    
    private val showMoreDrawable by lazy {
        ContextCompat.getDrawable(
                context,
                R.drawable.ic_show_more_animated) as AnimatedVectorDrawable
        
    }
    private val showLessDrawable: AnimatedVectorDrawable by lazy {
        ContextCompat.getDrawable(
                context,
                R.drawable.ic_show_less_animated) as AnimatedVectorDrawable
        
    }
    
    operator fun invoke(
        context: Context,
        activity: NoteActivity,
        note: NoteItem,
        noteBinding: ActivityNoteBinding,
        pickImageResult: ActivityResultLauncher<Intent>
    ) {
        this.context = context
        this.activity = activity
        this.note = note
        this.noteBinding = noteBinding
        this.menuBinding = noteBinding.drawerMenuInclude
        this.pickImageResult = pickImageResult
        
        this.toolbar = activity.findViewById(R.id.toolbar)
        
        initViews()
        
    }
    
    private fun initViews() {
        
        menuBinding.expandChangeBackgroundButton.setOnClickListener {
            if (menuBinding.changeBackgroundExpandableLayout.isExpanded) {
                menuBinding.changeBackgroundButtonImageView.setImageDrawable(showLessDrawable)
                showLessDrawable.start()
                menuBinding.changeBackgroundExpandableLayout.collapse()
            } else {
                menuBinding.changeBackgroundButtonImageView.setImageDrawable(showMoreDrawable)
                showMoreDrawable.start()
                menuBinding.changeBackgroundExpandableLayout.expand()
            }
            
        }
        
        menuBinding.drawerMenuSelectImageButton.setOnClickListener {
            
            ImagePicker.with(activity = activity)
                .crop(1f, 2f)
                .createIntent {
                    Log.d(TAG, "invoke: createIntent")
                    pickImageResult.launch(it)
                    
                }
            
            
        }
        menuBinding.drawerMenuResetButton.setOnClickListener() {
            val colorBackground = ContextCompat.getColor(
                    context,
                    ThemeHandler(context).getColorBackground())
            noteBinding.contentActivityInclude.noteBackgroundImageView.setImageDrawable(
                    ColorDrawable(colorBackground))
            CoroutineScope(Dispatchers.IO).launch {
                note.background = colorBackground.toString()
                updateNoteUseCase(note)
                
            }
            
        }
        
        initImageRecyclerView()
        initChangeTransparentViewsSwitchCompat()
    }
    
    private fun initImageRecyclerView() {
        val imagesList = fillImagesList()
        val imageRecyclerAdapter = ImagesRecyclerAdapter(context, imagesList)
        imageRecyclerAdapter.setOnClickListener(object : ImagesRecyclerAdapter.OnClickListener {
            override fun onItemClick(view: View?, drawable: Drawable, position: Int) {
                // Here we store int as background, then get drawable by position
                // from Utils.backgroundImagesIds and set it.
                noteBinding.contentActivityInclude.noteBackgroundImageView.setImageDrawable(drawable)
                note.background = position.toString()
                CoroutineScope(Dispatchers.IO).launch {
                    updateNoteUseCase(note)
                    
                }
                
            }
            
            override fun onItemLongClick(view: View?, drawable: Drawable, position: Int) {
            
            }
        })
        menuBinding.imageRecyclerView.adapter = imageRecyclerAdapter
        
    }
    
    private fun fillImagesList(): MutableList<Drawable> {
        val imagesList = mutableListOf<Drawable>()
        
        for (drawableResId in Utils.backgroundImages) {
            imagesList.add(ContextCompat.getDrawable(context, drawableResId)!!)
            
        }
        return imagesList
    }
    
    private fun initChangeTransparentViewsSwitchCompat() {
        menuBinding.switchTransparentActionBarSwitchCompat.setOnCheckedChangeListener { buttonView, isChecked ->
            setTransparentActionBar(isChecked)
            note.isShowTransparentActionBar = isChecked
            CoroutineScope(Dispatchers.IO).launch {
                updateNoteUseCase(note)
                
            }
            
        }
        menuBinding.switchTransparentActionBarSwitchCompat.isChecked =
            note.isShowTransparentActionBar
        
    }
    
    private fun setTransparentActionBar(isChecked: Boolean) {
        if (isChecked) {
            activity.supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } else {
            val colorPrimary =
                ContextCompat.getColor(context, ThemeHandler(context).getColorPrimary())
            activity.supportActionBar!!.setBackgroundDrawable(ColorDrawable(colorPrimary))
        }
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar!!.setHomeButtonEnabled(true)
    }
    
}